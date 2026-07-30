package com.visceralfit.domain.engine

import com.visceralfit.domain.model.BlockKind
import com.visceralfit.domain.model.ExperienceLevel
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.Segment
import com.visceralfit.domain.model.SegmentKind
import com.visceralfit.domain.model.Workout
import com.visceralfit.domain.model.WorkoutStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.math.absoluteValue
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The three remaining invariants from spec §1, exercised across the whole request space
 * rather than at a handful of points: duration fit, structure and eligibility.
 *
 * Every case runs against the real shipped catalogue (see [CatalogueFixture]), because an
 * invariant that only holds for a convenient test catalogue is not an invariant.
 *
 * A request that legitimately cannot be built — a five-minute HIIT session, for instance —
 * must fail with a *specific* reason rather than being quietly substituted, so the
 * failures are asserted as tightly as the successes.
 */
class WorkoutGeneratorInvariantTest {

    @Test
    fun `every generated session lands within thirty seconds of the request`() {
        forEveryRequest { request, result ->
            result.onSuccess { workout ->
                val drift = (workout.actualDuration - request.duration).absoluteValue
                assertTrue(
                    "${describe(request)} drifted by $drift",
                    drift <= WorkoutGenerator.DURATION_TOLERANCE,
                )
            }
        }
    }

    /**
     * The tolerance exists so a final work interval need not be truncated mid-effort. In
     * practice the engine fills every block to the second, so if this ever starts merely
     * *passing* rather than being exact, something has begun rounding.
     */
    @Test
    fun `sessions are in fact exact, not merely within tolerance`() {
        forEveryRequest { request, result ->
            result.onSuccess { workout ->
                assertEquals(describe(request), request.duration, workout.actualDuration)
            }
        }
    }

    @Test
    fun `every generated session has all three blocks, none of them empty`() {
        forEveryRequest { request, result ->
            result.onSuccess { workout ->
                assertEquals(
                    describe(request),
                    listOf(BlockKind.WARM_UP, BlockKind.MAIN, BlockKind.COOL_DOWN),
                    workout.blocks.map { it.kind },
                )
                workout.blocks.forEach { block ->
                    assertTrue("${describe(request)}: ${block.kind} is empty", block.segments.isNotEmpty())
                }
            }
        }
    }

    @Test
    fun `the warm-up and cool-down are never shorter than the style's minimum`() {
        forEveryRequest { request, result ->
            result.onSuccess { workout ->
                val budget = StyleBudget.of(request.style)
                val warmUp = workout.blocks.first { it.kind == BlockKind.WARM_UP }.duration
                val coolDown = workout.blocks.first { it.kind == BlockKind.COOL_DOWN }.duration
                assertTrue(
                    "${describe(request)}: warm-up $warmUp below ${budget.minWarmUpSeconds}s",
                    warmUp >= budget.minWarmUpSeconds.seconds,
                )
                assertTrue(
                    "${describe(request)}: cool-down $coolDown below ${budget.minCoolDownSeconds}s",
                    coolDown >= budget.minCoolDownSeconds.seconds,
                )
                // Spec §1.3 states the floor independently of the budget table: two
                // minutes, three for HIIT. Asserted separately so a budget-table edit
                // cannot lower it unnoticed.
                val floor = if (request.style == WorkoutStyle.HIIT) 3.minutes else 2.minutes
                assertTrue("${describe(request)}: cool-down $coolDown below $floor", coolDown >= floor)
            }
        }
    }

    @Test
    fun `no segment uses an exercise the request excluded`() {
        forEveryRequest { request, result ->
            result.onSuccess { workout ->
                workout.segments.mapNotNull(Segment::exercise).forEach { exercise ->
                    assertTrue(
                        "${describe(request)}: ${exercise.id} is outside ${request.modalities}",
                        exercise.modality in request.modalities,
                    )
                    assertTrue(
                        "${describe(request)}: ${exercise.id} is above ${request.level}",
                        request.level.canAttempt(exercise.difficulty),
                    )
                    val avoided = exercise.cautionTags.intersect(request.avoidTags)
                    assertEquals(
                        "${describe(request)}: ${exercise.id} carries avoided $avoided",
                        emptySet<String>(),
                        avoided,
                    )
                }
            }
        }
    }

    @Test
    fun `every work segment names an exercise and every segment has positive length`() {
        forEveryRequest { request, result ->
            result.onSuccess { workout ->
                workout.segments.forEach { segment ->
                    assertTrue("${describe(request)}: non-positive segment", segment.duration.isPositive())
                    if (segment.kind == SegmentKind.WORK) {
                        assertTrue("${describe(request)}: WORK with no exercise", segment.exercise != null)
                    }
                }
            }
        }
    }

    /**
     * A segment too short to cue is a rounding artefact, and it reads on screen as a bug.
     * Transitions are the one deliberate exception: they are fixed at 20 s.
     */
    @Test
    fun `no segment is too short to be cued`() {
        forEveryRequest { request, result ->
            result.onSuccess { workout ->
                workout.segments
                    .filter { it.kind != SegmentKind.TRANSITION }
                    .forEach { segment ->
                        assertTrue(
                            "${describe(request)}: ${segment.duration} segment",
                            segment.duration >= SessionConstants.MIN_SEGMENT_SECONDS.seconds,
                        )
                    }
            }
        }
    }

    /** A failure must name a reason the UI can act on, never a bare error. */
    @Test
    fun `every failure is a specific generation failure with usable detail`() {
        forEveryRequest { request, result ->
            result.onFailure { failure ->
                assertTrue("${describe(request)} failed with $failure", failure is GenerationFailure)
                if (failure is GenerationFailure.DurationTooShort) {
                    assertTrue(
                        "${describe(request)}: reported minimum ${failure.minimum} is not above the request",
                        failure.requested < failure.minimum || failure.requested > MAX_DURATION,
                    )
                }
            }
        }
    }

    /**
     * The specification's minimum totals are reachable, not merely stated: a request at
     * exactly the minimum must succeed, and one second's worth less must fail. Getting
     * this wrong makes the UI offer a duration that cannot be built (REQ-024).
     */
    @Test
    fun `each style's stated minimum total is exactly the shortest buildable session`() {
        val generator = DefaultWorkoutGenerator(CatalogueFixture.ALL)
        WorkoutStyle.entries.forEach { style ->
            val minimum = StyleBudget.of(style).minTotalSeconds
            val atMinimum = generator.generate(baseRequest(minimum.seconds, style))
            assertTrue(
                "$style at its stated minimum ${minimum}s failed: ${atMinimum.exceptionOrNull()}",
                atMinimum.isSuccess,
            )
            val belowMinimum = generator.generate(baseRequest((minimum - 1).seconds, style))
            assertTrue(
                "$style one second below its minimum unexpectedly succeeded",
                belowMinimum.exceptionOrNull() is GenerationFailure.DurationTooShort,
            )
        }
    }

    private fun baseRequest(duration: Duration, style: WorkoutStyle) = WorkoutRequest(
        duration = duration,
        style = style,
        modalities = Modality.entries.toSet(),
        level = ExperienceLevel.ADVANCED,
        seed = 1L,
    )

    /**
     * Every style against every duration preset, both custom-range boundaries, and each
     * experience level and modality combination. Roughly two thousand requests, all of
     * which run in well under a second because `:domain` has no Android dependency.
     */
    private fun forEveryRequest(assertion: (WorkoutRequest, Result<Workout>) -> Unit) {
        assertTrue("the request matrix produced nothing", REQUEST_MATRIX.size > MIN_CASES)
        REQUEST_MATRIX.forEach { request ->
            val catalogue = CatalogueFixture.forModalities(*request.modalities.toTypedArray())
            assertion(request, DefaultWorkoutGenerator(catalogue).generate(request))
        }
    }

    private fun describe(request: WorkoutRequest): String =
        "${request.duration.inWholeMinutes}min ${request.style.id} " +
            "${request.modalities.map { it.id }.sorted()} ${request.level.id} avoid=${request.avoidTags}"

    private companion object {
        /** REQ-012 presets, plus both ends of the accepted custom range (A-0003). */
        val DURATIONS_MINUTES = listOf(3, 4, 5, 7, 9, 10, 15, 16, 20, 30, 45, 60, 90, 119, 120)

        val MODALITY_SETS = listOf(
            setOf(Modality.FLOOR_PILATES),
            setOf(Modality.REFORMER_PILATES),
            setOf(Modality.SPIN_BIKE),
            setOf(Modality.ELLIPTICAL),
            setOf(Modality.SPIN_BIKE, Modality.FLOOR_PILATES),
            setOf(Modality.ELLIPTICAL, Modality.FLOOR_PILATES),
            Modality.entries.toSet(),
        )

        val AVOID_TAG_SETS = listOf(
            emptySet(),
            setOf("knee"),
            setOf("lower_back", "neck"),
            setOf("cardiac_caution"),
        )

        val MAX_DURATION = SessionConstants.MAX_TOTAL_SECONDS.seconds
        const val PRIME = 31
        const val MIN_CASES = 1_000

        /**
         * Every duration × style × modality set × level × avoid-tag combination, built once
         * and shared by every assertion in this class. Around 1,700 requests, all of which
         * generate in well under a second.
         */
        val REQUEST_MATRIX: List<WorkoutRequest> = DURATIONS_MINUTES.flatMap { minutes ->
            WorkoutStyle.entries.flatMap { style ->
                MODALITY_SETS.flatMap { modalities ->
                    ExperienceLevel.entries.flatMap { level ->
                        AVOID_TAG_SETS.map { avoidTags ->
                            WorkoutRequest(
                                duration = minutes.minutes,
                                style = style,
                                modalities = modalities,
                                level = level,
                                avoidTags = avoidTags,
                                seed = (minutes * PRIME + level.order).toLong(),
                            )
                        }
                    }
                }
            }
        }
    }
}

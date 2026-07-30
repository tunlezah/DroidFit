package com.visceralfit.domain.engine

import com.visceralfit.domain.model.Exercise
import com.visceralfit.domain.model.ExperienceLevel
import com.visceralfit.domain.model.IntensityTarget
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.WorkoutStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The failure paths and the honesty requirements — the two places where "it compiled and
 * produced a workout" is not good enough.
 *
 * A generation failure must arrive as a named reason the UI can turn into an actionable
 * message, because the alternative that competitors ship is a dead Start button
 * (`framework/03_competitive_analysis.md` §Complaint themes). And a session that could not
 * be built as requested must never be *presented* as the thing that was requested — that
 * is REQ-004, and for a Pilates-only session it is the direct implementation of the
 * evidence base's constraint on how Pilates is described.
 */
class WorkoutGeneratorFailureTest {

    private val generator = DefaultWorkoutGenerator(CatalogueFixture.ALL)

    // --- DurationTooShort ---------------------------------------------------------

    @Test
    fun `a request below the accepted range fails with the style's minimum`() {
        val failure = generator.generate(request(2.minutes, WorkoutStyle.ZONE_2)).failure()
        assertTrue(failure is GenerationFailure.DurationTooShort)
        failure as GenerationFailure.DurationTooShort
        assertEquals(2.minutes, failure.requested)
        assertEquals(StyleBudget.of(WorkoutStyle.ZONE_2).minTotalSeconds.seconds, failure.minimum)
    }

    @Test
    fun `a request above the accepted range fails rather than being clamped`() {
        val failure = generator.generate(request(121.minutes, WorkoutStyle.MIXED)).failure()
        assertTrue("121 minutes should not be silently clamped to 120", failure is GenerationFailure.DurationTooShort)
    }

    /**
     * The consequence the UI has to surface (REQ-024): a five-minute request cannot be a
     * HIIT session, because the smallest interval template needs eight minutes of main
     * block on its own.
     */
    @Test
    fun `five minutes cannot be a HIIT session and says so`() {
        val failure = generator.generate(request(5.minutes, WorkoutStyle.HIIT)).failure()
        assertTrue(failure is GenerationFailure.DurationTooShort)
        failure as GenerationFailure.DurationTooShort
        assertEquals(16.minutes, failure.minimum)
    }

    /**
     * The other half of REQ-024: the minimum is per style, so a duration that cannot be an
     * interval session can still be a good recovery session. This is why the failure has to
     * name the style's minimum rather than a single global one — the UI uses it to decide
     * which styles to offer for the chosen duration.
     */
    @Test
    fun `a duration too short for intervals is still long enough for recovery`() {
        assertTrue(generator.generate(request(7.minutes, WorkoutStyle.RECOVERY)).isSuccess)
        val failure = generator.generate(request(7.minutes, WorkoutStyle.HIIT)).exceptionOrNull()
        assertTrue(failure is GenerationFailure.DurationTooShort)
    }

    // --- NoEligibleExercises ------------------------------------------------------

    @Test
    fun `no enabled modality fails with the modalities and level that were asked for`() {
        val failure = generator.generate(
            request(20.minutes, WorkoutStyle.MIXED).copy(modalities = emptySet()),
        ).failure()
        assertTrue(failure is GenerationFailure.NoEligibleExercises)
        failure as GenerationFailure.NoEligibleExercises
        assertEquals(emptySet<Modality>(), failure.modalities)
    }

    /** Spec §1: every exercise excluded by a caution tag is still a named failure. */
    @Test
    fun `a catalogue in which every exercise is avoided fails with no eligible exercises`() {
        val everythingTagged = listOf(
            exercise("floor_pilates_tagged_one", 2.8, cautionTags = setOf("lower_back")),
            exercise("floor_pilates_tagged_two", 3.8, cautionTags = setOf("lower_back", "knee")),
        )
        val failure = DefaultWorkoutGenerator(everythingTagged).generate(
            request(20.minutes, WorkoutStyle.RECOVERY).copy(
                modalities = setOf(Modality.FLOOR_PILATES),
                avoidTags = setOf("lower_back"),
            ),
        ).failure()
        assertTrue(failure is GenerationFailure.NoEligibleExercises)
    }

    @Test
    fun `a level with no content beneath it fails rather than serving harder work`() {
        val advancedOnly = listOf(
            exercise("floor_pilates_hard", 3.8, difficulty = ExperienceLevel.ADVANCED),
        )
        val failure = DefaultWorkoutGenerator(advancedOnly).generate(
            request(20.minutes, WorkoutStyle.RECOVERY).copy(
                modalities = setOf(Modality.FLOOR_PILATES),
                level = ExperienceLevel.BEGINNER,
            ),
        ).failure()
        assertTrue(failure is GenerationFailure.NoEligibleExercises)
        assertEquals(ExperienceLevel.BEGINNER, (failure as GenerationFailure.NoEligibleExercises).level)
    }

    // --- InsufficientVariety ------------------------------------------------------

    /**
     * A single-exercise catalogue can fill a short session honestly, but not a warm-up
     * that wants three distinct movements. Repeating one movement three times is a content
     * gap, and the engine says so rather than shipping the repetition.
     */
    @Test
    fun `a one-exercise catalogue fails when a block needs three distinct movements`() {
        val single = listOf(exercise("floor_pilates_only_one", 2.8))
        val request = request(THREE_SEGMENT_WARM_UP, WorkoutStyle.ZONE_2)
            .copy(modalities = setOf(Modality.FLOOR_PILATES))
        val failure = DefaultWorkoutGenerator(single).generate(request).failure()
        assertTrue("expected InsufficientVariety, got $failure", failure is GenerationFailure.InsufficientVariety)
        failure as GenerationFailure.InsufficientVariety
        assertEquals(1, failure.available)
    }

    // --- Honesty ------------------------------------------------------------------

    /**
     * Spec §7 and REQ-004. A Pilates session must never be titled as an interval or
     * fat-loss session, and the style recorded must be what was built.
     */
    @Test
    fun `a Pilates-only HIIT request is titled and recorded as Pilates`() {
        val workout = DefaultWorkoutGenerator(CatalogueFixture.forModalities(Modality.FLOOR_PILATES))
            .generate(
                request(30.minutes, WorkoutStyle.HIIT).copy(modalities = setOf(Modality.FLOOR_PILATES)),
            ).getOrThrow()

        assertEquals("Floor Pilates: strength and control — 30 min", workout.title)
        assertEquals(WorkoutStyle.RECOVERY, workout.style)
        FORBIDDEN_IN_PILATES_TITLES.forEach { forbidden ->
            assertTrue(
                "a Pilates session was titled \"${workout.title}\"",
                !workout.title.contains(forbidden, ignoreCase = true),
            )
        }
        assertTrue(
            "the substitution was not explained to the user: ${workout.buildNotes}",
            workout.buildNotes.any { it.contains("Pilates", ignoreCase = true) },
        )
        // §7.3: contributes zero vigorous minutes.
        assertTrue(
            "a Pilates session prescribed vigorous work",
            workout.segments.none { it.intensity == IntensityTarget.VIGOROUS },
        )
    }

    @Test
    fun `a reformer-only session is named for the reformer`() {
        val workout = DefaultWorkoutGenerator(CatalogueFixture.forModalities(Modality.REFORMER_PILATES))
            .generate(
                request(20.minutes, WorkoutStyle.RECOVERY).copy(modalities = setOf(Modality.REFORMER_PILATES)),
            ).getOrThrow()
        assertEquals("Reformer Pilates: strength and control — 20 min", workout.title)
    }

    /**
     * Spec §3: when a fallback caps the intensity, the title must say so. The case is
     * reached by excluding the only vigorous-anchored elliptical work a beginner can do,
     * which leaves the steady pool as the hardest thing available.
     */
    @Test
    fun `capping the intensity downgrades the title and explains why`() {
        val workout = DefaultWorkoutGenerator(CatalogueFixture.forModalities(Modality.ELLIPTICAL))
            .generate(
                request(20.minutes, WorkoutStyle.HIIT).copy(
                    modalities = setOf(Modality.ELLIPTICAL),
                    level = ExperienceLevel.BEGINNER,
                    avoidTags = setOf("cardiac_caution"),
                ),
            ).getOrThrow()

        assertEquals("Intervals (steady) — 20 min", workout.title)
        assertTrue(
            "the cap was not explained: ${workout.buildNotes}",
            workout.buildNotes.any { it.contains("capped", ignoreCase = true) },
        )
        assertTrue(
            "a capped session still prescribed vigorous work",
            workout.segments.none { it.intensity == IntensityTarget.VIGOROUS },
        )
    }

    /** A style substitution is a bigger change than a cap, and gets said out loud too. */
    @Test
    fun `a style the duration cannot hold is substituted only with an explanation`() {
        val workout = generator.generate(request(10.minutes, WorkoutStyle.MIXED)).getOrThrow()
        assertEquals(WorkoutStyle.ZONE_2, workout.style)
        assertEquals("Steady — 10 min", workout.title)
        assertTrue(
            "the substitution was not explained: ${workout.buildNotes}",
            workout.buildNotes.any { it.contains("built as", ignoreCase = true) },
        )
    }

    @Test
    fun `generation never throws, whatever it is handed`() {
        val hostile = listOf(
            request(Duration.ZERO, WorkoutStyle.HIIT),
            request((-5).minutes, WorkoutStyle.MIXED),
            request(20.minutes, WorkoutStyle.HIIT).copy(modalities = emptySet()),
            request(20.minutes, WorkoutStyle.HIIT).copy(avoidTags = ALL_CAUTION_TAGS),
            request(20.minutes, WorkoutStyle.HIIT).copy(seed = Long.MIN_VALUE),
        )
        hostile.forEach { request ->
            val result = DefaultWorkoutGenerator(CatalogueFixture.ALL).generate(request)
            result.onFailure { assertTrue("$request threw $it", it is GenerationFailure) }
        }
        assertTrue(DefaultWorkoutGenerator(emptyList()).generate(request(20.minutes, WorkoutStyle.MIXED)).isFailure)
    }

    private fun request(duration: Duration, style: WorkoutStyle) = WorkoutRequest(
        duration = duration,
        style = style,
        modalities = Modality.entries.toSet(),
        level = ExperienceLevel.ADVANCED,
        seed = 7L,
    )

    private fun exercise(
        id: String,
        metValue: Double,
        difficulty: ExperienceLevel = ExperienceLevel.BEGINNER,
        cautionTags: Set<String> = emptySet(),
    ) = Exercise(
        id = id,
        name = id,
        modality = Modality.FLOOR_PILATES,
        difficulty = difficulty,
        metValue = metValue,
        howTo = listOf("A test fixture step."),
        spokenInstruction = id,
        safetyNotes = listOf("A test fixture note."),
        commonMistakes = listOf("A test fixture mistake."),
        musclesWorked = listOf("Test"),
        illustrationId = id,
        cautionTags = cautionTags,
    )

    private fun <T> Result<T>.failure(): Throwable =
        checkNotNull(exceptionOrNull()) { "expected a failure but got ${getOrNull()}" }

    private companion object {
        /**
         * Long enough that the warm-up wants three segments: Zone 2 takes 12% of the total,
         * and three segments need at least 270 s of warm-up.
         */
        val THREE_SEGMENT_WARM_UP = 2_250.seconds

        val FORBIDDEN_IN_PILATES_TITLES = listOf("HIIT", "Intervals", "fat", "burn")

        val ALL_CAUTION_TAGS = setOf(
            "lower_back", "neck", "shoulder", "wrist", "knee",
            "hip", "ankle", "pregnancy", "cardiac_caution", "balance",
        )
    }
}

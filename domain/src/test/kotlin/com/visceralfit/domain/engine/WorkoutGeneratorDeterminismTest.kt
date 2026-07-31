package com.visceralfit.domain.engine

import com.visceralfit.domain.model.ExperienceLevel
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.Workout
import com.visceralfit.domain.model.WorkoutStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

/**
 * The determinism invariant (spec §1.1 and the §6 checklist).
 *
 * WHY A CHECKED-IN GOLDEN FILE AND NOT TWO IN-PROCESS CALLS: two calls in one JVM share
 * `HashSet` and `HashMap` iteration order, and share whatever `hashCode` values that run
 * happened to produce. A test that compares them passes while a real non-determinism bug
 * is present — this is the classic way the bug hides, and the specification calls it out
 * by name. A checked-in expected output compares against a *different* run of a
 * *different* JVM, which is the only comparison that can catch it.
 *
 * When the algorithm or the catalogue changes on purpose, the file below changes with it.
 * The test writes what it actually produced to `build/golden/` on failure so the update
 * is a copy rather than a transcription.
 */
class WorkoutGeneratorDeterminismTest {

    @Test
    fun `reproduces the checked-in plan exactly`() {
        val actual = render(generate(GOLDEN_REQUEST))
        val expected = goldenFile().takeIf { it.exists() }?.readText()?.trim()
        if (expected == null || expected != actual) {
            val dump = File("build/golden/${GOLDEN_NAME}.actual.txt")
            dump.parentFile?.mkdirs()
            dump.writeText(actual + "\n")
        }
        assertEquals(
            "the generated plan no longer matches the checked-in golden file; " +
                "the actual output has been written to build/golden/${GOLDEN_NAME}.actual.txt",
            expected,
            actual,
        )
    }

    /**
     * A thousand seeds, each generated twice. This does not prove cross-process
     * determinism — the golden file above does that — but it does prove that nothing in
     * the engine carries state between calls, which the golden file cannot show because
     * it only ever generates once.
     */
    @Test
    fun `a thousand seeds each generate identically twice`() {
        val generator = DefaultWorkoutGenerator(CatalogueFixture.ALL)
        repeat(SEED_COUNT) { index ->
            val request = WorkoutRequest(
                duration = (MIN_MINUTES + index % MINUTE_RANGE).minutes,
                style = WorkoutStyle.entries[index % WorkoutStyle.entries.size],
                modalities = MODALITY_SETS[index % MODALITY_SETS.size],
                level = ExperienceLevel.entries[index % ExperienceLevel.entries.size],
                seed = index.toLong() * SEED_STRIDE,
            )
            val first = generator.generate(request)
            val second = generator.generate(request)
            assertEquals("seed ${request.seed} was not reproducible", first.isSuccess, second.isSuccess)
            if (first.isSuccess) {
                assertEquals(
                    "seed ${request.seed} produced two different plans",
                    render(first.getOrThrow()),
                    render(second.getOrThrow()),
                )
            } else {
                assertEquals(
                    first.exceptionOrNull()?.toString(),
                    second.exceptionOrNull()?.toString(),
                )
            }
        }
    }

    /**
     * The recency list is the one input intended to change the outcome, so it had better
     * change it — otherwise the variety requirement (REQ-034) is silently a no-op.
     */
    @Test
    fun `de-prioritising recent exercises changes the selection`() {
        val plain = generate(GOLDEN_REQUEST)
        val avoided = generate(GOLDEN_REQUEST.copy(recentExerciseIds = plain.segments.mapNotNull { it.exercise?.id }))
        assertTrue(
            "the recency list made no difference to any slot",
            render(plain) != render(avoided),
        )
    }

    private fun generate(request: WorkoutRequest): Workout =
        DefaultWorkoutGenerator(CatalogueFixture.forModalities(*request.modalities.toTypedArray()))
            .generate(request)
            .getOrThrow()

    /** A stable, diffable rendering: one line per segment, plus the workout's identity. */
    private fun render(workout: Workout): String = buildString {
        appendLine("id: ${workout.id}")
        appendLine("title: ${workout.title}")
        appendLine("style: ${workout.style.id}")
        appendLine("modalities: ${workout.modalities.map { it.id }.sorted().joinToString(",")}")
        appendLine("requested: ${workout.requestedDuration.inWholeSeconds}s")
        appendLine("actual: ${workout.actualDuration.inWholeSeconds}s")
        workout.buildNotes.forEach { appendLine("note: $it") }
        workout.blocks.forEach { block ->
            block.segments.forEach { segment ->
                val round = segment.roundIndex?.let { " round=$it/${segment.roundTotal}" } ?: ""
                appendLine(
                    "${block.kind.id} ${segment.kind.id} ${segment.duration.inWholeSeconds}s " +
                        "rpe=${segment.intensity.rpeRange} ${segment.exercise?.id ?: "-"}$round",
                )
            }
        }
    }.trim()

    /**
     * Resolved by walking up from the working directory, because Gradle runs JVM tests with
     * the module directory as the working directory while some IDEs use the repository root.
     */
    private fun goldenFile(): File {
        val relative = "src/test/resources/golden/$GOLDEN_NAME.txt"
        return listOf(File(relative), File("domain/$relative")).firstOrNull { it.exists() }
            ?: File(relative)
    }

    private companion object {
        const val GOLDEN_NAME = "mixed_20min_spin_and_floor_seed42"

        val GOLDEN_REQUEST = WorkoutRequest(
            duration = 20.minutes,
            style = WorkoutStyle.MIXED,
            modalities = setOf(Modality.SPIN_BIKE, Modality.BODYWEIGHT),
            level = ExperienceLevel.INTERMEDIATE,
            seed = 42L,
        )

        const val SEED_COUNT = 1_000
        const val SEED_STRIDE = 7_919L
        const val MIN_MINUTES = 16
        const val MINUTE_RANGE = 90

        val MODALITY_SETS = listOf(
            setOf(Modality.SPIN_BIKE, Modality.BODYWEIGHT),
            setOf(Modality.ELLIPTICAL, Modality.BODYWEIGHT),
            setOf(Modality.BODYWEIGHT),
            setOf(Modality.SPIN_BIKE, Modality.ELLIPTICAL, Modality.BODYWEIGHT),
            Modality.entries.toSet(),
        )
    }
}

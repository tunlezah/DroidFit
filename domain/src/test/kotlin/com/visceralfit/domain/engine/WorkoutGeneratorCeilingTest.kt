package com.visceralfit.domain.engine

import com.visceralfit.domain.model.EffortCeiling
import com.visceralfit.domain.model.ExperienceLevel
import com.visceralfit.domain.model.IntensityTarget
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.WorkoutStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

/**
 * The user's effort ceiling (A-0007, D-0040).
 *
 * This is the mechanism that answers "am I cleared for vigorous exercise?" in code. The
 * operator is cleared for *moderately* vigorous work, so the shipped default holds sessions
 * at threshold — 76–84% of maximum heart rate — rather than the 85–95% `helgerud2007`
 * prescribes. A ceiling that silently failed to apply would be a safety defect, not a
 * missing feature, so it is asserted at every level and in both directions.
 */
class WorkoutGeneratorCeilingTest {

    @Test
    fun `a threshold ceiling holds interval work at threshold`() {
        val workout = generate(EffortCeiling.THRESHOLD)
        assertTrue(
            "a threshold-capped session still prescribed vigorous work",
            workout.segments.none { it.intensity == IntensityTarget.VIGOROUS },
        )
        assertTrue(
            "the intervals were flattened rather than capped",
            workout.segments.any { it.intensity == IntensityTarget.THRESHOLD },
        )
    }

    /** The title must never claim more than was built (spec §3). */
    @Test
    fun `a capped session says so in its title and explains why`() {
        val workout = generate(EffortCeiling.THRESHOLD)
        assertEquals("Intervals (threshold) — 20 min", workout.title)
        assertTrue(
            "the ceiling was applied without telling the user: ${workout.buildNotes}",
            workout.buildNotes.any { it.contains("effort ceiling") },
        )
    }

    @Test
    fun `a steady ceiling holds everything at Zone 2 or easier`() {
        val workout = generate(EffortCeiling.STEADY)
        assertTrue(
            "a steady-capped session prescribed work above Zone 2",
            workout.segments.none {
                it.intensity == IntensityTarget.VIGOROUS || it.intensity == IntensityTarget.THRESHOLD
            },
        )
        assertEquals("Intervals (steady) — 20 min", workout.title)
    }

    /**
     * The other direction, and the reason this test exists as well as the ones above: a
     * ceiling of vigorous must be indistinguishable from no ceiling. If it left a cap behind,
     * every session would be titled as limited and the word would stop meaning anything.
     */
    @Test
    fun `a vigorous ceiling changes nothing`() {
        val capped = generate(EffortCeiling.VIGOROUS)
        assertEquals("Intervals — 20 min", capped.title)
        assertTrue(
            "a session at the vigorous ceiling prescribed no vigorous work",
            capped.segments.any { it.intensity == IntensityTarget.VIGOROUS },
        )
        assertTrue(
            "an uncapped session claimed a ceiling: ${capped.buildNotes}",
            capped.buildNotes.none { it.contains("effort ceiling") },
        )
    }

    /** A ceiling above what the catalogue can supply must not invent a cap either. */
    @Test
    fun `a ceiling is only reported when it is the binding constraint`() {
        val workout = DefaultWorkoutGenerator(CatalogueFixture.forModalities(Modality.ELLIPTICAL))
            .generate(
                request(EffortCeiling.VIGOROUS).copy(
                    modalities = setOf(Modality.ELLIPTICAL),
                    level = ExperienceLevel.BEGINNER,
                    avoidTags = setOf("cardiac_caution"),
                ),
            ).getOrThrow()
        // The pool ran out of hard work, not the user's ceiling, so the note names the pool.
        assertEquals("Intervals (steady) — 20 min", workout.title)
        assertTrue(
            "a pool cap was blamed on the user's ceiling: ${workout.buildNotes}",
            workout.buildNotes.none { it.contains("effort ceiling") },
        )
    }

    private fun generate(ceiling: EffortCeiling) =
        DefaultWorkoutGenerator(CatalogueFixture.forModalities(Modality.SPIN_BIKE, Modality.BODYWEIGHT))
            .generate(request(ceiling))
            .getOrThrow()

    private fun request(ceiling: EffortCeiling) = WorkoutRequest(
        duration = 20.minutes,
        style = WorkoutStyle.HIIT,
        modalities = setOf(Modality.SPIN_BIKE, Modality.BODYWEIGHT),
        level = ExperienceLevel.ADVANCED,
        seed = 11L,
        effortCeiling = ceiling,
    )
}

package com.visceralfit.domain.usecase

import com.visceralfit.domain.model.BlockKind
import com.visceralfit.domain.model.Exercise
import com.visceralfit.domain.model.ExperienceLevel
import com.visceralfit.domain.model.IntensityTarget
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.Segment
import com.visceralfit.domain.model.SegmentKind
import com.visceralfit.domain.model.Workout
import com.visceralfit.domain.model.WorkoutBlock
import com.visceralfit.domain.model.WorkoutStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class EstimateEnergyExpenditureTest {

    private val estimate = EstimateEnergyExpenditure()

    @Test
    fun `returns null when body mass is unknown`() {
        assertNull(estimate(workoutOf(spinMinutes = 30), bodyMassKg = null))
    }

    @Test
    fun `returns null for a non-positive body mass rather than a negative estimate`() {
        assertNull(estimate(workoutOf(spinMinutes = 30), bodyMassKg = 0.0))
    }

    @Test
    fun `applies the compendium formula MET times kilograms times hours`() {
        // 9.0 MET (spin class, code 01270) x 80 kg x 0.5 h = 360 kcal
        val result = estimate(workoutOf(spinMinutes = 30), bodyMassKg = 80.0)
        assertEquals(360, result)
    }

    @Test
    fun `charges rest segments at the resting rate not at zero`() {
        // 20 min work at 9.0 MET  = 9.0 x 70 x (20/60) = 210.0
        // 10 min rest at 1.3 MET  = 1.3 x 70 x (10/60) =  15.166...
        val result = estimate(workoutOf(spinMinutes = 20, restMinutes = 10), bodyMassKg = 70.0)
        assertEquals(225, result)
    }

    private fun workoutOf(spinMinutes: Int, restMinutes: Int = 0): Workout {
        val spin = Exercise(
            id = "spin_bike_seated_flat",
            name = "Seated flat road",
            modality = Modality.SPIN_BIKE,
            difficulty = ExperienceLevel.BEGINNER,
            metValue = 9.0,
            howTo = listOf("Sit tall, hands light on the bars."),
            spokenInstruction = "Seated flat road. Sit tall and spin.",
            safetyNotes = listOf("Keep a slight bend in the knee at the bottom of the stroke."),
            commonMistakes = listOf("Gripping the bars hard enough to hunch the shoulders."),
            musclesWorked = listOf("Quadriceps", "Glutes"),
            illustrationId = "spin_seated",
        )
        val segments = buildList {
            add(
                Segment(
                    kind = SegmentKind.WORK,
                    duration = spinMinutes.minutes,
                    exercise = spin,
                    intensity = IntensityTarget.ZONE_2,
                ),
            )
            if (restMinutes > 0) {
                add(
                    Segment(
                        kind = SegmentKind.REST,
                        duration = restMinutes.minutes,
                        exercise = null,
                        intensity = IntensityTarget.RECOVERY,
                    ),
                )
            }
        }
        return Workout(
            id = "test",
            title = "Test",
            style = WorkoutStyle.ZONE_2,
            modalities = setOf(Modality.SPIN_BIKE),
            level = ExperienceLevel.BEGINNER,
            blocks = listOf(WorkoutBlock(BlockKind.MAIN, segments)),
            requestedDuration = (spinMinutes + restMinutes).minutes,
            generationSeed = 1L,
        )
    }
}

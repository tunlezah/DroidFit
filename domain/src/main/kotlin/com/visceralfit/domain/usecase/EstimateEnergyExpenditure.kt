package com.visceralfit.domain.usecase

import com.visceralfit.domain.model.SegmentKind
import com.visceralfit.domain.model.Workout
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.time.DurationUnit

/**
 * Estimates session energy expenditure from MET values and body mass.
 *
 * Formula (2024 Adult Compendium of Physical Activities):
 *   kcal = MET × body mass in kg × hours
 *
 * Deliberate limitations, which the UI must surface (REQ-081):
 *  - Returns null when body mass is unknown. Guessing a mass would produce a
 *    number users treat as measured, and a wrong one biases every trend.
 *  - MET values are population averages, not personal measurements. The estimate
 *    is labelled "estimated" everywhere it appears.
 *  - Rest and transition segments are charged at [RESTING_MET] rather than zero,
 *    because the user is still sitting on a bike, not asleep.
 */
class EstimateEnergyExpenditure @Inject constructor() {

    operator fun invoke(workout: Workout, bodyMassKg: Double?): Int? {
        if (bodyMassKg == null || bodyMassKg <= 0.0) return null

        val kcal = workout.segments.sumOf { segment ->
            val met = when (segment.kind) {
                SegmentKind.WORK -> segment.exercise?.metValue ?: RESTING_MET
                SegmentKind.ACTIVE_RECOVERY -> ACTIVE_RECOVERY_MET
                SegmentKind.REST, SegmentKind.TRANSITION -> RESTING_MET
            }
            val hours = segment.duration.toDouble(DurationUnit.HOURS)
            met * bodyMassKg * hours
        }
        return kcal.roundToInt()
    }

    private companion object {
        /** Quiet sitting. Compendium code 07021. */
        const val RESTING_MET = 1.3

        /** Very light pedalling / marching in place between hard efforts. Compendium 01210. */
        const val ACTIVE_RECOVERY_MET = 3.5
    }
}

package com.visceralfit.domain.engine

import com.visceralfit.domain.model.EffortCeiling
import com.visceralfit.domain.model.ExperienceLevel
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.Workout
import com.visceralfit.domain.model.WorkoutStyle
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Produces a session plan from a request.
 *
 * IMPLEMENTATION IS PHASE 06 WORK. The algorithm is fully specified in
 * `/framework/07_workout_engine_spec.md`; implement exactly that spec, including
 * the four invariants restated below, and do not invent additional heuristics
 * without recording them in /project_memory/decisions.md.
 *
 * Invariants any implementation must satisfy (asserted by the property tests
 * described in /framework/13_testing_strategy.md §Engine):
 *  1. DETERMINISM — same [WorkoutRequest] and same seed yields an identical plan.
 *  2. DURATION FIT — `actualDuration` is within ±[DURATION_TOLERANCE] of the request.
 *  3. STRUCTURE — every plan has a warm-up, a main block and a cool-down, and the
 *     warm-up is never shorter than the minimum for the requested style.
 *  4. ELIGIBILITY — no segment uses an exercise outside the requested modalities,
 *     above the requested level, or carrying a tag in the user's avoid list.
 */
interface WorkoutGenerator {
    fun generate(request: WorkoutRequest): Result<Workout>

    companion object {
        /**
         * How far the generated duration may drift from the request. 30 s is one
         * short interval: tight enough that a 20-minute request is honestly
         * 20 minutes, loose enough that the generator need not truncate a final
         * work interval mid-effort.
         */
        val DURATION_TOLERANCE: Duration = 30.seconds
    }
}

data class WorkoutRequest(
    val duration: Duration,
    val style: WorkoutStyle,
    val modalities: Set<Modality>,
    val level: ExperienceLevel,
    val avoidTags: Set<String> = emptySet(),
    /**
     * Seed for the randomised selection. Callers pass a real clock-derived seed in
     * production and a fixed one in tests; the generator itself must never read
     * the clock or call a global RNG.
     */
    val seed: Long,
    /** Exercise ids used in recent sessions, de-prioritised to keep variety (REQ-034). */
    val recentExerciseIds: List<String> = emptyList(),
    /**
     * The hardest effort this session may prescribe.
     *
     * Defaults to [EffortCeiling.VIGOROUS] here and to [EffortCeiling.THRESHOLD] in
     * `UserPreferences`, and the asymmetry is deliberate (D-0040). The engine's contract is
     * to build what it was asked for; it has no view on whether a particular person is
     * cleared for 85–95% of maximum heart rate. The cautious default belongs where the user
     * can see it and change it, which is the settings screen.
     */
    val effortCeiling: EffortCeiling = EffortCeiling.VIGOROUS,
)

/** Why generation could not produce a plan. Surfaced to the user, never swallowed. */
sealed class GenerationFailure(message: String) : Exception(message) {
    data class NoEligibleExercises(
        val modalities: Set<Modality>,
        val level: ExperienceLevel,
    ) : GenerationFailure("No exercises match $modalities at $level")

    data class DurationTooShort(val requested: Duration, val minimum: Duration) :
        GenerationFailure("Requested $requested is below the minimum $minimum for this style")

    data class InsufficientVariety(val required: Int, val available: Int) :
        GenerationFailure("Need $required distinct exercises, only $available available")
}

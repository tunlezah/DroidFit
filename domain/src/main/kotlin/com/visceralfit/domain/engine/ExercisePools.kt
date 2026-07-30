package com.visceralfit.domain.engine

import com.visceralfit.domain.model.Exercise
import com.visceralfit.domain.model.Modality

/**
 * The role-based partition of the eligible exercises, spec §3.
 *
 * Pools overlap deliberately: an exercise at 9.0 MET can be in both the steady and the
 * vigorous pool, because whether it is steady or hard work depends on how the segment is
 * prescribed, not on the exercise.
 *
 * MET RANGE **AND** ANCHOR (D-0026). The specification defines these pools by MET range
 * alone. Implemented literally, that mis-prescribes real sessions, because MET is a cost
 * of work and not a description of it:
 *
 *  - a spin class is 9.0 MET but the Compendium anchors it at Zone 2 (code 01270), so a
 *    MET-only vigorous pool serves the easy flat road as a 30-second HIIT interval;
 *  - `spin_bike_high_cadence_surge` is 8.8 MET anchored vigorous (code 01305), inside the
 *    spec's 4.0–9.0 steady band, so a MET-only steady pool used it as the Zone 2 base of a
 *    Mixed session — a hard interval prescribed as conversational work;
 *  - the Pilates warm-up band of "MET <= 4.0" includes static stretches at 1.8–2.3 MET, so
 *    the first warm-up segment came out as a supine chest opener. A stretch does not warm
 *    anything up, and the specification's own worked example uses a march.
 *
 * All three were found by reading the first generated plan, not by a failing assertion,
 * which is why they are recorded rather than quietly fixed. Each pool below therefore
 * carries the spec's MET range *and* the Compendium intensity anchor; see
 * [IntensityAnchor]. The narrowing is recorded in `framework/07_workout_engine_spec.md` §3.
 *
 * Every pool is sorted by id. Nothing downstream may depend on the order the exercises
 * arrived in, and sorting here means the determinism invariant holds even if the
 * repository's ordering changes.
 */
internal class ExercisePools private constructor(
    val eligible: List<Exercise>,
    val warmUp: List<Exercise>,
    val cardio: List<Exercise>,
    val vigorous: List<Exercise>,
    val threshold: List<Exercise>,
    val steady: List<Exercise>,
    val strength: List<Exercise>,
    val mobility: List<Exercise>,
) {
    /** True when no machine-cardio exercise is available, i.e. this is a Pilates session (§7). */
    val isPilatesOnly: Boolean get() = cardio.isEmpty()

    /** Machine-cardio modalities present in the eligible set, in a stable order. */
    val cardioModalities: List<Modality> get() = cardio.map { it.modality }.distinct().sortedBy { it.id }

    companion object {
        fun partition(eligible: List<Exercise>): ExercisePools {
            val sorted = eligible.sortedBy { it.id }
            val cardio = sorted.filter { it.modality.isMachineCardio }
            return ExercisePools(
                eligible = sorted,
                warmUp = sorted.filter(::isWarmUpCandidate),
                cardio = cardio,
                vigorous = cardio.filter {
                    it.metValue >= SessionConstants.VIGOROUS_MIN_MET && it.isAtLeast(IntensityAnchor.THRESHOLD)
                },
                threshold = cardio.filter {
                    it.metValue >= SessionConstants.THRESHOLD_MIN_MET &&
                        it.metValue <= SessionConstants.THRESHOLD_MAX_MET &&
                        it.isAtLeast(IntensityAnchor.THRESHOLD)
                },
                steady = cardio.filter {
                    it.metValue >= SessionConstants.STEADY_MIN_MET &&
                        it.metValue <= SessionConstants.STEADY_MAX_MET &&
                        it.isAtMost(IntensityAnchor.ZONE_2)
                },
                strength = sorted.filter {
                    !it.modality.isMachineCardio && it.metValue > SessionConstants.STRENGTH_MIN_MET
                },
                mobility = sorted.filter { it.metValue <= SessionConstants.MOBILITY_MAX_MET },
            )
        }

        /**
         * Warm-up candidates: Pilates work in the band between static stretching and
         * steady work, or machine cardio the Compendium anchors no harder than Zone 2.
         *
         * The lower bound on the Pilates band is the correction described in the class
         * documentation: below it are the stretches and breathing drills that belong in a
         * cool-down. When that band is empty the fallback chain drops back to the mobility
         * pool, which is the specification's own answer for a catalogue that cannot fill it.
         */
        private fun isWarmUpCandidate(exercise: Exercise): Boolean = when {
            exercise.modality.isMachineCardio -> exercise.isAtMost(IntensityAnchor.ZONE_2)
            else -> exercise.metValue in RAMP_MET_BAND
        }

        /**
         * Pilates work that ramps: above the mobility band so a static stretch cannot be
         * chosen as a warm-up, at or below the warm-up ceiling so hard work cannot either.
         */
        private val RAMP_MET_BAND =
            SessionConstants.MOBILITY_MAX_MET + MET_EPSILON..SessionConstants.WARM_UP_MAX_MET

        /** MET values are authored to one decimal place, so this steps past the boundary. */
        private const val MET_EPSILON = 0.1

        private fun Exercise.isAtMost(anchor: IntensityAnchor): Boolean =
            IntensityAnchor.of(modality, metValue).isAtMost(anchor)

        private fun Exercise.isAtLeast(anchor: IntensityAnchor): Boolean =
            IntensityAnchor.of(modality, metValue).isAtLeast(anchor)
    }
}

/**
 * A pool resolved through the fallback chain in spec §3, together with what the
 * fallback cost.
 *
 * [cappedAt] is non-null when the fallback forced a lower intensity than the style
 * asked for. That is not a detail: presenting a capped session as the style the user
 * requested would be dishonest, so the caller must downgrade the title.
 */
internal data class ResolvedPool(
    val exercises: List<Exercise>,
    val cappedAt: IntensityAnchor? = null,
    val note: String? = null,
) {
    val isEmpty: Boolean get() = exercises.isEmpty()
}

/**
 * Applies the fallback chain for each pool the block builders need. Each returns the
 * first non-empty option, recording the substitution so the title can be honest about
 * it.
 */
internal object PoolFallbacks {

    fun warmUp(pools: ExercisePools): ResolvedPool = when {
        pools.warmUp.isNotEmpty() -> ResolvedPool(pools.warmUp)
        pools.mobility.isNotEmpty() -> ResolvedPool(pools.mobility, note = "warm-up drawn from mobility work")
        else -> ResolvedPool(
            pools.eligible.sortedBy { it.metValue }.take(LOWEST_MET_FALLBACK_COUNT),
            note = "warm-up drawn from the easiest available movements",
        )
    }

    fun vigorous(pools: ExercisePools): ResolvedPool = when {
        pools.vigorous.isNotEmpty() -> ResolvedPool(pools.vigorous)
        pools.threshold.isNotEmpty() -> ResolvedPool(
            pools.threshold,
            cappedAt = IntensityAnchor.THRESHOLD,
            note = "no vigorous machine work available; intervals capped at threshold",
        )

        else -> steady(pools).let { fallback ->
            fallback.copy(
                cappedAt = IntensityAnchor.ZONE_2,
                note = "no threshold machine work available; intervals capped at Zone 2",
            )
        }
    }

    fun threshold(pools: ExercisePools): ResolvedPool = when {
        pools.threshold.isNotEmpty() -> ResolvedPool(pools.threshold)
        else -> steady(pools).let { fallback ->
            fallback.copy(
                cappedAt = IntensityAnchor.ZONE_2,
                note = "no threshold machine work available; surges capped at Zone 2",
            )
        }
    }

    fun steady(pools: ExercisePools): ResolvedPool = when {
        pools.steady.isNotEmpty() -> ResolvedPool(pools.steady)
        pools.cardio.isNotEmpty() -> ResolvedPool(
            pools.cardio,
            note = "steady work drawn from the whole machine-cardio range",
        )

        pools.strength.isNotEmpty() -> ResolvedPool(
            pools.strength,
            cappedAt = IntensityAnchor.ZONE_2,
            note = "no machine available; built as Pilates strength and control",
        )

        else -> ResolvedPool(
            pools.eligible.sortedBy { it.metValue }.take(LOWEST_MET_FALLBACK_COUNT),
            cappedAt = IntensityAnchor.RECOVERY,
            note = "only low-intensity movements available",
        )
    }

    fun mobility(pools: ExercisePools): ResolvedPool = when {
        pools.mobility.isNotEmpty() -> ResolvedPool(pools.mobility)
        else -> ResolvedPool(
            pools.eligible.sortedBy { it.metValue }.take(1),
            note = "cool-down drawn from the easiest available movement",
        )
    }

    /**
     * Pilates strength work for a recovery block, and therefore for a Pilates-only session.
     *
     * Restricted to movements the Compendium anchors no harder than Zone 2. Without that
     * restriction the pool includes `floor_pilates_mountain_climber_slow` (7.0 MET,
     * anchored threshold) and `floor_pilates_star_jumps` (7.5, vigorous), and a recovery
     * block prescribes every segment at RECOVERY intensity — so a recovery session came out
     * containing slow mountain climbers labelled "easy" (D-0038).
     *
     * Found by the anchor-versus-intensity invariant added after KI-0014, on its first run.
     * A consequence worth knowing about is recorded as KI-0020: those two movements are now
     * unreachable by the generator entirely, because the `floor_pilates` modality conflates
     * Pilates with bodyweight cardio.
     */
    fun strength(pools: ExercisePools): ResolvedPool {
        val easyEnough = pools.strength.filter {
            IntensityAnchor.of(it.modality, it.metValue).isAtMost(IntensityAnchor.ZONE_2)
        }
        return when {
            easyEnough.isNotEmpty() -> ResolvedPool(easyEnough)
            else -> mobility(pools)
        }
    }

    /** Spec §3: "then the lowest-MET three exercises in `eligible`". */
    private const val LOWEST_MET_FALLBACK_COUNT = 3
}

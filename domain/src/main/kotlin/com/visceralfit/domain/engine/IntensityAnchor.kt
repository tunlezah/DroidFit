package com.visceralfit.domain.engine

import com.visceralfit.domain.model.Modality
import kotlin.math.roundToLong

/**
 * How hard a modality is being worked at a given MET value, as anchored by the 2024
 * Adult Compendium of Physical Activities.
 *
 * WHY THIS IS A LOOKUP AND NOT A MET THRESHOLD (D-0020): the anchor is not monotonic
 * in MET. A spin class averages 9.0 MET and the Compendium anchors it at Zone 2
 * (code 01270); the elliptical at the same 9.0 MET is anchored vigorous (code 02049).
 * Any rule of the form "MET >= x is vigorous" therefore mislabels one of the two, and
 * the warm-up pool in `framework/07_workout_engine_spec.md` §3 depends on the
 * distinction: an easy spin is a legitimate final warm-up segment, a hard elliptical
 * interval is not. The worked example in §9 puts `spin_bike_seated_flat` (9.0 MET) in
 * the warm-up, which no MET threshold can produce.
 *
 * The table below mirrors `framework/data/met_values.json`, for the same reason the
 * interval constants duplicate `workout_templates.json`: the engine needs compile-time
 * constants and a reviewer needs one file to check them against.
 * `scripts/check_framework_data.py` asserts the two agree, so drift fails CI rather
 * than silently changing which exercises are eligible to warm up on.
 */
enum class IntensityAnchor(val id: String) {
    REST("rest"),
    RECOVERY("recovery"),
    ZONE_2("zone_2"),
    THRESHOLD("threshold"),
    VIGOROUS("vigorous"),
    ;

    /**
     * Comparisons use the declaration order, which runs easiest to hardest. Ordinals are
     * never persisted — [id] is the stored form — so reordering the entries is a source
     * change that cannot corrupt stored data.
     */
    fun isAtMost(other: IntensityAnchor): Boolean = ordinal <= other.ordinal

    /** True when this anchor is at least as hard as [other]. */
    fun isAtLeast(other: IntensityAnchor): Boolean = ordinal >= other.ordinal

    companion object {
        /**
         * The anchor for a (modality, MET) pair, or a MET-derived approximation when
         * the pair is absent from the table.
         *
         * The fallback exists so an exercise added to the catalogue without a matching
         * MET-table row still generates rather than crashing; the data check makes that
         * situation a CI failure, so in a shipped build every lookup hits the table.
         * The thresholds are the Compendium's own moderate/vigorous boundaries (D-0021).
         */
        fun of(modality: Modality, metValue: Double): IntensityAnchor =
            TABLE[key(modality, metValue)] ?: approximateFrom(metValue)

        private fun approximateFrom(metValue: Double): IntensityAnchor = when {
            metValue < RECOVERY_CEILING_MET -> RECOVERY
            metValue < ZONE_2_CEILING_MET -> ZONE_2
            metValue < THRESHOLD_CEILING_MET -> THRESHOLD
            else -> VIGOROUS
        }

        /**
         * MET values are keyed as tenths so the map never depends on floating-point
         * equality, and never on a locale-sensitive string conversion.
         */
        private fun key(modality: Modality, metValue: Double): Long =
            modality.ordinal * MODALITY_STRIDE + (metValue * TENTHS).roundToLong()

        private const val TENTHS = 10.0
        private const val MODALITY_STRIDE = 10_000L

        private const val RECOVERY_CEILING_MET = 3.6
        private const val ZONE_2_CEILING_MET = 6.0
        private const val THRESHOLD_CEILING_MET = 8.0

        /**
         * Mirror of `framework/data/met_values.json`. One entry per (modality, MET)
         * pair that appears in the shipped catalogue. Entries with a null modality in
         * that file (resting, generic aerobic) are omitted: no exercise carries them.
         */
        private val ENTRIES: List<Anchored> = listOf(
            Anchored(Modality.BODYWEIGHT, 1.8, RECOVERY),
            Anchored(Modality.BODYWEIGHT, 2.3, RECOVERY),
            Anchored(Modality.BODYWEIGHT, 2.8, RECOVERY),
            Anchored(Modality.BODYWEIGHT, 3.5, RECOVERY),
            Anchored(Modality.BODYWEIGHT, 3.8, ZONE_2),
            Anchored(Modality.BODYWEIGHT, 7.0, THRESHOLD),
            Anchored(Modality.BODYWEIGHT, 7.5, VIGOROUS),
            Anchored(Modality.REFORMER_PILATES, 2.3, RECOVERY),
            Anchored(Modality.REFORMER_PILATES, 2.8, RECOVERY),
            Anchored(Modality.REFORMER_PILATES, 3.8, ZONE_2),
            Anchored(Modality.ELLIPTICAL, 4.0, RECOVERY),
            Anchored(Modality.ELLIPTICAL, 5.0, ZONE_2),
            Anchored(Modality.ELLIPTICAL, 9.0, VIGOROUS),
            Anchored(Modality.SPIN_BIKE, 3.5, RECOVERY),
            Anchored(Modality.SPIN_BIKE, 4.0, RECOVERY),
            Anchored(Modality.SPIN_BIKE, 8.0, THRESHOLD),
            Anchored(Modality.SPIN_BIKE, 8.8, VIGOROUS),
            Anchored(Modality.SPIN_BIKE, 9.0, ZONE_2),
            Anchored(Modality.SPIN_BIKE, 10.8, THRESHOLD),
            Anchored(Modality.SPIN_BIKE, 12.5, VIGOROUS),
        )

        private val TABLE: Map<Long, IntensityAnchor> =
            ENTRIES.associate { key(it.modality, it.metValue) to it.anchor }

        private data class Anchored(
            val modality: Modality,
            val metValue: Double,
            val anchor: IntensityAnchor,
        )
    }
}

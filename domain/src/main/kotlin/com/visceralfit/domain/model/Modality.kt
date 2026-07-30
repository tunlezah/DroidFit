package com.visceralfit.domain.model

/**
 * An exercise modality the user can enable or disable.
 *
 * Adding a value here is the *only* supported way to extend the app with a new
 * exercise category (PRD REQ-010, "future-proof for additional categories").
 * When you add one you must also:
 *   1. add a [requiresEquipment] answer,
 *   2. add MET values to `framework/data/met_values.json`,
 *   3. seed at least 12 exercises for it in `exercises_seed.json`,
 *   4. record the addition in /project_memory/decisions.md.
 *
 * The stored form is [id]; never persist [Modality.name] or the ordinal, because
 * both change when the enum is reordered.
 */
enum class Modality(
    val id: String,
    val requiresEquipment: Boolean,
) {
    FLOOR_PILATES("floor_pilates", requiresEquipment = false),
    REFORMER_PILATES("reformer_pilates", requiresEquipment = true),
    ELLIPTICAL("elliptical", requiresEquipment = true),
    SPIN_BIKE("spin_bike", requiresEquipment = true),
    ;

    /** True when this modality is a continuous machine-based cardio modality. */
    val isMachineCardio: Boolean get() = this == ELLIPTICAL || this == SPIN_BIKE

    companion object {
        fun fromId(id: String): Modality? = entries.firstOrNull { it.id == id }

        /**
         * Modalities enabled on a fresh install. Reformer is excluded because it
         * needs a machine most users will not own (ADR-0004).
         */
        val DEFAULT_ENABLED: Set<Modality> = setOf(FLOOR_PILATES, ELLIPTICAL, SPIN_BIKE)
    }
}

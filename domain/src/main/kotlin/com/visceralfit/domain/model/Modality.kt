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
    /**
     * Floor and bodyweight work: mat core and mobility at one end, calisthenics cardio —
     * jumping, mountain climbers, burpees — at the other. No equipment.
     *
     * WAS `FLOOR_PILATES`, RENAMED IN D-0039. That name was wrong in a way that mattered:
     * it forced the whole category under the Pilates evidence constraint, so a user with no
     * machine could only ever be given an easy session, even though jumping at 7.5 MET is
     * unambiguously aerobic work. What makes a movement aerobic-capable is its intensity
     * anchor, not the label on its category — see [supportsAerobicWork].
     */
    BODYWEIGHT("bodyweight", requiresEquipment = false),
    REFORMER_PILATES("reformer_pilates", requiresEquipment = true),
    ELLIPTICAL("elliptical", requiresEquipment = true),
    SPIN_BIKE("spin_bike", requiresEquipment = true),
    ;

    /** True when this modality is a continuous machine-based cardio modality. */
    val isMachineCardio: Boolean get() = this == ELLIPTICAL || this == SPIN_BIKE

    /**
     * True when a session may prescribe *aerobic* work on this modality — Zone 2, threshold
     * or vigorous efforts, the kind the evidence base credits with reducing visceral fat.
     *
     * This is where the Pilates constraint now lives, and it is structural rather than a
     * special case in the generator. `wang2021pilates` found real body-composition and
     * strength benefit from Pilates but **no** significant waist-circumference effect, so a
     * reformer session must never be presented as equivalent to hard cardio (REQ-004,
     * `framework/02_evidence_base.md` §1.5). Returning false here is what makes that true
     * everywhere at once.
     *
     * Bodyweight work returns true because the category genuinely contains both: a supine
     * chest opener is not aerobic and a set of star jumps is. Which of the two a given
     * exercise is comes from its [com.visceralfit.domain.engine.IntensityAnchor], so the
     * per-exercise question is answered per exercise and the per-modality question here.
     */
    val supportsAerobicWork: Boolean get() = this != REFORMER_PILATES

    companion object {
        fun fromId(id: String): Modality? = entries.firstOrNull { it.id == id }

        /**
         * Modalities enabled on a fresh install: all of them (D-0041, superseding ADR-0004).
         *
         * The reformer used to be excluded on the reasoning that most users will not own one.
         * The operator does (UF-0008), and more to the point the exclusion was solving the
         * wrong problem: whether a modality is *usable* is already answered by
         * [requiresEquipment] and the equipment the user has marked, so defaulting it off as
         * well hid content behind two switches instead of one.
         *
         * The rule that actually matters is REQ-011 — at least one modality must stay
         * enabled — and it is enforced when toggling, not by the default.
         */
        val DEFAULT_ENABLED: Set<Modality> = entries.toSet()
    }
}

package com.visceralfit.domain.model

/**
 * An exercise modality the user can enable or disable.
 *
 * Adding a value here is the *only* supported way to extend the app with a new
 * exercise category (PRD REQ-010, "future-proof for additional categories").
 * When you add one you must also:
 *   1. answer [requiresEquipment] and [supportsAerobicWork] — both are constructor
 *      arguments precisely so a new modality cannot compile without answering them,
 *   2. add MET values to `framework/data/met_values.json`, and mirror them in
 *      [com.visceralfit.domain.engine.IntensityAnchor],
 *   3. seed enough exercises for it in `exercises_seed.json` to satisfy the minimums in
 *      `framework/08_exercise_library_spec.md` §1,
 *   4. add its display name in Settings and on the home screen,
 *   5. record the addition in /project_memory/decisions.md.
 *
 * The stored form is [id]; never persist [Modality.name] or the ordinal, because
 * both change when the enum is reordered.
 */
enum class Modality(
    val id: String,
    val requiresEquipment: Boolean,
    /**
     * True when a session may prescribe *aerobic* work on this modality — Zone 2, threshold
     * or vigorous efforts, the kind the evidence base credits with reducing visceral fat.
     *
     * This is where the Pilates constraint lives, and it is structural rather than a special
     * case in the generator. `wang2021pilates` found real body-composition and strength
     * benefit from Pilates but **no** significant waist-circumference effect, so a Pilates
     * session must never be presented as equivalent to hard cardio (REQ-004,
     * `framework/02_evidence_base.md` §1.5). Answering false here is what makes that true
     * everywhere at once.
     *
     * A CONSTRUCTOR ARGUMENT RATHER THAN A DERIVED PROPERTY (D-0042, closing KI-0022): it
     * used to be `this != REFORMER_PILATES`, which silently *defaulted* every future
     * modality to aerobic-capable. That is the dangerous direction — a new Pilates or
     * stretching category would have been credited with cardio benefit by omission. Asking
     * the question in the constructor makes forgetting it a compile error.
     */
    val supportsAerobicWork: Boolean,
) {
    /**
     * Bodyweight work: general core and mobility at one end, calisthenics cardio — jumping,
     * mountain climbers, burpees — at the other. No equipment.
     *
     * WAS `FLOOR_PILATES`, RENAMED IN D-0039. That name was wrong in a way that mattered:
     * it forced the whole category under the Pilates evidence constraint, so a user with no
     * machine could only ever be given an easy session, even though jumping at 7.5 MET is
     * unambiguously aerobic work. What makes a movement aerobic-capable is its intensity
     * anchor, not the label on its category.
     *
     * [supportsAerobicWork] is true because the category genuinely contains both: a supine
     * chest opener is not aerobic and a set of star jumps is. Which of the two a given
     * exercise is comes from its [com.visceralfit.domain.engine.IntensityAnchor], so the
     * per-exercise question is answered per exercise and the per-modality question here.
     */
    BODYWEIGHT("bodyweight", requiresEquipment = false, supportsAerobicWork = true),

    /**
     * The classical Pilates mat repertoire and its named fundamentals: the hundred, roll up,
     * single and double leg stretch, side kick series, teaser, jack knife, neck pull. No
     * equipment beyond a mat.
     *
     * SPLIT OUT OF [BODYWEIGHT] IN D-0042. The two were one category and should not have
     * been. They differ in the only two ways a category matters here — the user picks them
     * separately ("an easy mat day" is a different intention from "a hard bodyweight day"),
     * and the evidence that applies to them is different. Holding both under one modality
     * forced a single answer to [supportsAerobicWork] for movements as unlike each other as
     * lateral breathing and burpees.
     *
     * [supportsAerobicWork] is false: this is the Pilates repertoire, so `wang2021pilates`
     * governs it and a mat session is never framed as cardio. That is not a demotion — mat
     * work is still prescribed as strength and control, which is what it is.
     */
    MAT_PILATES("mat_pilates", requiresEquipment = false, supportsAerobicWork = false),
    REFORMER_PILATES("reformer_pilates", requiresEquipment = true, supportsAerobicWork = false),
    ELLIPTICAL("elliptical", requiresEquipment = true, supportsAerobicWork = true),
    SPIN_BIKE("spin_bike", requiresEquipment = true, supportsAerobicWork = true),
    ;

    /** True when this modality is a continuous machine-based cardio modality. */
    val isMachineCardio: Boolean get() = this == ELLIPTICAL || this == SPIN_BIKE

    /** True when this modality is Pilates, mat or reformer, and so under `wang2021pilates`. */
    val isPilates: Boolean get() = this == MAT_PILATES || this == REFORMER_PILATES

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

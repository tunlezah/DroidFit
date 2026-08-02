package com.visceralfit.domain.engine

import com.visceralfit.domain.model.Exercise
import com.visceralfit.domain.model.ExperienceLevel
import com.visceralfit.domain.model.Modality

/**
 * The shipped exercise catalogue, reduced to the fields the engine actually reads.
 *
 * WHY A FIXTURE AND NOT THE ASSET: `:domain` is a pure Kotlin module with no Android
 * dependency and no access to `app/src/main/assets`. That is the point of the module
 * boundary — the engine's tests run on the JVM in milliseconds, with no device and no
 * Robolectric.
 *
 * WHY IT IS SAFE: a fixture that had drifted from the shipped content would turn the
 * golden-file test into a test of a catalogue nobody ships, which is worse than no test.
 * So `scripts/check_framework_data.py` asserts that every row below matches
 * `exercises_seed.json` field for field, and CI runs it before the tests.
 *
 * Format, one exercise per row: `id|modality|difficulty|met|caution_tags`.
 * Caution tags are comma-separated and alphabetical. The display name and every prose
 * field is a placeholder: the engine never reads them, and a second copy of ~65
 * exercises' worth of safety notes would be health guidance to keep in step in two
 * places. `ExerciseCatalogueValidationTest` in `:app` is what checks the prose.
 *
 * GENERATED from the asset. Regenerate rather than hand-editing.
 */
internal object CatalogueFixture {

    val ALL: List<Exercise> by lazy { TABLE.trimIndent().lines().map(::parse) }

    fun forModalities(vararg modalities: Modality): List<Exercise> =
        ALL.filter { it.modality in modalities }

    private fun parse(row: String): Exercise {
        val fields = row.split('|')
        val id = fields[ID]
        val tags = fields[TAGS]
        return Exercise(
            id = id,
            name = id,
            modality = checkNotNull(Modality.fromId(fields[MODALITY])) { "unknown modality in fixture row: $row" },
            difficulty = checkNotNull(ExperienceLevel.fromId(fields[LEVEL])) { "unknown level in fixture row: $row" },
            metValue = fields[MET].toDouble(),
            howTo = PLACEHOLDER,
            spokenInstruction = id,
            safetyNotes = PLACEHOLDER,
            commonMistakes = PLACEHOLDER,
            musclesWorked = PLACEHOLDER,
            illustrationId = id,
            cautionTags = if (tags.isEmpty()) emptySet() else tags.split(',').toSet(),
        )
    }

    private val PLACEHOLDER = listOf("See the shipped catalogue for the authored content.")

    private const val ID = 0
    private const val MODALITY = 1
    private const val LEVEL = 2
    private const val MET = 3
    private const val TAGS = 4

    private const val TABLE = """
        bodyweight_bird_dog|bodyweight|beginner|2.8|knee,wrist
        bodyweight_burpees|bodyweight|advanced|7.5|cardiac_caution,knee,lower_back,shoulder,wrist
        bodyweight_cat_cow|bodyweight|beginner|2.3|knee,wrist
        bodyweight_clam_shell|bodyweight|beginner|2.8|
        bodyweight_dead_bug|bodyweight|beginner|2.8|lower_back
        bodyweight_fast_feet|bodyweight|beginner|7.0|ankle,cardiac_caution
        bodyweight_high_knees|bodyweight|intermediate|7.5|ankle,cardiac_caution,knee
        bodyweight_jumping_jacks|bodyweight|beginner|7.5|ankle,cardiac_caution,knee
        bodyweight_mountain_climber_slow|bodyweight|intermediate|7.0|lower_back,shoulder,wrist
        bodyweight_plank_hold|bodyweight|intermediate|2.8|lower_back,shoulder
        bodyweight_plank_jacks|bodyweight|intermediate|7.0|cardiac_caution,lower_back,shoulder,wrist
        bodyweight_side_plank_full|bodyweight|advanced|3.8|shoulder,wrist
        bodyweight_side_plank_knees|bodyweight|intermediate|3.8|shoulder,wrist
        bodyweight_skater_hops|bodyweight|intermediate|7.0|ankle,balance,cardiac_caution,knee
        bodyweight_squat_jumps|bodyweight|advanced|7.5|ankle,cardiac_caution,knee
        bodyweight_star_jumps|bodyweight|advanced|7.5|ankle,balance,cardiac_caution,knee
        cooldown_hip_flexor_kneel|bodyweight|beginner|2.3|knee
        cooldown_seated_forward_fold|bodyweight|beginner|2.3|
        cooldown_supine_knee_hug|bodyweight|beginner|2.3|
        cooldown_thoracic_opener|bodyweight|beginner|2.3|
        elliptical_arms_and_legs_surge|elliptical|intermediate|9.0|cardiac_caution,shoulder
        elliptical_cadence_build|elliptical|beginner|5.0|
        elliptical_easy_spin_down|elliptical|beginner|4.0|
        elliptical_hard_interval|elliptical|intermediate|9.0|cardiac_caution
        elliptical_high_resistance_push|elliptical|advanced|9.0|cardiac_caution,knee
        elliptical_long_hard_effort|elliptical|advanced|9.0|cardiac_caution
        elliptical_reverse_stride|elliptical|intermediate|5.0|balance,knee
        elliptical_standing_tall_hold|elliptical|advanced|5.0|balance
        elliptical_steady_zone2|elliptical|beginner|5.0|
        elliptical_strong_effort|elliptical|beginner|9.0|cardiac_caution
        elliptical_warm_up_glide|elliptical|beginner|4.0|
        mat_breathing_lateral|mat_pilates|beginner|1.8|
        mat_criss_cross|mat_pilates|intermediate|3.8|lower_back,neck
        mat_double_leg_stretch|mat_pilates|intermediate|3.8|lower_back,neck
        mat_hundred_prep|mat_pilates|beginner|2.8|neck
        mat_imprint_and_release|mat_pilates|beginner|1.8|
        mat_jack_knife|mat_pilates|advanced|3.8|lower_back,neck,shoulder
        mat_leg_slides|mat_pilates|beginner|2.8|
        mat_neck_pull|mat_pilates|advanced|3.8|lower_back,neck
        mat_roll_up|mat_pilates|intermediate|3.8|lower_back,neck
        mat_shoulder_bridge_march|mat_pilates|intermediate|3.8|neck
        mat_shoulder_bridge|mat_pilates|beginner|2.8|
        mat_side_kick_series|mat_pilates|intermediate|3.8|lower_back,shoulder
        mat_single_leg_stretch|mat_pilates|intermediate|3.8|lower_back,neck
        mat_spine_stretch_forward|mat_pilates|beginner|2.3|lower_back
        mat_spine_twist_supine|mat_pilates|beginner|2.3|lower_back
        mat_swan_prep|mat_pilates|intermediate|2.8|lower_back
        mat_swimming_prep|mat_pilates|intermediate|2.8|lower_back
        mat_teaser_prep|mat_pilates|advanced|3.8|lower_back,neck
        reformer_arms_supine_press|reformer_pilates|beginner|2.3|shoulder
        reformer_bridging|reformer_pilates|beginner|2.8|
        reformer_elephant|reformer_pilates|intermediate|3.8|lower_back,wrist
        reformer_footwork_arches|reformer_pilates|beginner|2.8|ankle,knee
        reformer_footwork_toes|reformer_pilates|beginner|2.8|knee
        reformer_knee_stretches|reformer_pilates|advanced|3.8|knee,lower_back,wrist
        reformer_leg_circles|reformer_pilates|intermediate|2.8|lower_back
        reformer_long_stretch|reformer_pilates|advanced|3.8|lower_back,shoulder,wrist
        reformer_mermaid_stretch|reformer_pilates|beginner|2.3|
        reformer_pelvic_tilts|reformer_pilates|beginner|2.8|
        reformer_short_box_round|reformer_pilates|intermediate|2.8|lower_back
        reformer_spine_stretch_forward|reformer_pilates|intermediate|2.8|
        reformer_teaser|reformer_pilates|advanced|3.8|lower_back,neck
        spin_bike_easy_spin|spin_bike|beginner|3.5|
        spin_bike_high_cadence_surge|spin_bike|intermediate|8.8|cardiac_caution,knee
        spin_bike_jumps|spin_bike|advanced|8.8|cardiac_caution,hip,knee
        spin_bike_recovery_spin|spin_bike|beginner|4.0|
        spin_bike_seated_climb|spin_bike|intermediate|10.8|knee
        spin_bike_seated_flat|spin_bike|beginner|9.0|knee
        spin_bike_single_leg_focus|spin_bike|advanced|8.0|knee
        spin_bike_smooth_circles|spin_bike|beginner|3.5|
        spin_bike_sprint|spin_bike|advanced|12.5|cardiac_caution,hip,knee
        spin_bike_standing_climb|spin_bike|intermediate|10.8|hip,knee
        spin_bike_standing_sprint|spin_bike|advanced|12.5|balance,cardiac_caution,hip,knee
        spin_bike_tempo_seated|spin_bike|intermediate|8.0|knee
        spin_bike_warm_up_roll|spin_bike|beginner|4.0|
        warmup_march_in_place|bodyweight|beginner|3.5|
    """
}

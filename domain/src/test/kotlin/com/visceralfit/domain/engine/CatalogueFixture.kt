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
        cooldown_hip_flexor_kneel|floor_pilates|beginner|2.3|knee
        cooldown_seated_forward_fold|floor_pilates|beginner|2.3|
        cooldown_supine_knee_hug|floor_pilates|beginner|2.3|
        cooldown_thoracic_opener|floor_pilates|beginner|2.3|
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
        floor_pilates_bird_dog|floor_pilates|beginner|2.8|knee,wrist
        floor_pilates_breathing_lateral|floor_pilates|beginner|1.8|
        floor_pilates_cat_cow|floor_pilates|beginner|1.8|knee,wrist
        floor_pilates_clam_shell|floor_pilates|beginner|2.8|
        floor_pilates_criss_cross|floor_pilates|intermediate|3.8|lower_back,neck
        floor_pilates_dead_bug|floor_pilates|beginner|2.8|lower_back
        floor_pilates_double_leg_stretch|floor_pilates|intermediate|3.8|lower_back,neck
        floor_pilates_glute_bridge|floor_pilates|beginner|2.8|
        floor_pilates_hundred_prep|floor_pilates|beginner|2.8|neck
        floor_pilates_jack_knife|floor_pilates|advanced|3.8|lower_back,neck,shoulder
        floor_pilates_leg_slides|floor_pilates|beginner|2.8|
        floor_pilates_mountain_climber_slow|floor_pilates|intermediate|7.0|lower_back,shoulder,wrist
        floor_pilates_plank_hold|floor_pilates|intermediate|2.8|lower_back,shoulder
        floor_pilates_roll_up|floor_pilates|intermediate|3.8|lower_back,neck
        floor_pilates_shoulder_bridge_march|floor_pilates|intermediate|3.8|neck
        floor_pilates_side_kick_series|floor_pilates|intermediate|3.8|lower_back,shoulder
        floor_pilates_side_plank_full|floor_pilates|advanced|3.8|shoulder,wrist
        floor_pilates_side_plank_knees|floor_pilates|intermediate|3.8|shoulder,wrist
        floor_pilates_single_leg_stretch|floor_pilates|intermediate|3.8|lower_back,neck
        floor_pilates_spine_twist_supine|floor_pilates|beginner|2.3|lower_back
        floor_pilates_star_jumps|floor_pilates|advanced|7.5|ankle,balance,cardiac_caution,knee
        floor_pilates_swimming_prep|floor_pilates|intermediate|2.8|lower_back
        floor_pilates_teaser_prep|floor_pilates|advanced|3.8|lower_back,neck
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
        warmup_march_in_place|floor_pilates|beginner|3.5|
    """
}

package com.visceralfit.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Everything the user can configure. Persisted in DataStore, never in Room, because
 * these are scalar settings rather than records (ADR-0006).
 */
data class UserPreferences(
    val enabledModalities: Set<Modality> = Modality.DEFAULT_ENABLED,
    val level: ExperienceLevel = ExperienceLevel.BEGINNER,
    val defaultStyle: WorkoutStyle = WorkoutStyle.MIXED,
    val defaultDuration: Duration = 20.minutes,
    val coaching: CoachingPreferences = CoachingPreferences(),
    val display: DisplayPreferences = DisplayPreferences(),
    val body: BodyPreferences = BodyPreferences(),
    /** Weekly minutes the user is aiming for. Default is the WHO lower bound (REQ-002). */
    val weeklyMinutesGoal: Int = DEFAULT_WEEKLY_MINUTES_GOAL,
    /** True once the user has read and dismissed the medical-safety notice. */
    val safetyNoticeAcknowledged: Boolean = false,
    /**
     * The hardest effort any generated session may prescribe.
     *
     * Defaults to [EffortCeiling.THRESHOLD] rather than to the hardest available, and that
     * default is a safety decision rather than a taste one (A-0007, D-0040). The app cannot
     * know whether a user is cleared for 85–95% of maximum heart rate, so it does not assume
     * they are. Raising the ceiling is a deliberate act on a screen that says what it means.
     */
    val effortCeiling: EffortCeiling = EffortCeiling.THRESHOLD,
) {
    /** Modalities that are both enabled and usable given the equipment the user has. */
    fun usableModalities(): Set<Modality> =
        enabledModalities.filterTo(mutableSetOf()) { !it.requiresEquipment || it in body.availableEquipment }

    companion object {
        /** WHO 2020 lower bound for moderate-intensity activity, in minutes per week. */
        const val DEFAULT_WEEKLY_MINUTES_GOAL: Int = 150
    }
}

/**
 * Independently switchable coaching cues (PRD REQ-050). Each flag is its own
 * setting on purpose — bundling them was rejected in ADR-0007.
 */
data class CoachingPreferences(
    val speechEnabled: Boolean = true,
    val announceNextExercise: Boolean = true,
    val announceCountdown: Boolean = true,
    val announceHalfway: Boolean = true,
    val announceRemainingTime: Boolean = false,
    val motivationalPrompts: Boolean = false,
    val announceRestCountdown: Boolean = true,
    /** Read the full technique cues aloud, not just the one-line spoken cue. */
    val speakFullInstructions: Boolean = false,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    /** Play a tone at segment boundaries even when speech is off. */
    val cueTones: Boolean = true,
    val hapticCues: Boolean = true,
) {
    init {
        require(speechRate in MIN_RATE..MAX_RATE) { "speechRate out of range: $speechRate" }
        require(speechPitch in MIN_RATE..MAX_RATE) { "speechPitch out of range: $speechPitch" }
    }

    companion object {
        const val MIN_RATE = 0.5f
        const val MAX_RATE = 2.0f
    }
}

data class DisplayPreferences(
    /** Hold the screen awake for the whole session (PRD REQ-070). */
    val keepScreenOn: Boolean = true,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    /** Pure-black surfaces, which cost less battery on the Edge 60's pOLED panel. */
    val amoledDarkMode: Boolean = true,
    val dynamicColour: Boolean = true,
    /** Larger timer digits and controls for use at arm's length on a bike or elliptical. */
    val machineMode: Boolean = false,
)

/**
 * How hard the app is allowed to ask the user to work.
 *
 * Expressed as a user setting rather than derived from experience level, because they are
 * different questions: `ExperienceLevel` is about technique and coordination, this is about
 * cardiovascular clearance. Someone can be an advanced Pilates practitioner and still have a
 * reason not to reach 90% of maximum heart rate.
 *
 * The stored form is [id]. Percentages are of estimated maximum heart rate, and match the
 * anchors in `framework/02_evidence_base.md` §Intensity anchors.
 */
enum class EffortCeiling(val id: String) {
    /** Nothing above Zone 2 — conversational pace, 60–70%. */
    STEADY("steady"),

    /** Up to threshold: 76–84%, hard but sustainable. "Moderately vigorous". */
    THRESHOLD("threshold"),

    /** Up to vigorous intervals: 85–95%, the intensity `helgerud2007` prescribes. */
    VIGOROUS("vigorous"),
    ;

    companion object {
        fun fromId(id: String): EffortCeiling? = entries.firstOrNull { it.id == id }
    }
}

enum class ThemePreference(val id: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
}

data class BodyPreferences(
    /**
     * Equipment the user has access to. Defaults to everything (D-0041): the operator has an
     * elliptical, a spin bike and a reformer (UF-0008), and a user who does not can turn one
     * off in one tap — which is a better first run than a user who owns a reformer having to
     * discover why none of its thirteen exercises ever appear.
     */
    val availableEquipment: Set<Modality> = Modality.entries.filterTo(mutableSetOf()) { it.requiresEquipment },
    /** Used only for the energy estimate; null means the estimate is suppressed. */
    val bodyMassKg: Double? = null,
    val ageYears: Int? = null,
    val units: UnitSystem = UnitSystem.METRIC,
    /** Contraindication tags to exclude from generation, e.g. "lower_back". */
    val avoidTags: Set<String> = emptySet(),
)

enum class UnitSystem(val id: String) {
    METRIC("metric"),
    IMPERIAL("imperial"),
}

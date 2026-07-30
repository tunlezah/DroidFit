package com.visceralfit.domain.model

/**
 * The intensity architecture of a session.
 *
 * Each style maps to a documented, evidence-backed prescription. The mapping
 * lives in `/framework/02_evidence_base.md` and must not be changed in code
 * without a matching edit there plus a /project_memory/decisions.md entry.
 */
enum class WorkoutStyle(val id: String) {
    /** Repeated bouts at 85–95% HRmax with active recovery between them. */
    HIIT("hiit"),

    /** Continuous work at roughly 60–70% HRmax — conversational, nasal-breathing pace. */
    ZONE_2("zone_2"),

    /** A Zone 2 base with short vigorous surges; the default for most users. */
    MIXED("mixed"),

    /** Low-intensity mobility and controlled Pilates work. Counts toward volume, not intensity. */
    RECOVERY("recovery"),
    ;

    companion object {
        fun fromId(id: String): WorkoutStyle? = entries.firstOrNull { it.id == id }
    }
}

/** Self-reported training experience, used to gate exercise selection and progression. */
enum class ExperienceLevel(val id: String, val order: Int) {
    BEGINNER("beginner", 0),
    INTERMEDIATE("intermediate", 1),
    ADVANCED("advanced", 2),
    ;

    /** True when a user at this level may attempt content authored for [required]. */
    fun canAttempt(required: ExperienceLevel): Boolean = order >= required.order

    companion object {
        fun fromId(id: String): ExperienceLevel? = entries.firstOrNull { it.id == id }
    }
}

/** How hard a block should feel, expressed as a range so it works with or without a HR strap. */
data class IntensityTarget(
    /** Borg CR10 rating of perceived exertion, inclusive range. */
    val rpeRange: IntRange,
    /** Percentage of estimated maximum heart rate, inclusive range. */
    val percentHrMaxRange: IntRange,
) {
    init {
        require(rpeRange.first in 1..10 && rpeRange.last in 1..10) {
            "RPE must sit on the Borg CR10 scale, was $rpeRange"
        }
        require(percentHrMaxRange.first in 30..100 && percentHrMaxRange.last in 30..100) {
            "%HRmax outside plausible bounds, was $percentHrMaxRange"
        }
    }

    companion object {
        /** Values sourced from /framework/02_evidence_base.md §Intensity anchors. */
        val RECOVERY = IntensityTarget(rpeRange = 2..3, percentHrMaxRange = 50..60)
        val ZONE_2 = IntensityTarget(rpeRange = 3..4, percentHrMaxRange = 60..70)
        val THRESHOLD = IntensityTarget(rpeRange = 6..7, percentHrMaxRange = 76..84)
        val VIGOROUS = IntensityTarget(rpeRange = 8..9, percentHrMaxRange = 85..95)
    }
}

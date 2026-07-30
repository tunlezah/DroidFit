package com.visceralfit.domain.model

import kotlin.time.Duration

/**
 * A generated, immutable session plan. The workout player consumes this and never
 * mutates it; user changes produce a new [Workout] (PRD REQ-030).
 */
data class Workout(
    val id: String,
    val title: String,
    val style: WorkoutStyle,
    val modalities: Set<Modality>,
    val level: ExperienceLevel,
    val blocks: List<WorkoutBlock>,
    /** The duration the user asked for. [actualDuration] may differ by up to the tolerance in REQ-032. */
    val requestedDuration: Duration,
    /** Seed used to generate this workout, so any session can be reproduced exactly. */
    val generationSeed: Long,
    /**
     * What the generator had to do differently from the request, in plain language, so
     * the UI can say so rather than presenting a substituted session as the one that was
     * asked for. Empty when the session is exactly what was requested.
     *
     * Examples: an intensity cap because no vigorous machine work was available, or a
     * style downgrade because the duration could not hold any interval template. See
     * `framework/07_workout_engine_spec.md` §3.
     */
    val buildNotes: List<String> = emptyList(),
) {
    val actualDuration: Duration get() = blocks.fold(Duration.ZERO) { acc, b -> acc + b.duration }

    val segments: List<Segment> get() = blocks.flatMap { it.segments }

    val workSegments: List<Segment> get() = segments.filter { it.kind == SegmentKind.WORK }
}

/** A phase of the session: warm-up, main work, cool-down. */
data class WorkoutBlock(
    val kind: BlockKind,
    val segments: List<Segment>,
) {
    val duration: Duration get() = segments.fold(Duration.ZERO) { acc, s -> acc + s.duration }
}

enum class BlockKind(val id: String) {
    WARM_UP("warm_up"),
    MAIN("main"),
    COOL_DOWN("cool_down"),
}

/**
 * The atomic unit the timer counts down. Exactly one segment is active at a time.
 */
data class Segment(
    val kind: SegmentKind,
    val duration: Duration,
    /** Null for pure rest/transition segments. */
    val exercise: Exercise?,
    val intensity: IntensityTarget,
    /**
     * Position within a repeated set, 1-based, for cues like "round 3 of 6".
     * Null when the segment is not part of a repeating structure.
     */
    val roundIndex: Int? = null,
    val roundTotal: Int? = null,
) {
    init {
        require(duration.isPositive()) { "Segment duration must be positive" }
        if (kind == SegmentKind.WORK) {
            requireNotNull(exercise) { "WORK segments must name an exercise" }
        }
    }
}

enum class SegmentKind(val id: String) {
    /** The effort itself. Always names an exercise. */
    WORK("work"),

    /** Active recovery inside an interval set — the user keeps moving. */
    ACTIVE_RECOVERY("active_recovery"),

    /** Full rest between sets or exercises. */
    REST("rest"),

    /** Time budgeted for changing position or equipment. */
    TRANSITION("transition"),
}

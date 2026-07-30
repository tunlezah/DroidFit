package com.visceralfit.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.time.Duration

/** A completed (or abandoned) session as recorded in history. */
data class CompletedSession(
    val id: Long,
    val workoutId: String,
    val title: String,
    val style: WorkoutStyle,
    val modalities: Set<Modality>,
    val startedAt: Instant,
    val completedAt: Instant,
    /** Time actually spent moving, excluding paused time. */
    val activeDuration: Duration,
    /** Null when body mass is unknown — the UI must render "—", never 0 (REQ-081). */
    val estimatedKilocalories: Int?,
    /** Borg CR10 rating the user gave afterwards, if they gave one. */
    val perceivedExertion: Int?,
    val completionRatio: Float,
    val note: String? = null,
) {
    val wasCompleted: Boolean get() = completionRatio >= COMPLETION_THRESHOLD

    companion object {
        /** Sessions past this fraction count toward streaks and volume. */
        const val COMPLETION_THRESHOLD = 0.7f
    }
}

/**
 * An optional self-measurement. Waist circumference is included because it is a
 * validated *proxy* trend; the app never converts it into a visceral fat figure
 * (PRD "Do NOT estimate visceral fat directly", REQ-090).
 */
data class BodyMeasurement(
    val id: Long,
    val recordedOn: LocalDate,
    val kind: MeasurementKind,
    val value: Double,
    val unit: String,
)

enum class MeasurementKind(val id: String) {
    BODY_MASS("body_mass"),
    WAIST_CIRCUMFERENCE("waist_circumference"),
    HIP_CIRCUMFERENCE("hip_circumference"),
    CHEST_CIRCUMFERENCE("chest_circumference"),
    THIGH_CIRCUMFERENCE("thigh_circumference"),
}

/** Rolling training-load summary used by the recovery recommender. */
data class TrainingLoadSummary(
    val weekStart: LocalDate,
    val totalMinutes: Int,
    val vigorousMinutes: Int,
    val sessionCount: Int,
    val consecutiveVigorousDays: Int,
)

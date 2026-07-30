package com.visceralfit.data.mapper

import com.visceralfit.core.database.entity.ExerciseEntity
import com.visceralfit.core.database.entity.MeasurementEntity
import com.visceralfit.core.database.entity.SessionEntity
import com.visceralfit.domain.model.BodyMeasurement
import com.visceralfit.domain.model.CompletedSession
import com.visceralfit.domain.model.Exercise
import com.visceralfit.domain.model.ExperienceLevel
import com.visceralfit.domain.model.MeasurementKind
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.WorkoutStyle
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Entity <-> domain mapping.
 *
 * Rows whose stored enum id is no longer recognised are dropped by the callers
 * rather than coerced to a default. A row referencing a modality this build does
 * not know about is data from a newer version; silently re-labelling it as
 * "floor Pilates" would corrupt the user's history, so [toDomainOrNull] returns null
 * and the repository filters it out and logs a count.
 */
fun ExerciseEntity.toDomainOrNull(): Exercise? {
    val resolvedModality = Modality.fromId(modality) ?: return null
    val resolvedDifficulty = ExperienceLevel.fromId(difficulty) ?: return null
    return Exercise(
        id = id,
        name = name,
        modality = resolvedModality,
        difficulty = resolvedDifficulty,
        metValue = metValue,
        howTo = howTo,
        spokenInstruction = spokenInstruction,
        safetyNotes = safetyNotes,
        commonMistakes = commonMistakes,
        musclesWorked = musclesWorked,
        illustrationId = illustrationId,
        cautionTags = cautionTags.toSet(),
        isPerSide = isPerSide,
        evidenceKeys = evidenceKeys,
    )
}

fun SessionEntity.toDomainOrNull(): CompletedSession? {
    val resolvedStyle = WorkoutStyle.fromId(style) ?: return null
    return CompletedSession(
        id = id,
        workoutId = workoutId,
        title = title,
        style = resolvedStyle,
        modalities = modalities.mapNotNull(Modality::fromId).toSet(),
        startedAt = Instant.fromEpochMilliseconds(startedAtEpochMs),
        completedAt = Instant.fromEpochMilliseconds(completedAtEpochMs),
        activeDuration = activeSeconds.seconds,
        estimatedKilocalories = estimatedKcal,
        perceivedExertion = perceivedExertion,
        completionRatio = completionRatio,
        note = note,
    )
}

fun CompletedSession.toEntity(): SessionEntity = SessionEntity(
    id = id,
    workoutId = workoutId,
    title = title,
    style = style.id,
    modalities = modalities.map { it.id },
    startedAtEpochMs = startedAt.toEpochMilliseconds(),
    completedAtEpochMs = completedAt.toEpochMilliseconds(),
    activeSeconds = activeDuration.inWholeSeconds,
    estimatedKcal = estimatedKilocalories,
    perceivedExertion = perceivedExertion,
    completionRatio = completionRatio,
    note = note,
)

fun MeasurementEntity.toDomainOrNull(): BodyMeasurement? {
    val resolvedKind = MeasurementKind.entries.firstOrNull { it.id == kind } ?: return null
    return BodyMeasurement(
        id = id,
        recordedOn = LocalDate.fromEpochDays(recordedOnEpochDay.toInt()),
        kind = resolvedKind,
        value = value,
        unit = unit,
    )
}

fun BodyMeasurement.toEntity(): MeasurementEntity = MeasurementEntity(
    id = id,
    kind = kind.id,
    recordedOnEpochDay = recordedOn.toEpochDays().toLong(),
    value = value,
    unit = unit,
)

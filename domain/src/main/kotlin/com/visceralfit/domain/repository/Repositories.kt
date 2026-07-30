package com.visceralfit.domain.repository

import com.visceralfit.domain.model.BodyMeasurement
import com.visceralfit.domain.model.CompletedSession
import com.visceralfit.domain.model.Exercise
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.TrainingLoadSummary
import com.visceralfit.domain.model.UserPreferences
import com.visceralfit.domain.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Ports the domain layer depends on. Implementations live in `:data`; the domain
 * module must never import them (enforced by module boundaries, not convention).
 */
interface ExerciseRepository {
    /** All seeded exercises. Emits again if the seed is upgraded on app update. */
    fun observeAll(): Flow<List<Exercise>>

    suspend fun getById(id: String): Exercise?

    suspend fun getForModalities(modalities: Set<Modality>): List<Exercise>
}

interface PreferencesRepository {
    fun observe(): Flow<UserPreferences>

    /**
     * Applies [transform] to the current preferences atomically. Callers pass a
     * copy-lambda rather than a whole object so concurrent writes from different
     * settings rows cannot clobber each other.
     */
    suspend fun update(transform: (UserPreferences) -> UserPreferences)
}

interface HistoryRepository {
    fun observeRecent(limit: Int): Flow<List<CompletedSession>>

    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<CompletedSession>>

    fun observeWeeklyLoad(weeks: Int): Flow<List<TrainingLoadSummary>>

    suspend fun record(session: CompletedSession): Long

    suspend fun delete(sessionId: Long)
}

interface MeasurementRepository {
    fun observeAll(): Flow<List<BodyMeasurement>>

    suspend fun upsert(measurement: BodyMeasurement): Long

    suspend fun delete(measurementId: Long)
}

interface SavedWorkoutRepository {
    fun observeFavourites(): Flow<List<Workout>>

    suspend fun save(workout: Workout, isFavourite: Boolean)

    suspend fun getById(workoutId: String): Workout?
}

/**
 * Pluggable body-mass source (PRD "Weight import priority"). Only
 * [ManualEntryProvider] ships in v1; Health Connect and any future vendor API
 * arrive as additional implementations without touching call sites.
 * See /framework/05_architecture.md §Provider abstraction.
 */
interface BodyMassProvider {
    val providerId: String

    /** False when the integration is absent, unauthorised or disabled. */
    suspend fun isAvailable(): Boolean

    /** Most recent known body mass in kilograms, or null when unknown. */
    suspend fun latestBodyMassKg(): Double?
}

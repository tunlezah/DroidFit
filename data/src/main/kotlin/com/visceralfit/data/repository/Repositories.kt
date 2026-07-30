package com.visceralfit.data.repository

import com.visceralfit.core.database.dao.ExerciseDao
import com.visceralfit.core.database.dao.MeasurementDao
import com.visceralfit.core.database.dao.SessionDao
import com.visceralfit.core.datastore.PreferencesDataSource
import com.visceralfit.data.mapper.toDomainOrNull
import com.visceralfit.data.mapper.toEntity
import com.visceralfit.domain.model.BodyMeasurement
import com.visceralfit.domain.model.CompletedSession
import com.visceralfit.domain.model.Exercise
import com.visceralfit.domain.model.MeasurementKind
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.TrainingLoadSummary
import com.visceralfit.domain.model.UserPreferences
import com.visceralfit.domain.repository.BodyMassProvider
import com.visceralfit.domain.repository.ExerciseRepository
import com.visceralfit.domain.repository.HistoryRepository
import com.visceralfit.domain.repository.MeasurementRepository
import com.visceralfit.domain.repository.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultExerciseRepository @Inject constructor(
    private val dao: ExerciseDao,
) : ExerciseRepository {

    override fun observeAll(): Flow<List<Exercise>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override suspend fun getById(id: String): Exercise? = dao.getById(id)?.toDomainOrNull()

    override suspend fun getForModalities(modalities: Set<Modality>): List<Exercise> =
        dao.getForModalities(modalities.map { it.id }).mapNotNull { it.toDomainOrNull() }
}

@Singleton
class DefaultPreferencesRepository @Inject constructor(
    private val dataSource: PreferencesDataSource,
) : PreferencesRepository {

    override fun observe(): Flow<UserPreferences> = dataSource.observe()

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) =
        dataSource.update(transform)
}

@Singleton
class DefaultHistoryRepository @Inject constructor(
    private val dao: SessionDao,
) : HistoryRepository {

    override fun observeRecent(limit: Int): Flow<List<CompletedSession>> =
        dao.observeRecent(limit).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<CompletedSession>> {
        // Half-open range on local midnights: a session started at 23:59 belongs to
        // the day the user started it, not to UTC's idea of that day.
        val zone = TimeZone.currentSystemDefault()
        val fromMs = from.atStartOfDayIn(zone).toEpochMilliseconds()
        val toMs = to.atStartOfDayIn(zone).toEpochMilliseconds()
        return dao.observeBetween(fromMs, toMs).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }
    }

    /**
     * NOT YET IMPLEMENTED — phase 09. Emits an empty list rather than throwing so
     * the skeleton runs; the Progress screen renders its documented empty state.
     * Implement per /framework/07_workout_engine_spec.md §Recovery recommender and
     * delete this comment. The exit criteria for phase 09 include a test that
     * fails while this returns a constant.
     */
    override fun observeWeeklyLoad(weeks: Int): Flow<List<TrainingLoadSummary>> = flowOf(emptyList())

    override suspend fun record(session: CompletedSession): Long = dao.insert(session.toEntity())

    override suspend fun delete(sessionId: Long) = dao.delete(sessionId)
}

@Singleton
class DefaultMeasurementRepository @Inject constructor(
    private val dao: MeasurementDao,
) : MeasurementRepository {

    override fun observeAll(): Flow<List<BodyMeasurement>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override suspend fun upsert(measurement: BodyMeasurement): Long = dao.upsert(measurement.toEntity())

    override suspend fun delete(measurementId: Long) = dao.delete(measurementId)
}

/**
 * The only body-mass source that ships in v1.
 *
 * Health Connect and any future scale API arrive as additional
 * [BodyMassProvider] implementations bound `@IntoSet`, and a resolver picks the
 * highest-priority available one. Nothing in the app asks "is Health Connect
 * installed?" — it asks the provider set. See /framework/05_architecture.md
 * §Provider abstraction.
 */
@Singleton
class ManualEntryBodyMassProvider @Inject constructor(
    private val measurementDao: MeasurementDao,
) : BodyMassProvider {

    override val providerId: String = "manual_entry"

    override suspend fun isAvailable(): Boolean = true

    override suspend fun latestBodyMassKg(): Double? =
        measurementDao.latestOfKind(MeasurementKind.BODY_MASS.id)?.value
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun exerciseRepository(impl: DefaultExerciseRepository): ExerciseRepository

    @Binds
    abstract fun preferencesRepository(impl: DefaultPreferencesRepository): PreferencesRepository

    @Binds
    abstract fun historyRepository(impl: DefaultHistoryRepository): HistoryRepository

    @Binds
    abstract fun measurementRepository(impl: DefaultMeasurementRepository): MeasurementRepository

    @Binds
    @IntoSet
    abstract fun manualBodyMassProvider(impl: ManualEntryBodyMassProvider): BodyMassProvider
}

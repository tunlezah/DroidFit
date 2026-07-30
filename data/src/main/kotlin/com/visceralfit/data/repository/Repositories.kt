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
import com.visceralfit.domain.model.WorkoutStyle
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
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

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
     * Training load for the last [weeks] ISO weeks, most recent first.
     *
     * Weeks start on Monday and are bounded by *local* midnights, for the same reason
     * [observeBetween] is: a session started at 23:59 on Sunday belongs to the week the
     * user trained in, not to UTC's idea of it.
     *
     * WHAT COUNTS TOWARD WHAT (D-0035): `totalMinutes` sums the active minutes of **every**
     * session, including ones abandoned early. `sessionCount` counts only sessions past
     * [CompletedSession.COMPLETION_THRESHOLD]. The split is deliberate — twenty honest
     * minutes are twenty minutes toward the WHO 150 whether or not forty were planned, but
     * a session abandoned at 20% is not a session for the purposes of consistency.
     *
     * KNOWN LIMITATION: the week boundaries are computed when the flow is collected, so a
     * screen left open across midnight on a Sunday shows the previous week until it is
     * re-collected. Recorded as KI-0019 rather than papered over with a ticking clock.
     */
    override fun observeWeeklyLoad(weeks: Int): Flow<List<TrainingLoadSummary>> {
        require(weeks > 0) { "weeks must be positive, was $weeks" }
        val zone = TimeZone.currentSystemDefault()
        val currentWeekStart = Clock.System.todayIn(zone).startOfIsoWeek()
        val earliestWeekStart = currentWeekStart.minus(weeks - 1, DateTimeUnit.WEEK)
        val fromMs = earliestWeekStart.atStartOfDayIn(zone).toEpochMilliseconds()
        val toMs = currentWeekStart.plus(1, DateTimeUnit.WEEK).atStartOfDayIn(zone).toEpochMilliseconds()

        return dao.observeBetween(fromMs, toMs).map { rows ->
            val sessions = rows.mapNotNull { it.toDomainOrNull() }
            List(weeks) { index -> currentWeekStart.minus(index, DateTimeUnit.WEEK) }
                .map { weekStart -> summarise(weekStart, sessions, zone) }
        }
    }

    private fun summarise(
        weekStart: LocalDate,
        sessions: List<CompletedSession>,
        zone: TimeZone,
    ): TrainingLoadSummary {
        val weekEnd = weekStart.plus(1, DateTimeUnit.WEEK)
        val inWeek = sessions.filter { session ->
            val day = session.startedAt.toLocalDateTime(zone).date
            day >= weekStart && day < weekEnd
        }
        return TrainingLoadSummary(
            weekStart = weekStart,
            totalMinutes = inWeek.sumOf { it.activeDuration.inWholeMinutes }.toInt(),
            vigorousMinutes = inWeek.filter { it.style.countsAsVigorous }
                .sumOf { it.activeDuration.inWholeMinutes }
                .toInt(),
            sessionCount = inWeek.count { it.wasCompleted },
            consecutiveVigorousDays = consecutiveVigorousDays(inWeek, zone),
        )
    }

    /**
     * The longest run of consecutive calendar days ending at the most recent vigorous day.
     *
     * The recovery recommender treats two or more as a reason to suggest a rest day
     * (`framework/07_workout_engine_spec.md` §8), so the run has to end at the *latest*
     * vigorous session — a pair of hard days a fortnight ago is not a reason to rest today.
     */
    private fun consecutiveVigorousDays(sessions: List<CompletedSession>, zone: TimeZone): Int {
        val vigorousDays = sessions
            .filter { it.style.countsAsVigorous }
            .map { it.startedAt.toLocalDateTime(zone).date }
            .toSortedSet()
        if (vigorousDays.isEmpty()) return 0
        var run = 1
        var previous = vigorousDays.last()
        vigorousDays.toList().dropLast(1).asReversed().forEach { day ->
            if (day == previous.minus(1, DateTimeUnit.DAY)) {
                run++
                previous = day
            } else {
                return run
            }
        }
        return run
    }

    override suspend fun record(session: CompletedSession): Long = dao.insert(session.toEntity())

    override suspend fun delete(sessionId: Long) = dao.delete(sessionId)

    private companion object {
        /**
         * Which styles contribute vigorous minutes.
         *
         * Approximated from the session's style because `CompletedSession` records the
         * style but not per-segment intensity — the same gap as KI-0012. Interval sessions
         * obviously count. Mixed sessions count too, even though their surges are threshold
         * rather than vigorous: the recovery recommender uses this figure to *warn*, and
         * over-warning is the safe direction. Steady and recovery sessions contribute none,
         * which is what makes a Pilates session honest (engine spec §7.3) — those are
         * recorded as RECOVERY by the generator.
         */
        val VIGOROUS_STYLES = setOf(WorkoutStyle.HIIT, WorkoutStyle.MIXED)

        val WorkoutStyle.countsAsVigorous: Boolean get() = this in VIGOROUS_STYLES
    }
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

/**
 * The Monday of the week containing this date.
 *
 * ISO weeks rather than Sunday-start, because the WHO's weekly activity guidance and every
 * European convention the operator is in agree on Monday. Adding a preference for it would
 * be a setting nobody asked for.
 */
private fun LocalDate.startOfIsoWeek(): LocalDate =
    minus(dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber, DateTimeUnit.DAY)

private val DayOfWeek.isoDayNumber: Int get() = ordinal + 1

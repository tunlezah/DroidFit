package com.visceralfit.data.repository

import com.visceralfit.core.database.dao.SessionDao
import com.visceralfit.core.database.entity.SessionEntity
import com.visceralfit.domain.model.WorkoutStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Clock

/**
 * The weekly training-load summary (KI-0002, TD-0007).
 *
 * The bug this replaces was not a crash: `observeWeeklyLoad` returned a constant empty
 * list and the Progress screen showed `0 of 150 minutes` forever, which looks exactly like
 * a user who has not trained. A wrong number is worse than an absent one, so the first
 * assertion here is simply that the function is not a constant.
 *
 * Dates are relative to today rather than fixed, because the summary is defined in terms of
 * the current ISO week. A test pinned to a hard-coded date would pass or fail depending on
 * which day of the week CI happened to run.
 */
class WeeklyLoadTest {

    private val zone = TimeZone.currentSystemDefault()
    private val today = Clock.System.todayIn(zone)

    @Test
    fun `an empty history reports a week of zeroes, not an empty list`() = runTest {
        val summaries = repository(emptyList()).observeWeeklyLoad(WEEKS).first()
        // One entry per requested week, so the UI never has to distinguish "no data" from
        // "no training" — it reads a zero either way, which is the honest number.
        assertEquals(WEEKS, summaries.size)
        assertTrue(summaries.all { it.totalMinutes == 0 && it.sessionCount == 0 })
    }

    @Test
    fun `this week's minutes are the sum of its sessions' active time`() = runTest {
        val summaries = repository(
            listOf(
                session(daysAgo = 0, activeSeconds = 1_200, style = WorkoutStyle.ZONE_2),
                session(daysAgo = 0, activeSeconds = 600, style = WorkoutStyle.RECOVERY),
            ),
        ).observeWeeklyLoad(WEEKS).first()

        val thisWeek = summaries.first()
        assertEquals(30, thisWeek.totalMinutes)
        assertEquals(2, thisWeek.sessionCount)
        // Neither style is vigorous, so a steady week reports no vigorous minutes.
        assertEquals(0, thisWeek.vigorousMinutes)
        assertNotEquals("the summary is still a constant", 0, thisWeek.totalMinutes)
    }

    /**
     * D-0035: an abandoned session's minutes still count toward weekly volume, because they
     * were still performed — but it is not a *session* for the purposes of consistency.
     */
    @Test
    fun `an abandoned session counts its minutes but not as a completed session`() = runTest {
        val summaries = repository(
            listOf(session(daysAgo = 0, activeSeconds = 600, completionRatio = 0.3f)),
        ).observeWeeklyLoad(WEEKS).first()

        assertEquals(10, summaries.first().totalMinutes)
        assertEquals(0, summaries.first().sessionCount)
    }

    @Test
    fun `interval sessions contribute vigorous minutes and steady ones do not`() = runTest {
        val summaries = repository(
            listOf(
                session(daysAgo = 0, activeSeconds = 1_200, style = WorkoutStyle.HIIT),
                session(daysAgo = 0, activeSeconds = 1_200, style = WorkoutStyle.ZONE_2),
            ),
        ).observeWeeklyLoad(WEEKS).first()

        assertEquals(40, summaries.first().totalMinutes)
        assertEquals(20, summaries.first().vigorousMinutes)
    }

    /**
     * Engine spec §8's first rest-day signal. Only counted when the run reaches *today* or
     * the most recent hard day — two hard days a fortnight ago is not a reason to rest now.
     */
    @Test
    fun `consecutive vigorous days are counted`() = runTest {
        val summaries = repository(
            listOf(
                session(daysAgo = 0, activeSeconds = 1_200, style = WorkoutStyle.HIIT),
                session(daysAgo = 1, activeSeconds = 1_200, style = WorkoutStyle.HIIT),
            ),
        ).observeWeeklyLoad(WEEKS).first()

        // Both days must fall inside the current ISO week for this to be two; early in the
        // week one of them is in the previous week, and then the run is one.
        val expected = if (today.dayOfWeek.ordinal >= 1) 2 else 1
        assertEquals(expected, summaries.first().consecutiveVigorousDays)
    }

    @Test
    fun `weeks are returned most recent first`() = runTest {
        val summaries = repository(emptyList()).observeWeeklyLoad(WEEKS).first()
        val starts = summaries.map { it.weekStart }
        assertEquals(starts.sortedDescending(), starts)
        assertEquals(WEEKS, starts.distinct().size)
    }

    @Test
    fun `asking for no weeks is a programming error, not an empty result`() {
        val failure = runCatching { repository(emptyList()).observeWeeklyLoad(0) }.exceptionOrNull()
        assertTrue("expected a rejection, got $failure", failure is IllegalArgumentException)
    }

    private fun repository(sessions: List<SessionEntity>) = DefaultHistoryRepository(FakeSessionDao(sessions))

    private fun session(
        daysAgo: Int,
        activeSeconds: Long,
        style: WorkoutStyle = WorkoutStyle.MIXED,
        completionRatio: Float = 1.0f,
    ): SessionEntity {
        // Midday, so a timezone offset cannot push the session into an adjacent day.
        val startedAt = today.minus(daysAgo, DateTimeUnit.DAY)
            .atStartOfDayIn(zone)
            .toEpochMilliseconds() + MIDDAY_MILLIS
        return SessionEntity(
            workoutId = "w_test",
            title = "Test session",
            style = style.id,
            modalities = listOf("floor_pilates"),
            startedAtEpochMs = startedAt,
            completedAtEpochMs = startedAt + activeSeconds * 1_000,
            activeSeconds = activeSeconds,
            estimatedKcal = null,
            perceivedExertion = null,
            completionRatio = completionRatio,
            note = null,
        )
    }

    /** Only the two queries the repository actually uses; everything else is unreachable. */
    private class FakeSessionDao(sessions: List<SessionEntity>) : SessionDao {
        private val rows = MutableStateFlow(sessions)

        override fun observeRecent(limit: Int): Flow<List<SessionEntity>> =
            rows.map { it.sortedByDescending(SessionEntity::startedAtEpochMs).take(limit) }

        override fun observeBetween(fromEpochMs: Long, toEpochMs: Long): Flow<List<SessionEntity>> =
            rows.map { all -> all.filter { it.startedAtEpochMs in fromEpochMs until toEpochMs } }

        override fun observeActiveSecondsBetween(
            fromEpochMs: Long,
            toEpochMs: Long,
            minimumCompletion: Float,
        ): Flow<Long> = rows.map { all ->
            all.filter { it.startedAtEpochMs in fromEpochMs until toEpochMs }
                .filter { it.completionRatio >= minimumCompletion }
                .sumOf { it.activeSeconds }
        }

        override suspend fun insert(session: SessionEntity): Long {
            rows.value = rows.value + session
            return rows.value.size.toLong()
        }

        override suspend fun delete(id: Long) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }

    private companion object {
        const val WEEKS = 4
        const val MIDDAY_MILLIS = 12L * 60 * 60 * 1_000
    }
}

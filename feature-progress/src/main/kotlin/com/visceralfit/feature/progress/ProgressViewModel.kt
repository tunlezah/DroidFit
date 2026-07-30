package com.visceralfit.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visceralfit.domain.model.MeasurementKind
import com.visceralfit.domain.repository.HistoryRepository
import com.visceralfit.domain.repository.MeasurementRepository
import com.visceralfit.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    measurementRepository: MeasurementRepository,
    historyRepository: HistoryRepository,
) : ViewModel() {

    val state: StateFlow<ProgressUiState> = combine(
        preferencesRepository.observe(),
        measurementRepository.observeAll(),
        historyRepository.observeWeeklyLoad(WEEKS_OF_HISTORY),
    ) { prefs, measurements, weeklyLoad ->
        val waist = measurements.filter { it.kind == MeasurementKind.WAIST_CIRCUMFERENCE }
        // Most recent first, so the head of the list is the week in progress.
        val thisWeek = weeklyLoad.firstOrNull()
        ProgressUiState(
            minutesThisWeek = thisWeek?.totalMinutes ?: 0,
            vigorousMinutesThisWeek = thisWeek?.vigorousMinutes ?: 0,
            sessionsThisWeek = thisWeek?.sessionCount ?: 0,
            weeklyGoalMinutes = prefs.weeklyMinutesGoal,
            waistTrendSummary = waist.summarise(),
            restDaySuggestion = restDaySuggestion(weeklyLoad),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ProgressUiState(),
    )

    private fun List<com.visceralfit.domain.model.BodyMeasurement>.summarise(): String = when {
        isEmpty() -> "No waist measurements recorded yet."
        size == 1 -> "One measurement recorded. Add another in a few weeks to see a trend."
        else -> {
            val newest = first()
            val oldest = last()
            val delta = newest.value - oldest.value
            val direction = when {
                delta <= -MEANINGFUL_CHANGE_CM -> "down"
                delta >= MEANINGFUL_CHANGE_CM -> "up"
                else -> "roughly unchanged"
            }
            if (direction == "roughly unchanged") {
                "Roughly unchanged across $size measurements."
            } else {
                "%s %.1f %s across %d measurements.".format(
                    direction.replaceFirstChar { it.uppercase() },
                    kotlin.math.abs(delta),
                    newest.unit,
                    size,
                )
            }
        }
    }

    /**
     * The recovery recommender's two signals that can be computed from what history records
     * today (`framework/07_workout_engine_spec.md` §8).
     *
     * The other two conditions in §8 need per-session data the schema does not carry yet, so
     * they are deliberately absent rather than approximated: guessing at a rest-day
     * recommendation is worse than not making one. Returns null when there is nothing to
     * say — an advice card that always shows something stops being read.
     */
    private fun restDaySuggestion(weeks: List<com.visceralfit.domain.model.TrainingLoadSummary>): String? {
        val thisWeek = weeks.firstOrNull() ?: return null
        val priorWeeks = weeks.drop(1).filter { it.totalMinutes > 0 }
        val trailingAverage = if (priorWeeks.isEmpty()) 0 else priorWeeks.sumOf { it.totalMinutes } / priorWeeks.size

        return when {
            thisWeek.consecutiveVigorousDays >= CONSECUTIVE_VIGOROUS_LIMIT ->
                "That is ${thisWeek.consecutiveVigorousDays} hard days in a row. A recovery " +
                    "session or a rest day would let the adaptation happen."

            trailingAverage > 0 && thisWeek.totalMinutes > trailingAverage * VOLUME_JUMP_FACTOR ->
                "This week is well above your recent average of $trailingAverage minutes. " +
                    "Increases of more than about a tenth a week tend not to stick."

            else -> null
        }
    }

    private companion object {
        /**
         * Below 1 cm, a change is within the measurement error of a tape held by the
         * user themselves, so the app declines to call it a trend.
         */
        const val MEANINGFUL_CHANGE_CM = 1.0
        const val STOP_TIMEOUT_MS = 5_000L

        /** Four weeks is what the recovery recommender's trailing average is defined over. */
        const val WEEKS_OF_HISTORY = 4

        /** Engine spec §8: two consecutive vigorous days is the threshold to suggest recovery. */
        const val CONSECUTIVE_VIGOROUS_LIMIT = 2

        /** Engine spec §8: more than 30% above the trailing average is worth flagging. */
        const val VOLUME_JUMP_FACTOR = 1.3
    }
}

data class ProgressUiState(
    val minutesThisWeek: Int = 0,
    val vigorousMinutesThisWeek: Int = 0,
    val sessionsThisWeek: Int = 0,
    val weeklyGoalMinutes: Int = 150,
    val waistTrendSummary: String = "No waist measurements recorded yet.",
    /** Null when there is nothing worth saying. Advice that always appears is not read. */
    val restDaySuggestion: String? = null,
) {
    val goalProgress: Float
        get() = if (weeklyGoalMinutes <= 0) 0f else (minutesThisWeek.toFloat() / weeklyGoalMinutes).coerceIn(0f, 1f)

    /** Session and vigorous-minute counts in one line. Zero sessions says so in words. */
    fun weekSummaryLine(): String = when (sessionsThisWeek) {
        0 -> "No completed sessions yet this week."
        1 -> "1 session · $vigorousMinutesThisWeek vigorous minutes"
        else -> "$sessionsThisWeek sessions · $vigorousMinutesThisWeek vigorous minutes"
    }
}

package com.visceralfit.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visceralfit.domain.model.MeasurementKind
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
) : ViewModel() {

    val state: StateFlow<ProgressUiState> = combine(
        preferencesRepository.observe(),
        measurementRepository.observeAll(),
    ) { prefs, measurements ->
        val waist = measurements.filter { it.kind == MeasurementKind.WAIST_CIRCUMFERENCE }
        ProgressUiState(
            // Weekly minutes land in phase 09 with the training-load summary; the
            // card renders a real zero rather than a fabricated number.
            minutesThisWeek = 0,
            weeklyGoalMinutes = prefs.weeklyMinutesGoal,
            waistTrendSummary = waist.summarise(),
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

    private companion object {
        /**
         * Below 1 cm, a change is within the measurement error of a tape held by the
         * user themselves, so the app declines to call it a trend.
         */
        const val MEANINGFUL_CHANGE_CM = 1.0
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

data class ProgressUiState(
    val minutesThisWeek: Int = 0,
    val weeklyGoalMinutes: Int = 150,
    val waistTrendSummary: String = "No waist measurements recorded yet.",
) {
    val goalProgress: Float
        get() = if (weeklyGoalMinutes <= 0) 0f else (minutesThisWeek.toFloat() / weeklyGoalMinutes).coerceIn(0f, 1f)
}

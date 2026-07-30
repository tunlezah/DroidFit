package com.visceralfit.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.WorkoutStyle
import com.visceralfit.domain.repository.ExerciseRepository
import com.visceralfit.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class WorkoutHomeViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    /** Selections the user has made on this screen but not yet saved as defaults. */
    private val selection = MutableStateFlow(Selection())

    val state: StateFlow<WorkoutHomeUiState> = combine(
        preferencesRepository.observe(),
        exerciseRepository.observeAll(),
        selection,
    ) { prefs, exercises, chosen ->
        WorkoutHomeUiState(
            selectedMinutes = chosen.minutes ?: prefs.defaultDuration.inWholeMinutes.toInt(),
            selectedStyle = chosen.style ?: prefs.defaultStyle,
            usableModalities = prefs.usableModalities(),
            exerciseCount = exercises.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = WorkoutHomeUiState(),
    )

    fun selectDuration(minutes: Int) = selection.update { it.copy(minutes = minutes) }

    fun selectStyle(style: WorkoutStyle) = selection.update { it.copy(style = style) }

    private data class Selection(
        val minutes: Int? = null,
        val style: WorkoutStyle? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

data class WorkoutHomeUiState(
    val selectedMinutes: Int = DEFAULT_MINUTES,
    val selectedStyle: WorkoutStyle = WorkoutStyle.MIXED,
    val usableModalities: Set<Modality> = emptySet(),
    val exerciseCount: Int = 0,
) {
    companion object {
        /** Presets from PRD REQ-012. Custom duration is a separate entry point. */
        val DURATION_PRESETS_MINUTES = listOf(5, 10, 15, 20, 30, 45, 60, 90)
        const val DEFAULT_MINUTES = 20
    }
}

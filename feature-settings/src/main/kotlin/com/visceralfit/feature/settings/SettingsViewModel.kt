package com.visceralfit.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.UserPreferences
import com.visceralfit.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = preferencesRepository.observe()
        .map<UserPreferences, SettingsUiState> { SettingsUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SettingsUiState.Loading,
        )

    /**
     * Toggling a modality off is refused when it is the last one enabled: an empty
     * selection would make workout generation impossible, and a settings screen that
     * can put the app into a dead state is a defect, not a user choice (REQ-011).
     */
    fun setModalityEnabled(modality: Modality, enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.update { prefs ->
            val updated = if (enabled) {
                prefs.enabledModalities + modality
            } else {
                (prefs.enabledModalities - modality).ifEmpty { prefs.enabledModalities }
            }
            prefs.copy(enabledModalities = updated)
        }
    }

    fun setEquipmentAvailable(modality: Modality, available: Boolean) = viewModelScope.launch {
        preferencesRepository.update { prefs ->
            val equipment = if (available) {
                prefs.body.availableEquipment + modality
            } else {
                prefs.body.availableEquipment - modality
            }
            prefs.copy(body = prefs.body.copy(availableEquipment = equipment))
        }
    }

    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.update { it.copy(display = it.display.copy(keepScreenOn = enabled)) }
    }

    fun setAmoledDarkMode(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.update { it.copy(display = it.display.copy(amoledDarkMode = enabled)) }
    }

    fun setSpeechEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.update { it.copy(coaching = it.coaching.copy(speechEnabled = enabled)) }
    }

    fun setAnnounceNextExercise(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.update { it.copy(coaching = it.coaching.copy(announceNextExercise = enabled)) }
    }

    fun setSpeakFullInstructions(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.update { it.copy(coaching = it.coaching.copy(speakFullInstructions = enabled)) }
    }

    fun setAnnounceHalfway(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.update { it.copy(coaching = it.coaching.copy(announceHalfway = enabled)) }
    }

    fun setAnnounceCountdown(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.update { it.copy(coaching = it.coaching.copy(announceCountdown = enabled)) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Ready(val preferences: UserPreferences) : SettingsUiState
}

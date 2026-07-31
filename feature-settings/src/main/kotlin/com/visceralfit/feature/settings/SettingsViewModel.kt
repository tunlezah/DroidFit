package com.visceralfit.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visceralfit.domain.model.EffortCeiling
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.UserPreferences
import com.visceralfit.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    /**
     * A refusal the user needs told about, cleared on the next successful change.
     *
     * REQ-011 requires the UI to *explain* why the last enabled modality cannot be turned
     * off. Before this the toggle simply sprang back, which reads as a broken switch.
     */
    private val refusal = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = preferencesRepository.observe()
        .combine(refusal) { prefs, message -> SettingsUiState.Ready(prefs, message) }
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
                prefs.enabledModalities - modality
            }
            if (updated.isEmpty()) {
                refusal.value = "Keep at least one exercise type on — otherwise there is " +
                    "nothing to build a session from."
                return@update prefs
            }
            refusal.value = null
            prefs.copy(enabledModalities = updated)
        }
    }

    /**
     * Changes the hardest effort any session may prescribe (A-0007, D-0040).
     *
     * Deliberately not derived from experience level: technique and cardiovascular clearance
     * are different questions, and only the user knows the second one.
     */
    fun setEffortCeiling(ceiling: EffortCeiling) = viewModelScope.launch {
        refusal.value = null
        preferencesRepository.update { it.copy(effortCeiling = ceiling) }
    }

    fun dismissRefusal() {
        refusal.value = null
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
    data class Ready(
        val preferences: UserPreferences,
        /** A change the app declined, and why. Null when there is nothing to say. */
        val refusal: String? = null,
    ) : SettingsUiState
}

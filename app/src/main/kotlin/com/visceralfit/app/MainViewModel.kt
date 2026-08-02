package com.visceralfit.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visceralfit.domain.model.ThemePreference
import com.visceralfit.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns only what the app shell needs: the theme, and the two onboarding flags that gate it.
 * Deliberately narrow — putting the whole preferences object here would recompose the entire
 * app whenever an unrelated setting like the speech rate changed.
 *
 * Both flags earn their place because each gates the whole shell and each changes exactly once
 * in the life of an install, so together they cost two recompositions ever.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val state: StateFlow<MainUiState> = preferencesRepository.observe()
        .map { prefs ->
            MainUiState.Ready(
                theme = prefs.display.theme,
                amoled = prefs.display.amoledDarkMode,
                dynamicColour = prefs.display.dynamicColour,
                safetyNoticeAcknowledged = prefs.safetyNoticeAcknowledged,
                notificationPermissionRequested = prefs.notificationPermissionRequested,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = MainUiState.Loading,
        )

    /**
     * Records that the user has read the safety notice (REQ-005).
     *
     * Persisted rather than held in memory: a notice that reappears on every launch stops
     * being read, and one that is forgotten on process death was never acknowledged.
     */
    fun acknowledgeSafetyNotice() {
        viewModelScope.launch {
            preferencesRepository.update { it.copy(safetyNoticeAcknowledged = true) }
        }
    }

    /**
     * Records that the notification permission has been asked for, whatever the answer was.
     *
     * Persisted for the same reason the safety acknowledgement is: a question that reappears on
     * every launch stops being a question and becomes nagging. It records only *that* we asked,
     * never the answer — whether the permission is held is a live platform question and
     * caching it would go stale the moment the user changed it in Android settings.
     */
    fun markNotificationPermissionRequested() {
        viewModelScope.launch {
            preferencesRepository.update { it.copy(notificationPermissionRequested = true) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(
        val theme: ThemePreference,
        val amoled: Boolean,
        val dynamicColour: Boolean,
        val safetyNoticeAcknowledged: Boolean,
        val notificationPermissionRequested: Boolean,
    ) : MainUiState
}

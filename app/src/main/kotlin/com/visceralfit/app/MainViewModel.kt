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
import javax.inject.Inject

/**
 * Owns only what the theme needs. Deliberately narrow: putting the whole
 * preferences object here would recompose the entire app whenever an unrelated
 * setting like the speech rate changed.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val state: StateFlow<MainUiState> = preferencesRepository.observe()
        .map { prefs ->
            MainUiState.Ready(
                theme = prefs.display.theme,
                amoled = prefs.display.amoledDarkMode,
                dynamicColour = prefs.display.dynamicColour,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = MainUiState.Loading,
        )

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
    ) : MainUiState
}

package com.visceralfit.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visceralfit.domain.model.CompletedSession
import com.visceralfit.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    historyRepository: HistoryRepository,
) : ViewModel() {

    val state: StateFlow<HistoryUiState> = historyRepository.observeRecent(RECENT_LIMIT)
        .map { HistoryUiState(sessions = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HistoryUiState(),
        )

    private companion object {
        /**
         * Phase 08 replaces this with paging. Until then the cap is explicit rather
         * than unbounded, so a user with two years of history does not load every row
         * into memory to render one screen.
         */
        const val RECENT_LIMIT = 100
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

data class HistoryUiState(
    val sessions: List<CompletedSession> = emptyList(),
)

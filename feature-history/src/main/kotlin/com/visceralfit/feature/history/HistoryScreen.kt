package com.visceralfit.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visceralfit.core.common.DurationFormat
import com.visceralfit.core.designsystem.theme.VisceralFitTheme
import com.visceralfit.domain.model.CompletedSession

@Composable
fun HistoryRoute(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(state)
}

@Composable
internal fun HistoryScreen(state: HistoryUiState) {
    if (state.sessions.isEmpty()) {
        EmptyHistory()
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.sessions, key = { it.id }) { session ->
            SessionRow(session)
            HorizontalDivider()
        }
    }
}

@Composable
private fun SessionRow(session: CompletedSession) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(session.title, style = MaterialTheme.typography.bodyLarge)
        Text(
            buildString {
                append(DurationFormat.compact(session.activeDuration))
                // A null energy estimate renders as an em dash, never as 0 kcal.
                // Showing 0 would read as "you burned nothing", which is false.
                append(" · ")
                append(session.estimatedKilocalories?.let { "~$it kcal" } ?: "— kcal")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "No sessions yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            "Finished workouts appear here, on this device only.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyHistoryPreview() {
    VisceralFitTheme { HistoryScreen(HistoryUiState()) }
}

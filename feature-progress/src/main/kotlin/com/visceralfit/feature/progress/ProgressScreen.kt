package com.visceralfit.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visceralfit.core.designsystem.theme.VisceralFitTheme

@Composable
fun ProgressRoute(viewModel: ProgressViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProgressScreen(state)
}

@Composable
internal fun ProgressScreen(state: ProgressUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("This week", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${state.minutesThisWeek} of ${state.weeklyGoalMinutes} minutes",
                    style = MaterialTheme.typography.titleLarge,
                )
                LinearProgressIndicator(
                    progress = { state.goalProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "The World Health Organization recommends 150–300 minutes of moderate " +
                        "activity a week, or 75–150 vigorous.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Waist trend", style = MaterialTheme.typography.titleSmall)
                Text(
                    state.waistTrendSummary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                // This wording is not decoration: it is the guard against the app
                // implying it measures visceral fat. See PRD REQ-090 and
                // /framework/02_evidence_base.md §Claims we do not make.
                Text(
                    "Waist measurements track a trend. They are not a measure of visceral " +
                        "fat, and this app does not estimate it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressPreview() {
    VisceralFitTheme { ProgressScreen(ProgressUiState(minutesThisWeek = 95)) }
}

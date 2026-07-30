package com.visceralfit.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.WorkoutStyle

@Composable
fun WorkoutHomeRoute(viewModel: WorkoutHomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WorkoutHomeScreen(
        state = state,
        onDurationSelected = viewModel::selectDuration,
        onStyleSelected = viewModel::selectStyle,
    )
}

@Composable
internal fun WorkoutHomeScreen(
    state: WorkoutHomeUiState,
    onDurationSelected: (Int) -> Unit,
    onStyleSelected: (WorkoutStyle) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Today's session", style = MaterialTheme.typography.headlineSmall)

        Text("How long?", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkoutHomeUiState.DURATION_PRESETS_MINUTES.forEach { minutes ->
                FilterChip(
                    selected = state.selectedMinutes == minutes,
                    onClick = { onDurationSelected(minutes) },
                    label = { Text("$minutes min") },
                )
            }
        }

        Text("What kind?", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkoutStyle.entries.forEach { style ->
                FilterChip(
                    selected = state.selectedStyle == style,
                    onClick = { onStyleSelected(style) },
                    label = { Text(style.label) },
                )
            }
        }

        Text("Using", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.usableModalities.forEach { modality ->
                AssistChip(onClick = {}, label = { Text(modality.label) })
            }
        }
        if (state.usableModalities.isEmpty()) {
            Text(
                "No exercise types are usable yet. Enable at least one in Settings, and " +
                    "mark the equipment you have access to.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // The generator lands in phase 06. Until then the button states the reason
        // rather than silently doing nothing — a dead control with no explanation is
        // the single most common complaint in the competitor review sample
        // (/framework/03_competitive_analysis.md §Complaint themes).
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start — workout engine arrives in phase 06")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Exercise library", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${state.exerciseCount} movements loaded from the bundled catalogue.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Everything is stored on this device. The app has no internet permission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val WorkoutStyle.label: String
    get() = when (this) {
        WorkoutStyle.HIIT -> "Intervals"
        WorkoutStyle.ZONE_2 -> "Steady"
        WorkoutStyle.MIXED -> "Mixed"
        WorkoutStyle.RECOVERY -> "Recovery"
    }

private val Modality.label: String
    get() = when (this) {
        Modality.FLOOR_PILATES -> "Floor Pilates"
        Modality.REFORMER_PILATES -> "Reformer"
        Modality.ELLIPTICAL -> "Elliptical"
        Modality.SPIN_BIKE -> "Spin bike"
    }

@Preview(showBackground = true)
@Composable
private fun WorkoutHomePreview() {
    VisceralFitTheme {
        WorkoutHomeScreen(
            state = WorkoutHomeUiState(
                selectedMinutes = 20,
                selectedStyle = WorkoutStyle.MIXED,
                usableModalities = setOf(Modality.SPIN_BIKE, Modality.FLOOR_PILATES),
                exerciseCount = 14,
            ),
            onDurationSelected = {},
            onStyleSelected = {},
        )
    }
}

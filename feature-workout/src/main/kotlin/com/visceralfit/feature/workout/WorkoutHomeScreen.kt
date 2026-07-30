package com.visceralfit.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visceralfit.core.designsystem.theme.VisceralFitTheme
import com.visceralfit.domain.model.BlockKind
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.SegmentKind
import com.visceralfit.domain.model.Workout
import com.visceralfit.domain.model.WorkoutStyle
import com.visceralfit.feature.workout.player.WorkoutService

@Composable
fun WorkoutHomeRoute(
    onSessionStarted: () -> Unit,
    viewModel: WorkoutHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    WorkoutHomeScreen(
        state = state,
        onDurationSelected = viewModel::selectDuration,
        onStyleSelected = viewModel::selectStyle,
        onStart = viewModel::start,
        onDismissFailure = viewModel::dismissFailure,
        onDiscardPlan = viewModel::consumeGeneratedWorkout,
        onBeginSession = {
            // Starting the service is the caller's job because it needs a Context. The
            // coordinator is loaded first so the service finds a session to run.
            if (viewModel.beginSession()) {
                WorkoutService.start(context)
                onSessionStarted()
            }
        },
    )
}

@Composable
internal fun WorkoutHomeScreen(
    state: WorkoutHomeUiState,
    onDurationSelected: (Int) -> Unit,
    onStyleSelected: (WorkoutStyle) -> Unit,
    onStart: () -> Unit,
    onDismissFailure: () -> Unit,
    onDiscardPlan: () -> Unit,
    onBeginSession: () -> Unit,
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
        StyleChips(state = state, onStyleSelected = onStyleSelected)

        Text("Using", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.usableModalities.forEach { modality ->
                AssistChip(onClick = {}, label = { Text(modality.displayName) })
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

        StartControl(state = state, onStart = onStart)

        when (val generation = state.generation) {
            is GenerationUiState.Failed -> FailureCard(generation.message, onDismissFailure)
            is GenerationUiState.Ready -> PlanCard(generation.workout, onBeginSession, onDiscardPlan)
            GenerationUiState.Generating, GenerationUiState.Idle -> Unit
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

/**
 * A style the chosen duration cannot support is disabled *and* states its own minimum
 * (REQ-024). Letting the user pick it and then failing generation is the failure mode that
 * requirement exists to prevent.
 */
@Composable
private fun StyleChips(state: WorkoutHomeUiState, onStyleSelected: (WorkoutStyle) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WorkoutStyle.entries.forEach { style ->
            val available = style in state.availableStyles
            val minimum = state.minimumMinutesByStyle[style]
            FilterChip(
                selected = state.selectedStyle == style,
                enabled = available,
                onClick = { onStyleSelected(style) },
                label = {
                    Text(
                        when {
                            available -> style.displayName
                            minimum != null -> "${style.displayName} · ${minimum}min+"
                            else -> style.displayName
                        },
                    )
                },
                modifier = Modifier.semantics {
                    if (!available && minimum != null) {
                        contentDescription =
                            "${style.displayName}, unavailable: needs at least $minimum minutes"
                    }
                },
            )
        }
    }
}

@Composable
private fun StartControl(state: WorkoutHomeUiState, onStart: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onStart,
            enabled = state.canStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.generation is GenerationUiState.Generating) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .clearAndSetSemantics { contentDescription = "Building your session" },
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Start ${state.selectedMinutes}-minute ${state.selectedStyle.displayName.lowercase()}")
            }
        }
        // A disabled control always says why. See framework/10_screen_specs.md §3.
        state.startDisabledReason?.let { reason ->
            Text(
                reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FailureCard(message: GenerationMessage, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "That session could not be built",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                message.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                message.fix,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            OutlinedButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

/**
 * The generated plan, shown before the session starts so the user can see what they are
 * about to do and regenerate if they do not like it.
 *
 * [Workout.buildNotes] is rendered rather than hidden: when the engine had to substitute
 * or cap something, the user is told, in the same place they are told what the session is.
 */
@Composable
private fun PlanCard(workout: Workout, onBegin: () -> Unit, onDiscard: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(workout.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${workout.segments.size} segments · " +
                    "${workout.actualDuration.inWholeMinutes} minutes · " +
                    "${workout.workSegments.size} work efforts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            workout.buildNotes.forEach { note ->
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            workout.blocks.forEach { block ->
                Text(
                    "${block.kind.label} · ${block.duration.inWholeMinutes.coerceAtLeast(1)} min",
                    style = MaterialTheme.typography.titleSmall,
                )
                block.segments.forEach { segment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            formatSeconds(segment.duration.inWholeSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End,
                            modifier = Modifier.size(width = 48.dp, height = 20.dp),
                        )
                        Text(
                            segment.exercise?.name ?: segment.kind.label,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Button(onClick = onBegin, modifier = Modifier.fillMaxWidth()) { Text("Begin session") }
            OutlinedButton(onClick = onDiscard) { Text("Choose again") }
        }
    }
}

private fun formatSeconds(seconds: Long): String =
    "${seconds / SECONDS_PER_MINUTE}:${(seconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')}"

private const val SECONDS_PER_MINUTE = 60

private val BlockKind.label: String
    get() = when (this) {
        BlockKind.WARM_UP -> "Warm-up"
        BlockKind.MAIN -> "Main"
        BlockKind.COOL_DOWN -> "Cool-down"
    }

private val SegmentKind.label: String
    get() = when (this) {
        SegmentKind.WORK -> "Work"
        SegmentKind.ACTIVE_RECOVERY -> "Easy"
        SegmentKind.REST -> "Rest"
        SegmentKind.TRANSITION -> "Change over"
    }

@Preview(showBackground = true)
@Composable
private fun WorkoutHomePreview() {
    VisceralFitTheme {
        WorkoutHomeScreen(
            state = WorkoutHomeUiState(
                isLoading = false,
                selectedMinutes = 10,
                selectedStyle = WorkoutStyle.MIXED,
                usableModalities = setOf(Modality.SPIN_BIKE, Modality.FLOOR_PILATES),
                availableStyles = setOf(WorkoutStyle.ZONE_2, WorkoutStyle.MIXED, WorkoutStyle.RECOVERY),
                minimumMinutesByStyle = mapOf(
                    WorkoutStyle.HIIT to 16,
                    WorkoutStyle.ZONE_2 to 9,
                    WorkoutStyle.MIXED to 10,
                    WorkoutStyle.RECOVERY to 7,
                ),
                exerciseCount = 65,
            ),
            onDurationSelected = {},
            onStyleSelected = {},
            onStart = {},
            onDismissFailure = {},
            onDiscardPlan = {},
            onBeginSession = {},
        )
    }
}

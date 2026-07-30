package com.visceralfit.feature.workout.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visceralfit.core.designsystem.modifier.KeepScreenOn
import com.visceralfit.core.designsystem.theme.VisceralFitTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun WorkoutPlayerRoute(
    onSessionEnded: () -> Unit,
    viewModel: WorkoutPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The service is what keeps the clock alive, so stopping it is part of ending the
    // session rather than something the caller has to remember.
    val endSession: () -> Unit = {
        viewModel.finishAndRecord()
        WorkoutService.stop(context)
    }

    WorkoutPlayerScreen(
        state = state,
        onTogglePause = viewModel::togglePause,
        onSkip = viewModel::skip,
        onEndSession = endSession,
        onDismissSummary = {
            viewModel.dismissSummary()
            onSessionEnded()
        },
        onNoSession = onSessionEnded,
    )
}

@Composable
internal fun WorkoutPlayerScreen(
    state: PlayerUiState,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
    onEndSession: () -> Unit,
    onDismissSummary: () -> Unit,
    onNoSession: () -> Unit,
) {
    when (state) {
        PlayerUiState.NoSession -> NoSessionContent(onNoSession)
        is PlayerUiState.Finished -> SessionSummaryContent(state.summary, onDismissSummary)
        is PlayerUiState.Running -> RunningContent(
            state = state,
            onTogglePause = onTogglePause,
            onSkip = onSkip,
            onEndSession = onEndSession,
        )
    }
}

@Composable
private fun RunningContent(
    state: PlayerUiState.Running,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
    onEndSession: () -> Unit,
) {
    val session = state.session
    KeepScreenOn(enabled = state.keepScreenOn && !session.isPaused)

    // An accidental edge swipe must not silently end a 45-minute effort
    // (framework/10_screen_specs.md §Navigation).
    var confirmingExit by remember { mutableStateOf(false) }
    BackHandler { confirmingExit = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "${formatClock(session.remainingTotal)} remaining · " +
                "segment ${session.segmentNumber} of ${session.segmentCount}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { session.completionRatio },
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            formatClock(session.remainingInSegment),
            // Monospaced so the digits do not shift as the countdown runs — at arm's length
            // on a bike, a jumping clock is unreadable.
            fontFamily = FontFamily.Monospace,
            fontSize = if (state.machineMode) MACHINE_COUNTDOWN_SP.sp else COUNTDOWN_SP.sp,
            textAlign = TextAlign.Center,
        )

        Text(
            session.currentSegment?.label ?: "Finishing",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        session.currentSegment?.let { segment ->
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        "${segment.intensity.describe()} · RPE ${segment.intensity.rpeRange.first}" +
                            "–${segment.intensity.rpeRange.last}",
                    )
                },
            )
            segment.roundIndex?.let { round ->
                Text(
                    "Round $round of ${segment.roundTotal}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        if (session.isPaused) {
            Text(
                "Paused",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        // Machine mode drops the technique block: nobody reads how-to steps at 90 rpm
        // (framework/10_screen_specs.md §4).
        if (!state.machineMode) {
            session.currentSegment?.exercise?.let { exercise ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        exercise.howTo.forEach { step ->
                            Text("• $step", style = MaterialTheme.typography.bodyMedium)
                        }
                        exercise.safetyNotes.firstOrNull()?.let { note ->
                            Text(
                                note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        session.nextSegment?.let { next ->
            Text(
                "Next: ${next.label} · ${formatClock(next.duration)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onTogglePause, modifier = Modifier.weight(1f)) {
                Text(if (session.isPaused) "Resume" else "Pause")
            }
            OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                Text("Skip")
            }
        }
        TextButton(onClick = { confirmingExit = true }) { Text("End session") }
    }

    if (confirmingExit) {
        AlertDialog(
            onDismissRequest = { confirmingExit = false },
            title = { Text("End this session?") },
            text = {
                Text(
                    "It will be saved with what you have done so far — " +
                        "${formatClock(session.activeElapsed)} of " +
                        "${formatClock(session.workout.actualDuration)}.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingExit = false
                        onEndSession()
                    },
                ) { Text("End and save") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingExit = false }) { Text("Keep going") }
            },
        )
    }
}

@Composable
private fun SessionSummaryContent(summary: SessionSummary, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Session recorded", style = MaterialTheme.typography.headlineSmall)
        Text(summary.title, style = MaterialTheme.typography.titleMedium)

        SummaryRow("Planned", formatClock(summary.plannedDuration))
        SummaryRow("Time moving", formatClock(summary.activeDuration))
        // Never a zero. An absent estimate reads as absent (D-0005, REQ-081).
        SummaryRow(
            "Energy",
            summary.estimatedKilocalories?.let { "~$it kcal estimated" } ?: "— kcal",
        )
        SummaryRow("Segments", "${summary.segmentsCompleted} of ${summary.segmentCount}")
        if (summary.skippedSegments > 0) {
            SummaryRow("Skipped", "${summary.skippedSegments}")
        }

        // Stated neutrally. Recovery is part of training, and guilt framing is what makes
        // people stop opening the app (framework/10_screen_specs.md §5).
        if (!summary.ranToCompletion) {
            Text(
                "Ended early — ${formatClock(summary.activeDuration)} of " +
                    "${formatClock(summary.plannedDuration)}.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            "Body mass is needed for an energy estimate. Add it in Settings if you want one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NoSessionContent(onLeave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("No session is running", style = MaterialTheme.typography.titleMedium)
        Text(
            "Sessions are started from the Train screen.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onLeave) { Text("Back to Train") }
    }
}

private fun com.visceralfit.domain.model.IntensityTarget.describe(): String = when (this) {
    com.visceralfit.domain.model.IntensityTarget.RECOVERY -> "Easy"
    com.visceralfit.domain.model.IntensityTarget.ZONE_2 -> "Steady"
    com.visceralfit.domain.model.IntensityTarget.THRESHOLD -> "Threshold"
    com.visceralfit.domain.model.IntensityTarget.VIGOROUS -> "Vigorous"
    else -> "Effort"
}

private fun formatClock(duration: Duration): String {
    val total = duration.inWholeSeconds
    val minutes = total / SECONDS_PER_MINUTE
    val seconds = total % SECONDS_PER_MINUTE
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private const val SECONDS_PER_MINUTE = 60
private const val COUNTDOWN_SP = 96
private const val MACHINE_COUNTDOWN_SP = 148

@Preview(showBackground = true)
@Composable
private fun SummaryPreview() {
    VisceralFitTheme {
        WorkoutPlayerScreen(
            state = PlayerUiState.Finished(
                SessionSummary(
                    title = "Mixed — 20 min",
                    plannedDuration = 1_200.seconds,
                    activeDuration = 1_180.seconds,
                    estimatedKilocalories = 214,
                    segmentsCompleted = 13,
                    segmentCount = 13,
                    skippedSegments = 0,
                    completionRatio = 0.98f,
                ),
            ),
            onTogglePause = {},
            onSkip = {},
            onEndSession = {},
            onDismissSummary = {},
            onNoSession = {},
        )
    }
}

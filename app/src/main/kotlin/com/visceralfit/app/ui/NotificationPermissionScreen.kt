package com.visceralfit.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.visceralfit.core.designsystem.theme.VisceralFitTheme

/**
 * The one runtime permission this app has, asked as the second and final step of onboarding —
 * immediately after the safety notice, before the app proper (D-0046).
 *
 * ## Why this is a screen and not a bare system dialog
 *
 * The system dialog says "Allow VisceralFit to send you notifications?", which is a question
 * about a channel rather than about a feature, and the honest answer depends entirely on what
 * the notifications are *for*. This screen answers that first: it is one notification, it
 * exists for the whole length of a session and no longer, it never makes a sound, and what it
 * buys is the pause, skip and end controls without unlocking the phone.
 *
 * ## Why here rather than at the first session
 *
 * `framework/15_device_targets_motorola_edge_60.md` §Notification permission asks for it at the
 * first workout, "not at launch". The operator asked for onboarding instead, and D-0046 records
 * the departure and why the spec's concern does not apply here: the worry is a prompt that
 * arrives before the user knows what the app is, and by this point they have read the safety
 * notice and know exactly what the app does. Asking mid-flow, as the user is trying to start
 * training, has its own cost the spec does not weigh.
 *
 * ## What it must never do
 *
 * Block. Both buttons leave onboarding. There is no path where declining a notification stops
 * someone reaching the app, because the permission buys convenience and nothing else — the
 * clock lives in a foreground service that runs without it.
 */
@Composable
internal fun NotificationPermissionScreen(onDecided: () -> Unit) {
    // The result is deliberately ignored. Granted or refused, onboarding is over — branching
    // on it would be the first step towards a gate.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { onDecided() },
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("One notification", style = MaterialTheme.typography.headlineSmall)

            Text(
                "While a session is running, the app can show a notification with the current " +
                    "segment and buttons to pause, skip or end it. That means you can control " +
                    "a workout from the lock screen without unlocking your phone.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "It appears only while a session is running, and disappears when it ends.",
                        "It never makes a sound or vibrates — the coaching cues do that.",
                        "This app sends nothing else. No reminders, no streak nudges, nothing.",
                    ).forEach { point ->
                        Text("• $point", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Text(
                "Your workouts run either way. Without it you will need to reopen the app to " +
                    "pause or skip. You can change this any time in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Show session controls")
            }
            TextButton(onClick = onDecided, modifier = Modifier.fillMaxWidth()) {
                Text("Not now")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationPermissionPreview() {
    VisceralFitTheme {
        NotificationPermissionScreen(onDecided = {})
    }
}

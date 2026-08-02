package com.visceralfit.feature.workout

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * The runtime request for `POST_NOTIFICATIONS`, asked at the moment a session is about to
 * start (closes KI-0016).
 *
 * ## Why the permission is asked for at all
 *
 * The app needs no permission to *run* a session — the clock lives in a foreground service and
 * the screen shows everything. What the permission buys is the session notification, and with
 * it the pause, skip and end controls on the lock screen. Without it, a user who locks the
 * phone mid-interval has to unlock and reopen the app to pause. On Android 13 and later the
 * permission is not granted by declaring it, so until now the notification and its controls
 * simply never appeared on the operator's device (Android 16).
 *
 * ## Why it is asked here rather than at launch
 *
 * A permission prompt on first launch, before the user has seen what the app does, is the
 * pattern that trains people to tap Deny. Asked as they start their first session, the
 * rationale is about something they are doing right now: "so you can pause from the lock
 * screen". `framework/11_permissions_and_privacy.md` requires the rationale to name the
 * concrete benefit, and this is the only point at which one exists.
 *
 * ## What it must never do
 *
 * Block. Every path through [SessionNotificationPermission.launch] ends in [onProceed] —
 * granted, denied, dismissed, or on a version where the permission does not exist. A denied
 * notification permission costs the lock-screen controls and nothing else, so a flow that
 * could strand a user who tapped Deny would be a far worse defect than the one it fixes.
 */
@Composable
internal fun rememberSessionNotificationPermission(
    onProceed: () -> Unit,
): SessionNotificationPermission {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }

    // The result is deliberately ignored. The session starts either way, and branching on it
    // would be the first step towards a flow that can refuse to start.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { onProceed() },
    )

    val controller = remember(launcher, onProceed) {
        SessionNotificationPermission(
            onNeedsRationale = { showRationale = true },
            onProceed = onProceed,
            isGranted = { isNotificationPermissionGranted(context) },
            requestPermission = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
        )
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = {
                // Dismissing is a refusal, and a refusal still starts the session.
                showRationale = false
                onProceed()
            },
            title = { Text("Show session controls?") },
            text = {
                Text(
                    "A notification keeps the current segment and the pause, skip and end " +
                        "buttons on your lock screen, so you can control the session without " +
                        "unlocking. Your workout runs exactly the same without it.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        controller.request()
                    },
                ) { Text("Allow") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        onProceed()
                    },
                ) { Text("Not now") }
            },
        )
    }

    return controller
}

/**
 * The three-way decision, separated from the composable so it can be reasoned about — and
 * tested — without a Compose runtime or a device.
 */
internal class SessionNotificationPermission(
    private val onNeedsRationale: () -> Unit,
    private val onProceed: () -> Unit,
    private val isGranted: () -> Boolean,
    private val requestPermission: () -> Unit,
) {
    /**
     * Starts the session, asking for the permission first if it is worth asking.
     *
     * Three outcomes, and the first two are indistinguishable to the user: already granted, or
     * a version with no such permission, both proceed straight through. Only the third shows
     * anything.
     */
    fun launch() {
        if (isGranted()) {
            onProceed()
        } else {
            onNeedsRationale()
        }
    }

    /** Called when the user accepts the rationale. */
    fun request() = requestPermission()
}

/**
 * True when the session notification will actually appear.
 *
 * Below API 33 the permission does not exist and notifications are granted by installation, so
 * "granted" is the honest answer rather than a special case the caller has to know about.
 */
internal fun isNotificationPermissionGranted(context: Context): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

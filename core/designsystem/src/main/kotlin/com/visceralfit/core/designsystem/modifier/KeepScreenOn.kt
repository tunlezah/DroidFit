package com.visceralfit.core.designsystem.modifier

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Holds the display awake while the calling composable is in the composition
 * (PRD REQ-070).
 *
 * Why FLAG_KEEP_SCREEN_ON and not a WakeLock: the window flag is scoped to the
 * activity and is released automatically if the process dies, so it cannot leak and
 * flatten the battery. `PowerManager.SCREEN_BRIGHT_WAKE_LOCK` is deprecated and
 * counts against the Android vitals excessive-wake-lock metric. Google's own
 * guidance is explicit that the flag belongs in an activity and nowhere else.
 *
 * Compose 1.9 added `Modifier.keepScreenOn()`, which does the same thing more
 * neatly. This helper exists because it also handles [enabled] flipping at runtime
 * when the user toggles the setting mid-session, and because it degrades cleanly
 * when the composable is hosted outside an Activity (in a preview, or in a
 * screenshot test) instead of throwing.
 *
 * @param enabled when false the flag is cleared immediately, so toggling the
 *   setting during a workout takes effect without restarting the screen.
 */
@Composable
fun KeepScreenOn(enabled: Boolean = true) {
    val context = LocalContext.current
    DisposableEffect(context, enabled) {
        val window = context.findActivity()?.window
        if (window == null || !enabled) {
            return@DisposableEffect onDispose { }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

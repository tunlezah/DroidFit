package com.visceralfit.core.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Whether the session notification will actually be shown, and how to get the user to the
 * screen where they can change it.
 *
 * IN `core:common` RATHER THAN IN A FEATURE because three places need the same answer and
 * they must not disagree: onboarding decides whether to ask, the workout feature decides
 * whether the session notification is worth relying on, and Settings decides whether to offer
 * a way back. Two copies of a version check is how one of them ends up wrong on one API level.
 *
 * The single runtime permission this app has. Everything else in the manifest —
 * `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `VIBRATE` — is normal-level and
 * granted at install, so there is nothing else to ask for and nothing else to check.
 */
object NotificationPermission {

    /**
     * True when a posted notification will be visible to the user.
     *
     * Below API 33 the permission does not exist and notifications are granted by
     * installation, so "granted" is the honest answer rather than a special case every caller
     * has to remember. Note this reports the *permission*, not whether the user has since
     * muted the app's channel — a muted channel still posts a visible silent notification,
     * which is exactly what this one is (`IMPORTANCE_LOW`, `setSilent(true)`).
     */
    fun isGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }

    /**
     * An intent onto this app's notification settings, for the user who declined during
     * onboarding and later changed their mind.
     *
     * Necessary because a runtime permission can only be requested a limited number of times:
     * once Android stops showing the system dialog, the app's own request is a silent no-op and
     * the only remaining route is system settings. Offering it is the difference between "you
     * declined once, permanently" and "you can change this whenever you like".
     */
    fun settingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

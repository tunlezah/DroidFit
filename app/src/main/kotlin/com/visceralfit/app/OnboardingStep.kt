package com.visceralfit.app

/**
 * Which of the app's gates the user is behind, if any.
 *
 * WHY THIS IS A FUNCTION AND NOT A `when` INSIDE `setContent`: the ordering *is* the
 * requirement. The safety notice must come before anything that lets a user make an intensity
 * decision (REQ-005), the notification step must come after it and must be skipped when there
 * is nothing to ask (D-0046), and neither may be reachable once passed. That is four rules
 * about sequence, and a `when` block inside a composable is not somewhere they can be
 * asserted. Extracting it costs one file and makes every rule a test.
 */
internal enum class OnboardingStep {
    /** Preferences have not loaded. Show nothing rather than flashing a gate that may not apply. */
    WAITING,

    /** REQ-005: the medical-safety notice, acknowledged once. */
    SAFETY_NOTICE,

    /** The one runtime permission this app has (D-0046). */
    NOTIFICATION_PERMISSION,

    /** Onboarding is over. */
    APP,
    ;

    companion object {
        /**
         * The step to show.
         *
         * [notificationsGranted] is passed in rather than read here because it is a live
         * platform question — the user can change it in Android settings while the app is
         * backgrounded — and this function has to stay free of a `Context` to be testable.
         *
         * Note what the notification step depends on: *either* having already asked or already
         * holding the permission is enough to skip it. Both matter. Without the "already asked"
         * half the step reappears on every launch for anyone who declined, which is nagging.
         * Without the "already granted" half a user who granted it in system settings before
         * ever opening the app would still be asked.
         */
        fun of(state: MainUiState, notificationsGranted: Boolean): OnboardingStep {
            val ready = state as? MainUiState.Ready ?: return WAITING
            return when {
                !ready.safetyNoticeAcknowledged -> SAFETY_NOTICE
                !ready.notificationPermissionRequested && !notificationsGranted -> NOTIFICATION_PERMISSION
                else -> APP
            }
        }
    }
}

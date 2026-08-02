package com.visceralfit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visceralfit.app.ui.NotificationPermissionScreen
import com.visceralfit.app.ui.SafetyNoticeScreen
import com.visceralfit.app.ui.VisceralFitApp
import com.visceralfit.core.common.NotificationPermission
import com.visceralfit.core.designsystem.theme.VisceralFitTheme
import com.visceralfit.domain.model.ThemePreference
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Android 15 enforces edge-to-edge for apps targeting SDK 35+, so this is not
        // optional decoration. Insets are consumed per screen in the feature modules;
        // see /framework/15_device_targets_motorola_edge_60.md §Insets.
        enableEdgeToEdge()

        // Hold the system splash until preferences resolve, so the app never flashes
        // the light theme at a user who chose dark.
        var themeResolved = false
        splash.setKeepOnScreenCondition { !themeResolved }

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            themeResolved = state is MainUiState.Ready

            val ready = state as? MainUiState.Ready
            val systemDark = isSystemInDarkTheme()
            val context = LocalContext.current
            VisceralFitTheme(
                darkTheme = ready?.theme?.resolvesToDark(systemDark) ?: systemDark,
                amoled = ready?.amoled == true,
                dynamicColour = ready?.dynamicColour != false,
            ) {
                // Onboarding is two steps and both gate the shell rather than the player. The
                // ordering rules live in `OnboardingStep`, where they are tested; this is only
                // the mapping from step to screen.
                //
                // `isGranted` is read here rather than held in state on purpose: the user can
                // change it in Android settings while the app is backgrounded, so a cached copy
                // would be a copy that goes stale.
                when (OnboardingStep.of(state, NotificationPermission.isGranted(context))) {
                    OnboardingStep.WAITING -> Unit
                    OnboardingStep.SAFETY_NOTICE ->
                        SafetyNoticeScreen(onAcknowledge = viewModel::acknowledgeSafetyNotice)

                    OnboardingStep.NOTIFICATION_PERMISSION ->
                        NotificationPermissionScreen(
                            onDecided = viewModel::markNotificationPermissionRequested,
                        )

                    OnboardingStep.APP -> VisceralFitApp()
                }
            }
        }
    }
}

private fun ThemePreference.resolvesToDark(isSystemDark: Boolean): Boolean = when (this) {
    ThemePreference.SYSTEM -> isSystemDark
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}

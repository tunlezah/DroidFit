package com.visceralfit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visceralfit.app.ui.VisceralFitApp
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
            VisceralFitTheme(
                darkTheme = ready?.theme?.resolvesToDark(systemDark) ?: systemDark,
                amoled = ready?.amoled == true,
                dynamicColour = ready?.dynamicColour != false,
            ) {
                VisceralFitApp()
            }
        }
    }
}

private fun ThemePreference.resolvesToDark(isSystemDark: Boolean): Boolean = when (this) {
    ThemePreference.SYSTEM -> isSystemDark
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}

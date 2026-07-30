package com.visceralfit.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.visceralfit.core.designsystem.icon.VisceralFitIcons
import com.visceralfit.feature.history.HistoryRoute
import com.visceralfit.feature.progress.ProgressRoute
import com.visceralfit.feature.settings.SettingsRoute
import com.visceralfit.feature.workout.WorkoutHomeRoute

/**
 * Top-level shell: four destinations in a bottom navigation bar.
 *
 * Routes are string constants in one place rather than scattered through feature
 * modules, because a typo'd route string fails at runtime rather than compile time.
 * Phase 05 replaces these with type-safe routes once the argument shapes settle —
 * see /framework/10_screen_specs.md §Navigation.
 */
@Composable
fun VisceralFitApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.WORKOUT.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            appGraph()
        }
    }
}

private fun NavGraphBuilder.appGraph() {
    composable(TopLevelDestination.WORKOUT.route) { WorkoutHomeRoute() }
    composable(TopLevelDestination.HISTORY.route) { HistoryRoute() }
    composable(TopLevelDestination.PROGRESS.route) { ProgressRoute() }
    composable(TopLevelDestination.SETTINGS.route) { SettingsRoute() }
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    WORKOUT("workout", "Train", VisceralFitIcons.Dumbbell),
    HISTORY("history", "History", VisceralFitIcons.Calendar),
    PROGRESS("progress", "Progress", VisceralFitIcons.TrendingUp),
    SETTINGS("settings", "Settings", VisceralFitIcons.Settings),
}

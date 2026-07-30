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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.visceralfit.core.designsystem.icon.VisceralFitIcons
import com.visceralfit.feature.history.HistoryRoute
import com.visceralfit.feature.progress.ProgressRoute
import com.visceralfit.feature.settings.SettingsRoute
import com.visceralfit.feature.workout.WorkoutHomeRoute
import com.visceralfit.feature.workout.player.WorkoutPlayerRoute

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

    // The player is full-screen: a bottom bar during a session invites a mis-tap that
    // navigates away from a running workout, and the session controls need the room.
    val showBottomBar = currentRoute != PLAYER_ROUTE

    Scaffold(
        bottomBar = {
            if (!showBottomBar) return@Scaffold
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
            appGraph(navController)
        }
    }
}

private fun NavGraphBuilder.appGraph(navController: NavHostController) {
    composable(TopLevelDestination.WORKOUT.route) {
        WorkoutHomeRoute(onSessionStarted = { navController.navigate(PLAYER_ROUTE) })
    }
    composable(TopLevelDestination.HISTORY.route) { HistoryRoute() }
    composable(TopLevelDestination.PROGRESS.route) { ProgressRoute() }
    composable(TopLevelDestination.SETTINGS.route) { SettingsRoute() }
    composable(PLAYER_ROUTE) {
        WorkoutPlayerRoute(
            onSessionEnded = {
                // popBackStack rather than navigate: the player must leave the back stack
                // when it ends, or a back press lands on a finished session.
                navController.popBackStack(TopLevelDestination.WORKOUT.route, inclusive = false)
            },
        )
    }
}

/**
 * The player takes no arguments — the session it shows lives in the coordinator the service
 * writes (ADR-0008), which is what lets it survive process recreation. Passing a `Workout`
 * through a navigation argument would need it to be serialisable and would give the player a
 * second, staler source of truth.
 */
private const val PLAYER_ROUTE = "player"

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

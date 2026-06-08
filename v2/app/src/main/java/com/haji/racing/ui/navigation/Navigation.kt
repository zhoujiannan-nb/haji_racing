package com.haji.racing.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.haji.racing.ui.history.HistoryScreen
import com.haji.racing.ui.profile.ProfileScreen
import com.haji.racing.ui.recording.RecordingScreen
import com.haji.racing.ui.track.TrackScreen
import com.haji.racing.ui.video.VideoScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val iconSelected: ImageVector) {
    data object Track : Screen("track", "赛道", Icons.Outlined.Map, Icons.Filled.Map)
    data object Recording : Screen("recording", "记录", Icons.Outlined.Home, Icons.Filled.Home)
    data object History : Screen("history", "轨迹", Icons.Outlined.History, Icons.Filled.History)
    data object Profile : Screen("profile", "我的", Icons.Outlined.Person, Icons.Filled.Person)
    data object Video : Screen("video", "视频", Icons.Outlined.History, Icons.Filled.History)
}

val bottomScreens = listOf(Screen.Track, Screen.Recording, Screen.History, Screen.Profile)

@Composable
fun HajiRacingNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomScreens.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomScreens.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.iconSelected else screen.icon,
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Recording.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Track.route) { TrackScreen(navController) }
            composable(Screen.Recording.route) { RecordingScreen(navController) }
            composable(Screen.History.route) { HistoryScreen(navController) }
            composable(Screen.Profile.route) { ProfileScreen(navController) }
            composable(Screen.Video.route) { VideoScreen(navController) }
        }
    }
}

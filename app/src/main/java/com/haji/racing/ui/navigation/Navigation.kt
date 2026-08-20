package com.haji.racing.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.haji.racing.ui.home.HomeScreen
import com.haji.racing.ui.profile.ProfileScreen
import com.haji.racing.ui.record.RecordingLiveScreen
import com.haji.racing.ui.records.RecordDetailScreen
import com.haji.racing.ui.records.RecordsScreen
import com.haji.racing.ui.theme.RacingBg
import com.haji.racing.ui.theme.RacingCardElevated
import com.haji.racing.ui.theme.RacingPrimary
import com.haji.racing.ui.theme.TextSecondary
import com.haji.racing.ui.track.TrackCreateScreen
import com.haji.racing.ui.track.TrackDetailScreen
import com.haji.racing.ui.track.TracksScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val iconSelected: ImageVector) {
    data object Home : Screen("home", "记录", Icons.AutoMirrored.Outlined.FactCheck, Icons.AutoMirrored.Filled.FactCheck)
    data object Tracks : Screen("tracks", "赛道", Icons.Outlined.Flag, Icons.Filled.Flag)
    data object Records : Screen("records", "轨迹", Icons.Outlined.History, Icons.Filled.History)
    data object Profile : Screen("profile", "我的", Icons.Outlined.Person, Icons.Filled.Person)
}

val bottomScreens = listOf(Screen.Home, Screen.Tracks, Screen.Records, Screen.Profile)

object Routes {
    const val TRACK_CREATE = "track_create"
    const val TRACK_DETAIL = "track_detail/{trackUid}"
    const val RECORD_DETAIL = "record_detail/{recordingUid}"
    const val RECORDING_LIVE = "recording_live"

    fun trackDetail(trackUid: String) = "track_detail/$trackUid"
    fun recordDetail(recordingUid: String) = "record_detail/$recordingUid"
}

@Composable
fun HajiRacingNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomScreens.map { it.route }

    Scaffold(
        containerColor = RacingBg,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = RacingCardElevated,
                    tonalElevation = 0.dp,
                ) {
                    bottomScreens.forEach { screen ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.iconSelected else screen.icon,
                                    contentDescription = screen.label,
                                    tint = if (selected) RacingPrimary else TextSecondary,
                                )
                            },
                            label = {
                                Text(
                                    text = screen.label,
                                    fontSize = 11.sp,
                                    color = if (selected) RacingPrimary else TextSecondary,
                                )
                            },
                            selected = selected,
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = RacingPrimary,
                                indicatorColor = RacingPrimary.copy(alpha = 0.12f),
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Tracks.route) { TracksScreen(navController) }
            composable(Screen.Records.route) { RecordsScreen(navController) }
            composable(Screen.Profile.route) { ProfileScreen(navController) }
            composable(Routes.TRACK_CREATE) { TrackCreateScreen(navController) }
            composable(Routes.TRACK_DETAIL) {
                TrackDetailScreen(navController)
            }
            composable(Routes.RECORD_DETAIL) {
                RecordDetailScreen(navController)
            }
            composable(Routes.RECORDING_LIVE) {
                RecordingLiveScreen(navController)
            }
        }
    }
}

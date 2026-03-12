package com.example.presentmate

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.presentmate.ui.screens.*
import kotlinx.coroutines.launch

// Sealed class for Navigation items
sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("main", "Home", Icons.Filled.Home)
    object Overview : Screen("overview", "Overview", Icons.Filled.BarChart)
    object Location : Screen("location", "Location", Icons.Filled.LocationOn)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object AboutDeveloper : Screen("aboutDeveloper", "About Developer", Icons.Filled.Info)
}

val navItems = listOf(
    Screen.Home,
    Screen.Overview,
    Screen.Location,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val isHomeScreen = currentRoute == Screen.Home.route

    var currentScreenTitle = navItems.find { it.route == currentDestination?.route }?.label
    if (currentScreenTitle == null) { // Handle titles for screens not in bottom nav
        currentScreenTitle = when (currentDestination?.route) {
            "recycleBin" -> "Recycle Bin"
            "helpScreen" -> "Help"
            "whyPresentMateScreen" -> "Why Present Mate?"
            "aboutDeveloper" -> "About the Developer"
            "locationPickerScreen" -> "Select Location"
            "geofenceScreen" -> "Geofences"
            "calendarSyncSettings" -> "Calendar Sync"
            "preferences" -> "Preferences"
            "notificationPreferences" -> "Notification Preferences"
            "aiPreferences" -> "AI Settings"
            "changelog" -> "Changelog"
            else -> "Present Mate"
        }
    }

    val routesWithoutBottomBar = listOf("locationPickerScreen")
    val routesWithCustomTopBar = listOf("locationPickerScreen") // These screens provide their own top bar

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isHomeScreen, // Only enable gestures on the home screen
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                navItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                            }
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (currentRoute !in routesWithCustomTopBar) {
                    AppTopBar(title = currentScreenTitle,
                        isHomeScreen = isHomeScreen,
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            },
            bottomBar = {
                if (currentRoute !in routesWithoutBottomBar) {
                    AppBottomNavigationBar(navController = navController)
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            val context = LocalContext.current
            val searchHistoryRepository = remember { SearchHistoryRepository(context) }
            val database = remember { com.example.presentmate.db.PresentMateDatabase.getDatabase(context) }
            val savedPlacesRepository = remember { com.example.presentmate.data.SavedPlacesRepository(database.savedPlaceDao()) }

            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { AttendanceScreen(navController = navController) }
                composable(Screen.Overview.route) { OverviewScreen() }
                composable(Screen.Location.route) { LocationScreen(navController = navController) }
                composable("locationPickerScreen") {
                    LocationPickerScreen(
                        searchHistoryRepository = searchHistoryRepository,
                        savedPlacesRepository = savedPlacesRepository,
                        onLocationConfirmed = { _ ->
                            navController.popBackStack()
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("geofenceScreen") { GeofenceScreen(navController = navController) }
                composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
                composable(Screen.AboutDeveloper.route) { AboutDeveloperScreen() }
                composable("recycleBin") { RecycleBinScreen() }
                composable("helpScreen") { HelpScreen(_navController = navController) }
                composable("whyPresentMateScreen") { WhyPresentMateScreen(_navController = navController) }
                composable("calendarSyncSettings") { CalendarSyncSettingsScreen() }
                composable("aiAssistant") { AIAssistantScreen() }
                composable("preferences") { PreferencesScreen(navController = navController) }
                composable("notificationPreferences") { NotificationPreferencesScreen() }
                composable("aiPreferences") { AIPreferencesScreen() }
                composable("changelog") { ChangelogScreen() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, isHomeScreen: Boolean, onMenuClick: () -> Unit, onBackClick: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (isHomeScreen) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                }
            } else {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        }
    )
}

@Composable
fun AppBottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        navItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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

package com.example.presentmate

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.presentmate.ui.theme.PresentMateTheme
import kotlinx.coroutines.launch

// Sealed class for Navigation items
sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("main", "Home", Icons.Filled.Home)
    object Overview : Screen("overview", "Overview", Icons.Filled.BarChart)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

val navItems = listOf(
    Screen.Home,
    Screen.Overview,
    Screen.Settings,
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PresentMateTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                var showLocationDialog by remember { mutableStateOf(false) }

                // Check for location services
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                if (!isLocationEnabled) {
                    showLocationDialog = true
                }

                if (showLocationDialog) {
                    LocationServicesDisabledDialog(onDismiss = { showLocationDialog = false }) {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                        showLocationDialog = false
                    }
                }

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
                        else -> "Present Mate"
                    }
                }

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
                            AppTopBar(title = currentScreenTitle,
                                isHomeScreen = isHomeScreen,
                                onMenuClick = {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        },
                        bottomBar = {
                            AppBottomNavigationBar(navController = navController)
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Home.route) { AttendanceScreen() }
                            composable(Screen.Overview.route) { OverviewScreen() }
                            composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
                            composable("recycleBin") { RecycleBinScreen() }
                            composable("helpScreen") { HelpScreen(navController = navController) } 
                            composable("whyPresentMateScreen") { WhyPresentMateScreen(navController = navController) } 
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationServicesDisabledDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location Services Disabled") },
        text = { Text("Please enable location services for automatic session tracking.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Enable")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

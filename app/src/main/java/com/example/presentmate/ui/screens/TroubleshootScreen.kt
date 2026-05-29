package com.example.presentmate.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun TroubleshootScreen(navController: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Trigger state to force re-evaluation of checks when screen resumes
    var refreshTrigger by remember { mutableStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-refresh every few seconds just in case
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            refreshTrigger++
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "System Diagnostics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Use these checks to ensure your device is configured correctly for all PresentMate features.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item { ActivityDiagnosticsCard(context, refreshTrigger) }
        item { LocationDiagnosticsCard(context, refreshTrigger) }
        item { NetworkDiagnosticsCard(context, refreshTrigger) }
        item { NotificationDiagnosticsCard(context, refreshTrigger) }
        item { BatteryDiagnosticsCard(context, refreshTrigger) }
    }
}

// ── Components ──────────────────────────────────────────────────────────

@Composable
fun DiagnosticCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
fun DiagnosticRow(
    label: String,
    isPassed: Boolean,
    errorMessage: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isPassed) Color.Green else MaterialTheme.colorScheme.error)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isPassed) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Passed",
                    tint = Color.Green,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = "Failed",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        if (!isPassed && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 4.dp)
            )
        }
        
        if (!isPassed && actionLabel != null && onActionClick != null) {
            Row(
                modifier = Modifier
                    .padding(start = 20.dp, top = 4.dp)
                    .clickable(onClick = onActionClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ── Specific Cards ──────────────────────────────────────────────────────

@Composable
fun ActivityDiagnosticsCard(context: Context, trigger: Int) {
    val sensorManager = remember(trigger) { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val hasStepCounter = remember(trigger) { sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null }
    val hasStepDetector = remember(trigger) { sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null }
    val hasActivityPermission = remember(trigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    DiagnosticCard("Step Tracking", Icons.Filled.DirectionsWalk) {
        DiagnosticRow(
            label = "Hardware Step Counter",
            isPassed = hasStepCounter,
            errorMessage = if (!hasStepCounter) "Your device lacks a hardware step counter sensor." else null
        )
        DiagnosticRow(
            label = "Hardware Step Detector",
            isPassed = hasStepDetector,
            errorMessage = if (!hasStepDetector) "Your device lacks a hardware step detector sensor." else null
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            DiagnosticRow(
                label = "Activity Recognition Permission",
                isPassed = hasActivityPermission,
                errorMessage = "Required to read step data.",
                actionLabel = "Open App Settings",
                onActionClick = { openAppSettings(context) }
            )
        }
    }
}

@Composable
fun LocationDiagnosticsCard(context: Context, trigger: Int) {
    val locationManager = remember(trigger) { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }
    val isGpsEnabled = remember(trigger) { locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true }
    val hasFineLocation = remember(trigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val hasBackgroundLocation = remember(trigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    DiagnosticCard("Location & Geofencing", Icons.Filled.LocationOn) {
        DiagnosticRow(
            label = "GPS / Location Services",
            isPassed = isGpsEnabled,
            errorMessage = "Location services are disabled.",
            actionLabel = "Enable Location",
            onActionClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
        )
        DiagnosticRow(
            label = "Precise Location Permission",
            isPassed = hasFineLocation,
            errorMessage = "Required for accurate geofencing.",
            actionLabel = "Open App Settings",
            onActionClick = { openAppSettings(context) }
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            DiagnosticRow(
                label = "Background Location Permission",
                isPassed = hasBackgroundLocation,
                errorMessage = "Required to detect geofence entries in background. Must be 'Allow all the time'.",
                actionLabel = "Open App Settings",
                onActionClick = { openAppSettings(context) }
            )
        }
    }
}

@Composable
fun NetworkDiagnosticsCard(context: Context, trigger: Int) {
    val connectivityManager = remember(trigger) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    val hasInternet = remember(trigger) {
        val network = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(network)
        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
    
    val googlePlayServicesAvailable = remember(trigger) {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }

    val user = remember(trigger) { FirebaseAuth.getInstance().currentUser }
    val isGoogleSignedIn = remember(trigger) { user != null && !user.isAnonymous }

    DiagnosticCard("Cloud & Network", if (hasInternet) Icons.Filled.CloudDone else Icons.Filled.CloudOff) {
        DiagnosticRow(
            label = "Internet Connection",
            isPassed = hasInternet,
            errorMessage = "No active internet connection. AI Chat and Drive Sync will fail.",
            actionLabel = "Check Network",
            onActionClick = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
        )
        DiagnosticRow(
            label = "Google Play Services",
            isPassed = googlePlayServicesAvailable,
            errorMessage = "Google Play Services is missing or out of date."
        )
        DiagnosticRow(
            label = "Google Drive Access",
            isPassed = isGoogleSignedIn,
            errorMessage = "You are not signed in with a Google Account. Cloud backups will not work.",
            actionLabel = "Go to Sign In",
            onActionClick = { /* User can navigate to settings account section, or we handle it */ }
        )
    }
}

@Composable
fun NotificationDiagnosticsCard(context: Context, trigger: Int) {
    val hasNotificationPermission = remember(trigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    val hasExactAlarmPermission = remember(trigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    DiagnosticCard("Notifications & Alarms", Icons.Filled.NotificationsActive) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            DiagnosticRow(
                label = "Notification Permission",
                isPassed = hasNotificationPermission,
                errorMessage = "Required to show session reminders and step alerts.",
                actionLabel = "Open App Settings",
                onActionClick = { openAppSettings(context) }
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DiagnosticRow(
                label = "Exact Alarms Permission",
                isPassed = hasExactAlarmPermission,
                errorMessage = "Required for precise study reminders.",
                actionLabel = "Allow Exact Alarms",
                onActionClick = { 
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    intent.data = Uri.parse("package:${context.packageName}")
                    try { context.startActivity(intent) } catch (e: Exception) { openAppSettings(context) }
                }
            )
        }
    }
}

@Composable
fun BatteryDiagnosticsCard(context: Context, trigger: Int) {
    val powerManager = remember(trigger) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringOptimizations = remember(trigger) {
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    DiagnosticCard("Battery Optimization", Icons.Filled.BatteryAlert) {
        DiagnosticRow(
            label = "Unrestricted Background Work",
            isPassed = isIgnoringOptimizations,
            errorMessage = "Battery optimizations may kill background geofencing and step counting.",
            actionLabel = "Disable Restrictions",
            onActionClick = {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                try { context.startActivity(intent) } catch (e: Exception) { openAppSettings(context) }
            }
        )
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

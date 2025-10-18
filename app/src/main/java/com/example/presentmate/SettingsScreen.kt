package com.example.presentmate

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.presentmate.db.AppDatabase
import com.example.presentmate.geofence.GeofenceBroadcastReceiver
import com.example.presentmate.geofence.GeofenceManager
import kotlinx.coroutines.flow.map

fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "N/A"
    } catch (_: Exception) {
        "N/A"
    }
}

@Composable
fun UnderProgressDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Feature Under Progress") },
        text = { Text("This feature is currently under development and will be available soon.") },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val prefs = remember { context.getSharedPreferences("geofence_prefs", Context.MODE_PRIVATE) }
    val deletedRecordsCount by db.attendanceDao().getAllDeletedRecords()
        .map { it.size }
        .collectAsState(initial = 0)
    val appVersion = remember { getAppVersion(context) }
    var showUnderProgressDialog by remember { mutableStateOf(false) }
    var geofenceEnabled by remember { mutableStateOf(prefs.getBoolean("geofence_enabled", false)) }
    val geofenceManager = remember { GeofenceManager(context) }

    val locationPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
                val longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0

                with(prefs.edit()) {
                    putFloat("geofence_latitude", latitude.toFloat())
                    putFloat("geofence_longitude", longitude.toFloat())
                    putBoolean("geofence_enabled", true)
                    apply()
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, GeofenceBroadcastReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                geofenceManager.addGeofence(
                    "library_geofence",
                    latitude,
                    longitude,
                    100f,
                    pendingIntent
                )
                geofenceEnabled = true
            }
        }
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !permissions.getOrDefault(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false)
                ) {
                    // TODO: Explain to the user why background location is needed
                } else {
                    val intent = Intent(context, LocationPickerActivity::class.java)
                    locationPickerLauncher.launch(intent)
                }
            } else {
                Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    if (showUnderProgressDialog) {
        UnderProgressDialog { showUnderProgressDialog = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsGroup("Data Management") {
            SettingsItem(
                title = "Automatic Session Tracking",
                description = if (geofenceEnabled) "Enabled" else "Disabled",
                icon = Icons.Default.LocationOn,
                onClick = { /* Toggle Switch will handle it */ },
                showArrow = false,
                trailingContent = {
                    Switch(
                        checked = geofenceEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val permissions = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                }
                                locationPermissionLauncher.launch(permissions.toTypedArray())
                            } else {
                                with(prefs.edit()) {
                                    remove("geofence_latitude")
                                    remove("geofence_longitude")
                                    putBoolean("geofence_enabled", false)
                                    apply()
                                }
                                val pendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    0,
                                    Intent(context, GeofenceBroadcastReceiver::class.java),
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                                )
                                geofenceManager.removeGeofence(pendingIntent)
                                geofenceEnabled = false
                            }
                        }
                    )
                }
            )
            SettingsItem(
                title = "Recycle Bin",
                description = "$deletedRecordsCount items",
                icon = Icons.Default.DeleteOutline,
                onClick = { navController.navigate("recycleBin") }
            )
            SettingsItem(
                title = "Export Data",
                description = "Save records to a file",
                icon = Icons.Filled.FileUpload,
                onClick = { showUnderProgressDialog = true }
            )
            SettingsItem(
                title = "Import Data",
                description = "Restore records from a file",
                icon = Icons.Filled.FileDownload,
                onClick = { showUnderProgressDialog = true }
            )
        }

        SettingsGroup("General") {
            SettingsItem(
                title = "App Version",
                description = appVersion,
                icon = Icons.Filled.Verified,
                onClick = { Toast.makeText(context, "App Version: $appVersion", Toast.LENGTH_SHORT).show() }
            )
        }

        SettingsGroup("Information") {
            SettingsItem(
                title = "Help",
                description = "Find answers to your questions",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                onClick = { navController.navigate("helpScreen") }
            )
            SettingsItem(
                title = "Why Present Mate?",
                description = "Learn about the app's mission",
                icon = Icons.Outlined.Info,
                onClick = { navController.navigate("whyPresentMateScreen") }
            )
            SettingsItem(
                title = "About Developer",
                description = "Learn more about the creator",
                icon = Icons.Filled.Info,
                onClick = { navController.navigate("aboutDeveloper") }
            )
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    showArrow: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailingContent != null) {
                trailingContent()
            } else if (showArrow) {
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Navigate")
            }
        }
    }
}

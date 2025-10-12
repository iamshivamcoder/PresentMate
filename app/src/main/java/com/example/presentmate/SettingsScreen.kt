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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.presentmate.db.AppDatabase
import com.example.presentmate.geofence.GeofenceBroadcastReceiver
import com.example.presentmate.geofence.GeofenceManager
import kotlinx.coroutines.flow.map

fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (_: Exception) {
        "N/A"
    }
}

// Helper function to check if storage permissions are granted
fun hasStoragePermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+ (API 33+) - Check for READ_MEDIA_DOCUMENTS
        ContextCompat.checkSelfPermission(
            context,
            "android.permission.READ_MEDIA_DOCUMENTS"
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        // For all other versions, check READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
    }
}

// Helper function to get required permissions for current Android version
fun getRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf("android.permission.READ_MEDIA_DOCUMENTS")
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val deletedRecordsCount by db.attendanceDao().getAllDeletedRecords()
        .map { it.size }
        .collectAsState(initial = 0)
    val appVersion = remember { getAppVersion(context) }
    val backupFileName = "PresentMate_Backup.doc"
    var showUnderProgressDialog by remember { mutableStateOf(false) }
    var geofenceEnabled by remember { mutableStateOf(false) }
    val geofenceManager = remember { GeofenceManager(context) }

    val locationPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
                val longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, GeofenceBroadcastReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                geofenceManager.addGeofence("library_geofence", latitude, longitude, 100f, pendingIntent)
                geofenceEnabled = true
            }
        }
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                val intent = Intent(context, LocationPickerActivity::class.java)
                locationPickerLauncher.launch(intent)
            } else {
                Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions: Map<String, Boolean> ->
            val allGranted = permissions.values.all { it }
            if (allGranted || hasStoragePermissions(context)) {
                // Permissions granted, proceed with export
                // This logic will be replaced by the dialog
                Toast.makeText(context, "Permissions granted, but feature is under progress.", Toast.LENGTH_LONG).show()
            } else {
                // Permissions denied
                Toast.makeText(context, "Storage permission denied. Export/Import cancelled.", Toast.LENGTH_LONG).show()
            }
        }
    )

    if (showUnderProgressDialog) {
        UnderProgressDialog { showUnderProgressDialog = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsItem(
            title = "Recycle Bin",
            description = "$deletedRecordsCount items",
            icon = Icons.Default.DeleteOutline,
            onClick = {
                navController.navigate("recycleBin")
            }
        )
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
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
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                )
                            )
                        } else {
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
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        SettingsItem(
            title = "App Version",
            description = appVersion,
            icon = Icons.Filled.Verified,
            onClick = { Toast.makeText(context, "App Version: $appVersion", Toast.LENGTH_SHORT).show() }
        )
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        SettingsItem(
            title = "Check Permissions",
            description = if (hasStoragePermissions(context)) "Storage permissions granted" else "Storage permissions needed",
            icon = Icons.Filled.Verified,
            onClick = {
                val hasPerms = hasStoragePermissions(context)
                if (hasPerms) {
                    Toast.makeText(
                        context,
                        "All required storage permissions are granted",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val requiredPermissions = getRequiredPermissions()
                    if (requiredPermissions.isNotEmpty()) {
                        Toast.makeText(context, "Requesting permissions...", Toast.LENGTH_SHORT)
                            .show()
                        requestPermissionLauncher.launch(requiredPermissions)
                    } else {
                        Toast.makeText(
                            context,
                            "No permissions required for this Android version",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
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
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        SettingsItem(
            title = "Export Data",
            description = "Save records to $backupFileName",
            icon = Icons.Filled.FileUpload,
            onClick = {
                showUnderProgressDialog = true
            }
        )
        SettingsItem(
            title = "Import Data",
            description = "Restore records from $backupFileName",
            icon = Icons.Filled.FileDownload,
            onClick = {
                showUnderProgressDialog = true
            }
        )
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
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailingContent != null) {
                trailingContent()
            } else if (showArrow) {
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Navigate")
            }
        }
    }
}

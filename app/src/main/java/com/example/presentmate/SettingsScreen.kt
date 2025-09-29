package com.example.presentmate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.presentmate.db.AppDatabase
import com.example.presentmate.utils.DataExportUtils
import com.example.presentmate.utils.DataImportUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: Exception) {
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
    val coroutineScope = rememberCoroutineScope()
    val deletedRecordsCount by db.attendanceDao().getAllDeletedRecords()
        .map { it.size }
        .collectAsState(initial = 0)
    val appVersion = remember { getAppVersion(context) }
    val backupFileName = "PresentMate_Backup.doc"
    var showUnderProgressDialog by remember { mutableStateOf(false) }

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

    // Removed launchExportProcess as it's not directly used with the dialog approach for now.
    // If permissions were granted, the original export/import logic would have run.
    // Now, we just show "under progress".

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
        Divider()
        SettingsItem(
            title = "App Version",
            description = appVersion,
            icon = Icons.Filled.Verified,
            onClick = { Toast.makeText(context, "App Version: $appVersion", Toast.LENGTH_SHORT).show() }
        )
        Divider()
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
        Divider()
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
    showArrow: Boolean = true
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
            if (showArrow) {
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Navigate")
            }
        }
    }
}

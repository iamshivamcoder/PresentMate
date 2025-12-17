package com.example.presentmate.ui.screens

// Removed GeofenceBroadcastReceiver and GeofenceManager as they are no longer directly used here.
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.presentmate.db.AppDatabase
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
    // Removed prefs, geofenceManager, geofenceEnabled as they are no longer directly used here.
    val deletedRecordsCount by db.attendanceDao().getAllDeletedRecords()
        .map { it.size }
        .collectAsState(initial = 0)
    val appVersion = remember { getAppVersion(context) }
    var showUnderProgressDialog by remember { mutableStateOf(false) }

    // Removed locationPermissionLauncher as it's no longer used.

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
            // Removed Automatic Session Tracking SettingsItem
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
    icon: ImageVector,
    onClick: () -> Unit,
    showArrow: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = title, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
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

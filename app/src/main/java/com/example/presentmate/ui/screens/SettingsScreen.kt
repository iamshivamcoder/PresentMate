package com.example.presentmate.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.db.PresentMateDatabase
import com.example.presentmate.utils.DataTransferManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "N/A"
    } catch (_: Exception) {
        "N/A"
    }
}

@Composable
fun ImportConfirmDialog(
    records: List<AttendanceRecord>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Import") },
        text = {
            Column {
                Text("Found ${records.size} records to import.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This will add these records to your existing data. Continue?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Import")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SettingsScreen(
    navController: NavHostController,
    db: PresentMateDatabase = PresentMateDatabase.getDatabase(LocalContext.current),
    authViewModel: com.example.presentmate.ui.viewmodel.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unassigned"
    val deletedRecordsFlow = remember(db, uid) {
        db.attendanceDao().getAllDeletedRecords(uid).map { it.size }
    }
    val deletedRecordsCount by deletedRecordsFlow.collectAsState(initial = 0)
    
    val allRecordsFlow = remember(db, uid) {
        db.attendanceDao().getAllRecords(uid)
    }
    val allRecords by allRecordsFlow.collectAsState(initial = emptyList())
    val appVersion = remember { getAppVersion(context) }
    
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }

    var isBackingUpToDrive by remember { mutableStateOf(false) }
    var isRestoringFromDrive by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val intentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Google Drive authorized. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    // SAF launcher for creating export file
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            isExporting = true
            scope.launch {
                val records = db.attendanceDao().getAllRecords((com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unassigned")).first()
                when (val result = DataTransferManager.exportToCSV(context, it, records)) {
                    is DataTransferManager.ExportResult.Success -> {
                        Toast.makeText(
                            context,
                            "Exported ${result.recordCount} records successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is DataTransferManager.ExportResult.Error -> {
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    }
                }
                isExporting = false
            }
        }
    }

    // SAF launcher for selecting import file
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            isImporting = true
            scope.launch {
                when (val result = DataTransferManager.importFromCSV(context, it)) {
                    is DataTransferManager.ImportResult.Success -> {
                        pendingImportRecords = result.records
                        showImportConfirmDialog = true
                    }
                    is DataTransferManager.ImportResult.Error -> {
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    }
                }
                isImporting = false
            }
        }
    }

    // Import confirmation dialog
    if (showImportConfirmDialog && pendingImportRecords.isNotEmpty()) {
        ImportConfirmDialog(
            records = pendingImportRecords,
            onConfirm = {
                scope.launch {
                    pendingImportRecords.forEach { record ->
                        db.attendanceDao().insertRecord(record)
                    }
                    Toast.makeText(
                        context,
                        "Imported ${pendingImportRecords.size} records",
                        Toast.LENGTH_SHORT
                    ).show()
                    pendingImportRecords = emptyList()
                    showImportConfirmDialog = false
                }
            },
            onDismiss = {
                pendingImportRecords = emptyList()
                showImportConfirmDialog = false
            }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore from Google Drive") },
            text = { Text("This will merge your cloud backup into your current device. Proceed?") },
            confirmButton = {
                Button(onClick = {
                    showRestoreConfirm = false
                    isRestoringFromDrive = true
                    authViewModel.restoreDatabaseFromDrive { result ->
                        isRestoringFromDrive = false
                        if (result.isSuccess) {
                            val restored = result.getOrDefault(false)
                            if (restored) {
                                Toast.makeText(context, "Database restored successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No backup found in Google Drive.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val exception = result.exceptionOrNull()
                            if (exception is com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                                intentLauncher.launch(exception.intent)
                            } else {
                                Toast.makeText(context, "Failed to restore: ${exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            val user = FirebaseAuth.getInstance().currentUser
            val displayName = user?.displayName ?: "User"
            val email = user?.email ?: "Not signed in"
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Green) // Status indicator
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Online",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        SettingsGroup("Profile") {
            SettingsItem(
                title = "Manage Profile",
                description = "Update personal details",
                icon = Icons.Default.Person,
                onClick = { navController.navigate("manageProfile") }
            )
        }

        SettingsGroup("Preferences") {
            SettingsItem(
                title = "Preferences",
                description = "Notifications, AI settings & more",
                icon = Icons.Default.Tune,
                onClick = { navController.navigate("preferences") }
            )
        }

        SettingsGroup("Data Management") {
            SettingsItem(
                title = "Recycle Bin",
                description = "$deletedRecordsCount items",
                icon = Icons.Default.DeleteOutline,
                onClick = { navController.navigate("recycleBin") }
            )
            SettingsItem(
                title = "Export Data",
                description = if (isExporting) "Exporting..." else "${allRecords.size} records",
                icon = Icons.Filled.FileUpload,
                onClick = {
                    if (!isExporting) {
                        exportLauncher.launch(DataTransferManager.generateExportFileName())
                    }
                },
                trailingContent = if (isExporting) {
                    { CircularProgressIndicator(modifier = Modifier.width(24.dp)) }
                } else null
            )
            SettingsItem(
                title = "Import Data",
                description = if (isImporting) "Reading file..." else "Restore from CSV",
                icon = Icons.Filled.FileDownload,
                onClick = {
                    if (!isImporting) {
                        importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                    }
                },
                trailingContent = if (isImporting) {
                    { CircularProgressIndicator(modifier = Modifier.width(24.dp)) }
                } else null
            )
            SettingsItem(
                title = "Backup to Google Drive",
                description = if (isBackingUpToDrive) "Backing up..." else "Save data to Drive appData",
                icon = Icons.Filled.FileUpload,
                onClick = {
                    if (!isBackingUpToDrive) {
                        isBackingUpToDrive = true
                        authViewModel.backupDatabaseToDrive { result ->
                            isBackingUpToDrive = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Database backed up successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                val exception = result.exceptionOrNull()
                                if (exception is com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                                    intentLauncher.launch(exception.intent)
                                } else {
                                    Toast.makeText(context, "Failed to backup: ${exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                },
                trailingContent = if (isBackingUpToDrive) {
                    { CircularProgressIndicator(modifier = Modifier.width(24.dp)) }
                } else null
            )
            SettingsItem(
                title = "Restore from Google Drive",
                description = if (isRestoringFromDrive) "Restoring..." else "Merge cloud data to device",
                icon = Icons.Filled.FileDownload,
                onClick = {
                    if (!isRestoringFromDrive) {
                        showRestoreConfirm = true
                    }
                },
                trailingContent = if (isRestoringFromDrive) {
                    { CircularProgressIndicator(modifier = Modifier.width(24.dp)) }
                } else null
            )
        }

        SettingsGroup("Study Calendar Sync") {
            SettingsItem(
                title = "Calendar Sync",
                description = "Automated study checkpoints",
                icon = Icons.Default.CalendarMonth,
                onClick = { navController.navigate("calendarSyncSettings") }
            )
        }

        SettingsGroup("AI Assistant") {
            SettingsItem(
                title = "AI Chat",
                description = "Process attendance sheets with AI",
                icon = Icons.Filled.SmartToy,
                onClick = { navController.navigate("aiAssistant") }
            )
        }

        SettingsGroup("Account") {
            val authState by authViewModel.authState.collectAsState()
            
            SettingsItem(
                title = if (authState is com.example.presentmate.ui.viewmodel.AuthState.Authenticated) "Sign Out" else "Sign In",
                description = "Manage your Google account session",
                icon = androidx.compose.material.icons.Icons.Default.Verified,
                onClick = { 
                    if (authState is com.example.presentmate.ui.viewmodel.AuthState.Authenticated) {
                        authViewModel.signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate("login")
                    }
                }
            )
        }

        SettingsGroup("General") {
            SettingsItem(
                title = "App Version",
                description = "v$appVersion — Tap to see what's new",
                icon = Icons.Filled.Info,
                onClick = { navController.navigate("changelog") }
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
                title = "Troubleshoot",
                description = "System diagnostics and permission checks",
                icon = Icons.Filled.Build,
                onClick = { navController.navigate("troubleshootScreen") }
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


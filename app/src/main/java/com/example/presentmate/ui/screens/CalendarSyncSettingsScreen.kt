package com.example.presentmate.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.presentmate.calendar.CalendarInfo
import com.example.presentmate.calendar.CalendarRepository
import com.example.presentmate.data.CalendarSyncPreferences
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.presentmate.worker.CalendarSyncWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarSyncSettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var isEnabled by remember { mutableStateOf(CalendarSyncPreferences.isCalendarSyncEnabled(context)) }
    var selectedCalendarId by remember { mutableStateOf(CalendarSyncPreferences.getSelectedCalendarId(context)) }
    var keywordsList by remember { mutableStateOf(CalendarSyncPreferences.getWhitelistKeywords(context)) }
    var delayMinutes by remember { mutableStateOf(CalendarSyncPreferences.getDelayMinutes(context)) }
    
    var availableCalendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
    var hasPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) 
            == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
             // Load calendars
             scope.launch {
                 try {
                     availableCalendars = CalendarRepository(context).getCalendarList()
                 } catch (e: Exception) {
                     Log.e("CalendarSyncSettings", "Error loading calendars", e)
                 }
             }
        }
    }

    // Load calendars on start if permission granted
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
             try {
                 availableCalendars = CalendarRepository(context).getCalendarList()
             } catch (e: Exception) {
                 Log.e("CalendarSyncSettings", "Error loading calendars", e)
             }
        }
    }

    // Logic to update WorkManager
    fun updateWorkManager(enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
                6, TimeUnit.HOURS // Minimum repeatable interval roughly
            ).build()
            
            workManager.enqueueUniquePeriodicWork(
                "CalendarSyncWorker",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Toast.makeText(context, "Sync enabled (runs every 6 hours)", Toast.LENGTH_SHORT).show()
        } else {
            workManager.cancelUniqueWork("CalendarSyncWorker")
            Toast.makeText(context, "Sync disabled", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Intro Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Sync with Google Calendar to automatically track study sessions. Notifications appear after sessions end.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // --- Master Toggle ---
        SettingsGroup("Status") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnabled) "Sync Enabled" else "Sync Disabled",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { 
                        if (it && !hasPermission) {
                            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        } else {
                            isEnabled = it
                            CalendarSyncPreferences.setCalendarSyncEnabled(context, it)
                            updateWorkManager(it)
                        }
                    }
                )
            }
            if (!hasPermission) {
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text("Grant Calendar Permission")
                }
            }
        }

        // --- Config Section (Only visible if enabled/permission granted) ---
        if (isEnabled && hasPermission) {
            
            // --- Calendar Picker ---
            SettingsGroup("Select Calendar") {
                var expanded by remember { mutableStateOf(false) }
                val selectedCalendarName = availableCalendars.find { it.id == selectedCalendarId }?.displayName 
                    ?: if (selectedCalendarId == -1L) "All Calendars" else "Unknown"

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    OutlinedTextField(
                        value = selectedCalendarName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("Calendar") }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Calendars") },
                            onClick = {
                                selectedCalendarId = -1L
                                CalendarSyncPreferences.setSelectedCalendarId(context, -1L)
                                expanded = false
                            }
                        )
                        availableCalendars.forEach { calendar ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(calendar.displayName)
                                        Text(calendar.accountName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedCalendarId = calendar.id
                                    CalendarSyncPreferences.setSelectedCalendarId(context, calendar.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            // --- Keywords ---
            SettingsGroup("Whitelist Keywords") {
                Text(
                    "Only events containing these words will be tracked:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                
                // Chip Group
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    keywordsList.forEach { keyword ->
                        InputChip(
                            selected = true,
                            onClick = { 
                                val newList = keywordsList - keyword
                                keywordsList = newList
                                CalendarSyncPreferences.setWhitelistKeywords(context, newList)
                            },
                            label = { Text(keyword) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove") }
                        )
                    }
                }
                
                // Add new keyword
                var newKeyword by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newKeyword,
                        onValueChange = { newKeyword = it },
                        label = { Text("Add Keyword") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (newKeyword.isNotBlank() && !keywordsList.contains(newKeyword.trim())) {
                            val newList = keywordsList + newKeyword.trim()
                            keywordsList = newList
                            CalendarSyncPreferences.setWhitelistKeywords(context, newList)
                            newKeyword = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }
            
            // --- Delay ---
            SettingsGroup("Notification Delay") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify $delayMinutes min after event ends")
                    }
                    Slider(
                        value = delayMinutes.toFloat(),
                        onValueChange = { delayMinutes = it.toInt() },
                        onValueChangeFinished = {
                            CalendarSyncPreferences.setDelayMinutes(context, delayMinutes)
                        },
                        valueRange = 1f..15f,
                        steps = 14
                    )
                }
            }
        }
    }
}

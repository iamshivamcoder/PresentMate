package com.example.presentmate.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentmate.viewmodel.StairActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiActivityPromptDialog(
    onDismiss: () -> Unit,
    viewModel: StairActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var editedTime by remember(uiState.suggestedTime) { mutableStateOf(uiState.suggestedTime) }
    var isClockIn by remember(uiState.isClockIn) { mutableStateOf(uiState.isClockIn) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.analyzeActivity()
    }

    if (showTimePicker) {
        val timeParts = editedTime.split(":")
        val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9
        val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 30
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time", style = MaterialTheme.typography.titleMedium) },
            confirmButton = {
                TextButton(onClick = {
                    val hr = String.format("%02d", timePickerState.hour)
                    val min = String.format("%02d", timePickerState.minute)
                    editedTime = "$hr:$min"
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimeInput(state = timePickerState)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Suggestion",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Smart Verification", style = MaterialTheme.typography.titleMedium)
                }
                
                if (!uiState.isLoading && uiState.error == null) {
                    val confidenceColor = when {
                        uiState.confidence >= 80 -> MaterialTheme.colorScheme.primaryContainer
                        uiState.confidence >= 50 -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                    Surface(
                        color = confidenceColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${uiState.confidence}% Match",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = contentColorFor(confidenceColor),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Analyzing your schedule...")
                    }
                } else if (uiState.error != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    // Reason Box
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Historical Schedule Reference Card
                    if (uiState.weekdaySchedule.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "📅 Weekday Pattern",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.weekdaySchedule,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Action Segmented Choice
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Action Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val options = listOf("Clock Out", "Clock In")
                            options.forEachIndexed { index, label ->
                                SegmentedButton(
                                    selected = (isClockIn && index == 1) || (!isClockIn && index == 0),
                                    onClick = { isClockIn = (index == 1) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }

                    // Interactive Time Picker Box
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Log Time", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = editedTime.ifEmpty { "Select Time" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Select Time",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.confirmAction(isClockIn, editedTime) {
                        onDismiss()
                    }
                },
                enabled = !uiState.isLoading
            ) {
                Text("Log Activity")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

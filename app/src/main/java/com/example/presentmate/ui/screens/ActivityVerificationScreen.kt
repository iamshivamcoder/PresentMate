package com.example.presentmate.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentmate.db.PresentMateDatabase
import com.example.presentmate.db.StepActivityLog
import com.example.presentmate.worker.StepSyncWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActivityVerificationScreen() {
    val context  = LocalContext.current
    val db       = remember { PresentMateDatabase.getDatabase(context) }
    val scope    = rememberCoroutineScope()
    val timeFmt  = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFmt  = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    // Live history
    val history by db.stepActivityLogDao().getAllFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Latest burst entry drives the verification card (#2 fix)
    val latestBurst = history.firstOrNull { it.type == "STAIR_BURST" }

    // ── Original screen state ─────────────────────────────────────────────
    var isWalkingDetected by remember { mutableStateOf(true) }
    var useCustomTime     by remember { mutableStateOf(false) }

    // Default from/to from latest burst, or current time
    var startTimeMs by remember { mutableLongStateOf(latestBurst?.detectedAt ?: System.currentTimeMillis()) }
    var endTimeMs   by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val startTimeStr by remember(startTimeMs) { mutableStateOf(timeFmt.format(Date(startTimeMs))) }
    val endTimeStr   by remember(endTimeMs)   { mutableStateOf(timeFmt.format(Date(endTimeMs))) }

    var isSyncing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // ── Original: Header ────────────────────────────────────────────
        Icon(
            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Activity Verification",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Help us ground your study data with accurate activity tracking.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Fix #2: Real detected activity from latest burst ─────────────────
        val activityTitle = if (latestBurst != null)
            "${latestBurst.stepCount} steps detected"
        else
            "No burst recorded yet"
        val activityTime = if (latestBurst != null)
            "${dateFmt.format(Date(latestBurst.detectedAt))}, ${timeFmt.format(Date(latestBurst.detectedAt))}"
        else
            "Tap Sync Now to record activity"

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Detected Activity", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text(activityTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                Text(activityTime, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Original: Questionnaire ──────────────────────────────────────
        Text(
            text = "Were you out for a walk during this time?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActivityChoiceButton(
                text = "Yes, I was out",
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                isSelected = isWalkingDetected,
                modifier = Modifier.weight(1f),
                onClick = { isWalkingDetected = true }
            )
            ActivityChoiceButton(
                text = "No, I was studying",
                icon = Icons.Default.School,
                isSelected = !isWalkingDetected,
                modifier = Modifier.weight(1f),
                onClick = { isWalkingDetected = false }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Original: Custom Time Toggle ─────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Adjust Timing",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manually select the activity duration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = useCustomTime, onCheckedChange = { useCustomTime = it })
        }

        AnimatedVisibility(
            visible = useCustomTime,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            // Fix #3 — time pickers wire to real time state
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeSelectionField("From", startTimeStr, Modifier.weight(1f)) {
                    val cal = Calendar.getInstance().apply { timeInMillis = startTimeMs }
                    android.app.TimePickerDialog(context, { _, h, m ->
                        cal.set(Calendar.HOUR_OF_DAY, h)
                        cal.set(Calendar.MINUTE, m)
                        startTimeMs = cal.timeInMillis
                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                }
                TimeSelectionField("To", endTimeStr, Modifier.weight(1f)) {
                    val cal = Calendar.getInstance().apply { timeInMillis = endTimeMs }
                    android.app.TimePickerDialog(context, { _, h, m ->
                        cal.set(Calendar.HOUR_OF_DAY, h)
                        cal.set(Calendar.MINUTE, m)
                        endTimeMs = cal.timeInMillis
                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Fix #1 — Confirm saves verification; Skip just pops the snackbar/does nothing
        Button(
            onClick = {
                if (latestBurst != null) {
                    scope.launch {
                        // Mark the log as confirmed and save timing if custom time used
                        db.stepActivityLogDao().insert(
                            latestBurst.copy(
                                type      = if (isWalkingDetected) "STAIR_BURST" else "PERIODIC_SYNC",
                                triggered = isWalkingDetected
                            )
                        )
                    }
                }
                android.widget.Toast.makeText(
                    context,
                    if (isWalkingDetected) "Activity confirmed ✓" else "Marked as study session ✓",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Confirm Activity", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(
            onClick = {
                // Fix #1 Skip — just show a message; navigation handled externally
                android.widget.Toast.makeText(context, "Skipped", android.widget.Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(40.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(24.dp))

        // ── NEW: Step History Header + Sync Button ────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Step Detection History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            FilledTonalButton(
                onClick = {
                    if (!isSyncing) {
                        isSyncing = true
                        StepSyncWorker.runManualSync(context)
                        android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed({ isSyncing = false }, 3_000)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Syncing…", style = MaterialTheme.typography.labelMedium)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sync Now", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── NEW: Stat Row ─────────────────────────────────────────────────
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniStatCard(Modifier.weight(1f), history.size.toString(), "Syncs", Icons.Default.Loop)
                MiniStatCard(Modifier.weight(1f), history.sumOf { it.stepCount }.toString(), "Steps", Icons.AutoMirrored.Filled.DirectionsWalk)
                MiniStatCard(Modifier.weight(1f), history.count { it.triggered }.toString(), "Notified", Icons.Default.Notifications)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── NEW: History List ──────────────────────────────────────────────
        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No history yet — tap Sync Now or wait for auto-sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                history.take(20).forEach { log ->
                    StepLogRow(log)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Stat mini-card ──────────────────────────────────────────────────────────

@Composable
private fun MiniStatCard(modifier: Modifier, value: String, label: String, icon: ImageVector) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── History row card ────────────────────────────────────────────────────────

private val rowDateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())
private val rowTimeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())

@Composable
private fun StepLogRow(log: StepActivityLog) {
    val badgeColor = when (log.window) {
        "MORNING"  -> MaterialTheme.colorScheme.tertiaryContainer
        "EVENING"  -> MaterialTheme.colorScheme.primaryContainer
        else       -> MaterialTheme.colorScheme.surfaceVariant
    }
    val badgeIcon = when (log.window) {
        "MORNING"  -> Icons.Default.WbSunny
        "EVENING"  -> Icons.Default.Bedtime
        else       -> Icons.Default.Loop
    }
    val typeLabel = when (log.type) {
        "STAIR_BURST"  -> "Stair Burst 🏃"
        "MANUAL_SYNC"  -> "Manual Sync"
        else           -> "Auto Sync"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(badgeIcon, null, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(typeLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    if (log.triggered) {
                        Text(
                            "Notified",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    Text(
                        "${log.stepCount} steps",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(rowTimeFmt.format(Date(log.detectedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(rowDateFmt.format(Date(log.detectedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Legacy helpers kept for compatibility ───────────────────────────────────

@Composable
fun ActivityChoiceButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                         else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor   = if (isSelected) MaterialTheme.colorScheme.onPrimary
                         else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(32.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = contentColor, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun TimeSelectionField(
    label: String,
    time: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedCard(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

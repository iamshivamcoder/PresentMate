package com.example.presentmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.db.PresentMateDatabase
import com.example.presentmate.ui.components.common.DrumRollerPicker
import com.example.presentmate.worker.ReminderSnoozeScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/** Options for Section 3 – leave/afternoon status */
enum class LeaveStatus { NONE, TAKING_LEAVE, GOING_AFTERNOON }

/**
 * The 9:30 AM reminder dialog with 3 horizontal sections and a bottom Confirm button.
 *
 * Section 1 – Remind me in      (drum-roller: 10 min / 20 min / 30 min / 1 hr)
 * Section 2 – Already started at (drum-roller time picker)
 * Section 3 – Leave status      (chips: none / leave today / will go afternoon)
 */
@Composable
fun SessionReminderDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    // ── Section 1: Snooze ──────────────────────────────────────────────────
    val snoozeLabels = listOf("10 min", "20 min", "30 min", "1 hour")
    val snoozeMinutes = listOf(10, 20, 30, 60)
    var snoozeIndex by remember { mutableIntStateOf(0) }

    // ── Section 2: Already-started time ───────────────────────────────────
    // Build half-hour slots from 5:00 AM to 11:30 AM
    val timeSlots = buildList {
        for (h in 5..11) {
            add(formatTime(h, 0))
            add(formatTime(h, 30))
        }
    }
    var timeIndex by remember { mutableIntStateOf(6) } // default 8:00 AM

    // ── Section 3: Leave status ────────────────────────────────────────────
    var leaveStatus by remember { mutableStateOf(LeaveStatus.NONE) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Title
            Text(
                text = "What's the plan? 📚",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // ── 3 Sections Row ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                // Section 1 – Snooze
                SectionContainer(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                    label = "Remind me in"
                ) {
                    DrumRollerPicker(
                        items = snoozeLabels,
                        selectedIndex = snoozeIndex,
                        onItemSelected = { snoozeIndex = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Section 2 – Already started at
                SectionContainer(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp)) },
                    label = "Already started"
                ) {
                    DrumRollerPicker(
                        items = timeSlots,
                        selectedIndex = timeIndex,
                        onItemSelected = { timeIndex = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Section 3 – Leave status
                SectionContainer(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Default.BeachAccess, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp)) },
                    label = "Today's status"
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LeaveChip(
                            label = "Going afternoon",
                            selected = leaveStatus == LeaveStatus.GOING_AFTERNOON,
                            onClick = {
                                leaveStatus = if (leaveStatus == LeaveStatus.GOING_AFTERNOON)
                                    LeaveStatus.NONE else LeaveStatus.GOING_AFTERNOON
                            }
                        )
                        LeaveChip(
                            label = "Taking leave",
                            selected = leaveStatus == LeaveStatus.TAKING_LEAVE,
                            onClick = {
                                leaveStatus = if (leaveStatus == LeaveStatus.TAKING_LEAVE)
                                    LeaveStatus.NONE else LeaveStatus.TAKING_LEAVE
                            }
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // ── Confirm Button (full width) ───────────────────────────────
            Button(
                onClick = {
                    handleReminderConfirm(
                        context = context,
                        snoozeMinutes = snoozeMinutes[snoozeIndex],
                        alreadyStartedSlot = timeSlots[timeIndex],
                        leaveStatus = leaveStatus,
                        onDismiss = onDismiss
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Confirm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

private fun handleReminderConfirm(
    context: android.content.Context,
    snoozeMinutes: Int,
    alreadyStartedSlot: String,
    leaveStatus: LeaveStatus,
    onDismiss: () -> Unit
) {
    when {
        leaveStatus == LeaveStatus.TAKING_LEAVE -> {
            // Suppress all study notifications for today
            val prefs = context.getSharedPreferences("presentmate_leave_prefs", android.content.Context.MODE_PRIVATE)
            val cal = Calendar.getInstance()
            val todayKey = "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
            prefs.edit().putString("leave_day", todayKey).apply()
            onDismiss()
        }
        leaveStatus == LeaveStatus.GOING_AFTERNOON -> {
            // Snooze to 12:30 PM
            ReminderSnoozeScheduler.scheduleSnooze(context, minutesFromNowUntil(12, 30))
            onDismiss()
        }
        else -> {
            // Section 2 selected: retroactively start session at that time
            val parsedTime = parseTimeSlot(alreadyStartedSlot)
            if (parsedTime != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = PresentMateDatabase.getDatabase(context)
                        val ongoing = db.attendanceDao().getOngoingSession()
                        if (ongoing == null) {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                            }
                            db.attendanceDao().insertRecord(
                                AttendanceRecord(date = cal.timeInMillis, timeIn = parsedTime, timeOut = null)
                            )
                        }
                    } catch (_: Exception) {}
                }
            }
            // Also schedule a snooze reminder regardless
            ReminderSnoozeScheduler.scheduleSnooze(context, snoozeMinutes)
            onDismiss()
        }
    }
}

/** Parse "8:30 AM" style time slot into a timestamp for today */
private fun parseTimeSlot(slot: String): Long? {
    return try {
        val isPm = slot.endsWith("PM")
        val parts = slot.removeSuffix(" AM").removeSuffix(" PM").split(":")
        var hour = parts[0].toInt()
        val minute = parts[1].toInt()
        if (isPm && hour != 12) hour += 12
        if (!isPm && hour == 12) hour = 0
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } catch (_: Exception) { null }
}

/** Minutes from now until a given hour:minute today */
private fun minutesFromNowUntil(hour: Int, minute: Int): Int {
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val diff = (target - System.currentTimeMillis()) / 60_000
    return if (diff > 0) diff.toInt() else 5
}

private fun formatTime(hour: Int, minute: Int): String {
    val ampm = if (hour < 12) "AM" else "PM"
    val h = if (hour % 12 == 0) 12 else hour % 12
    val m = minute.toString().padStart(2, '0')
    return "$h:$m $ampm"
}

// ── Sub-components ─────────────────────────────────────────────────────────

@Composable
private fun SectionContainer(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            icon()
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        content()
    }
}

@Composable
private fun LeaveChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

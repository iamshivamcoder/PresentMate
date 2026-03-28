package com.example.presentmate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.presentmate.db.PresentMateDatabase
import com.example.presentmate.db.StudySessionLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dialog shown after a study session when the user taps "Studied, Done! 📖"
 * Lets them write a brief recall note and optionally adjust the duration.
 */
@Composable
fun StudyRecapDialog(
    logId: Int,
    isPartial: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    var recallNote by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var sessionLog by remember { mutableStateOf<StudySessionLog?>(null) }

    // Load the log from DB
    LaunchedEffect(logId) {
        withContext(Dispatchers.IO) {
            val db = PresentMateDatabase.getDatabase(context)
            sessionLog = db.studySessionLogDao().getById(logId)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Header ──────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (isPartial) "Partial Session Done!" else "Session Complete! 🎉",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        sessionLog?.let { log ->
                            Text(
                                text = log.subject?.let { "${log.subject} · ${log.topic ?: ""}" }
                                    ?: log.eventTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Recall Note ─────────────────────────────────────────────
                Text(
                    text = "What did you study? ✍️",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = recallNote,
                    onValueChange = { recallNote = it },
                    placeholder = {
                        Text(
                            "e.g. Covered Anglo-Carnatic Wars, battle timelines…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() })
                )

                // ── Duration (for partial or optional) ──────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isPartial) "How many minutes did you actually study?" else "Actual duration (optional)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { c -> c.isDigit() }.take(3) },
                    placeholder = { Text("Minutes, e.g. 45") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                    suffix = { Text("min") }
                )

                // ── Action Buttons ───────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Skip")
                    }

                    Button(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                keyboard?.hide()
                                val minutes = durationText.toIntOrNull()
                                val status = when {
                                    isPartial -> "PARTIAL"
                                    else -> "COMPLETED"
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = PresentMateDatabase.getDatabase(context)
                                        val log = db.studySessionLogDao().getById(logId)
                                        if (log != null) {
                                            db.studySessionLogDao().update(
                                                log.copy(
                                                    status = status,
                                                    recallNote = recallNote.trim().ifBlank { null },
                                                    actualDurationMinutes = minutes,
                                                    loggedAt = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                    } finally {
                                        withContext(Dispatchers.Main) { onDismiss() }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save & Done ✅")
                        }
                    }
                }
            }
        }
    }
}

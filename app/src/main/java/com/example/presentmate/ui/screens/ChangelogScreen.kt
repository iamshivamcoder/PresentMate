package com.example.presentmate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ChangelogEntry(
    val version: String,
    val date: String,
    val isLatest: Boolean = false,
    val changes: List<String>
)

private val changelog = listOf(
    ChangelogEntry(
        version = "1.6",
        date = "March 2026",
        isLatest = true,
        changes = listOf(
            "🛠️ Finalized AI Chat: Fixed floaty text bar & model logic (v1.5-flash-latest)",
            "🛠️ Graph Optimization: Clutter-free Monthly/Yearly views",
            "✨ Activity Verification: Step-tracker based study grounding (UI)",
            "✨ Weekly Goal: Added confirmation dialog for goal changes",
            "✨ Notifications: Scheduled custom reminders now listed as cards",
            "✨ Added 'Progress Report' reminder notification preference",
            "✨ Modernized Help Screen with visual cards",
            "✨ Automatic Geofence Tracking toggle fully synced",
            "📋 Removed Whitelist Keywords from Calendar Sync"
        )
    ),
    ChangelogEntry(
        version = "1.4",
        date = "March 2026",
        changes = listOf(
            "✅ Daily 'Started the session?' notification at 9:30 AM (Mon–Sat)",
            "📱 Home screen is now fully scrollable",
            "📅 Calendar sync: Visual schedule section after import",
            "⚙️ New Preferences screen with notification tweaking",
            "🔔 Change reminder time, check permissions, troubleshoot",
            "🤖 AI Assistant: multi-platform API key support (Gemini, OpenRouter, Groq, Cohere)",
            "💬 Floating AI chat button on Home screen",
            "📋 This changelog screen"
        )
    ),
    ChangelogEntry(
        version = "1.3",
        date = "February 2026",
        changes = listOf(
            "🗓️ Google Calendar sync for study sessions",
            "🤖 AI attendance sheet processing with Gemini",
            "📊 Study checkpoint notifications after calendar events",
            "⚡ Auto attendance marking via geofence exit"
        )
    ),
    ChangelogEntry(
        version = "1.2",
        date = "January 2026",
        changes = listOf(
            "📍 Location picker with map support (OpenStreetMap)",
            "🗺️ Improved MapView stability and performance",
            "🔍 Search history for location picker",
            "💾 Saved places for quick geofence setup"
        )
    ),
    ChangelogEntry(
        version = "1.1",
        date = "December 2025",
        changes = listOf(
            "🎉 Initial release of PresentMate",
            "⏱️ Manual attendance tracking (Time In / Time Out)",
            "📊 Attendance overview and statistics",
            "📁 CSV export and import",
            "🗑️ Recycle bin for deleted records",
            "📍 Geofence auto-tracking",
            "🤖 Quick Settings tile for session control"
        )
    )
)

@Composable
fun ChangelogScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.NewReleases,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "What's New",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "PresentMate release history",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        changelog.forEach { entry ->
            ChangelogCard(entry)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ChangelogCard(entry: ChangelogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isLatest)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (entry.isLatest) 4.dp else 0.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "v${entry.version}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (entry.isLatest) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "LATEST",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    entry.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            entry.changes.forEach { change ->
                Text(
                    change,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

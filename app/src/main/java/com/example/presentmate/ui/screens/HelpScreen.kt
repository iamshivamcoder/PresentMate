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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.presentmate.ui.components.common.CollapsibleCard

data class FaqItem(val question: String, val answer: String)

val faqs = listOf(
    FaqItem("Q1: How do I record my attendance?", "A1: Go to the 'Home' screen and tap the 'Time In' button to start a session. Tap 'Time Out' to end it."),
    FaqItem("Q2: Can I edit a past attendance record?", "A2: Yes, on the 'Home' screen, you can see your attendance log. Tap the edit icon next to a record to modify its time in or time out."),
    FaqItem("Q3: What happens when I delete a record?", "A3: Deleted records are moved to the 'Recycle Bin' accessible from the 'Settings' screen. You can restore them or permanently delete them from there."),
    FaqItem("Q4: How can I see my attendance overview?", "A4: Navigate to the 'Overview' screen from the bottom navigation bar. It shows your daily total attendance durations."),
    FaqItem("Q5: How does the Quick Settings Tile work?", "A5: If you add the Present Mate tile to your Quick Settings panel, you can quickly start or stop an attendance session without opening the app."),
    FaqItem("Q6: Where is my data stored?", "A6: Your attendance data is stored locally on your device and is not uploaded to any server.")
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun HelpScreen(_navController: NavHostController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "What's Planned: Feature Explanations",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item {
            PlannedFeaturesSection()
        }
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }
        item {
            Text(
                text = "A Note From the Developer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item {
            DeveloperNoteSection()
        }
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }
        item {
            Text(
                text = "Frequently Asked Questions (FAQ)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(faqs) { faq ->
            CollapsibleCard(
                headerContent = {
                    Text(
                        faq.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                collapsibleContent = {
                    Text(faq.answer, style = MaterialTheme.typography.bodyLarge)
                }
            )
        }
    }
}

@Composable
fun PlannedFeaturesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FeatureExplanationCard(
            title = "Location-Based Session",
            explanation = "Uses your phone's GPS to automatically start and stop your focus sessions when you arrive at or leave your study spot.",
            icon = Icons.Default.LocationOn,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        FeatureExplanationCard(
            title = "Cloud Sync & Backup",
            explanation = "Create a secure cloud backup of your progress and restore it anytime, ensuring your hard-earned data is safe and portable.",
            icon = Icons.Default.CloudSync,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        FeatureExplanationCard(
            title = "AI Chatbot Assistant",
            explanation = "An intelligent AI assistant to help log your activities and answer questions about your productivity patterns.",
            icon = Icons.Default.SmartToy,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        FeatureExplanationCard(
            title = "Interactive Graphs",
            explanation = "Dynamic, visually appealing graphs. Tap, zoom, and interact with your data to explore your progress and identify trends.",
            icon = Icons.Outlined.BarChart,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FeatureExplanationCard(
    title: String,
    explanation: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun DeveloperNoteSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp).align(Alignment.CenterHorizontally)
            )
            
            Text(
                "Hello everyone,\n\nI am a single, independent developer building this app from the ground up in my own time. This is a passion project, and I am the designer, coder, tester, and support team all rolled into one.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            
            Text(
                "Because it's a one-person journey, updates might not be as frequent as those from a big company, and a few bugs might occasionally slip through. Your patience and feedback are what fuel this entire project.",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                "Thank you for being a part of this journey.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

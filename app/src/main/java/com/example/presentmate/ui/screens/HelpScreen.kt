package com.example.presentmate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
        FeatureExplanation(
            title = "Location-Based Session (Automatic)",
            explanation = "This feature will use your phone's GPS to automatically start and stop your focus sessions. For example, when you arrive at your library, the timer begins, and it automatically pauses when you leave, removing the need for manual tracking."
        )
        FeatureExplanation(
            title = "Import/Export Data (for Security)",
            explanation = "You will have full control over your data. This function allows you to create a local backup (export) of your progress and restore it (import) anytime. This ensures your hard-earned data is safe, secure, and portable."
        )
        FeatureExplanation(
            title = "AI Agent (Automatic Session Logging / Chatbot)",
            explanation = "An intelligent AI assistant is planned. It will help automatically log your activities and provide analysis through a conversational chatbot. You can ask it questions about your productivity patterns to get deeper insights into your study habits."
        )
        FeatureExplanation(
            title = "Interactive & Visual Graphs",
            explanation = "This feature will introduce dynamic, visually appealing graphs. You'll be able to tap, zoom, and interact with your data to explore your progress and identify trends in a much more engaging and insightful way than static charts allow."
        )
    }
}

@Composable
fun FeatureExplanation(title: String, explanation: String) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = explanation, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun DeveloperNoteSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Hello everyone,", style = MaterialTheme.typography.bodyLarge)
        Text("I wanted to take a moment to add a personal note and be completely transparent with you. When you look at the app and its future roadmap, you might picture a large team working in a busy office.", style = MaterialTheme.typography.bodyLarge)
        Text("The truth is, there is no large team. There's no corporation, no external funding, and no marketing department.", style = MaterialTheme.typography.bodyLarge)
        Text("There’s just me.", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text("I am a single, independent developer building this app from the ground up in my own time. This is a passion project, and I am the designer, the coder, the tester, and the support team, all rolled into one. I pour my spare hours into this, fueled by the belief that I can create something genuinely helpful for this community.", style = MaterialTheme.typography.bodyLarge)
        Text("Because it's a one-person journey, updates might not be as frequent as those from a big company. A few bugs might occasionally slip through, despite my best efforts to catch them all. I know this can be frustrating when you're trying to be productive.", style = MaterialTheme.typography.bodyLarge)
        Text("All I ask is for a little patience and understanding. This app is a work in progress, and I am learning as I go. So, if you find a bug or feel a feature is missing, please don't hold a grudge. Instead, please send me your constructive feedback. Every bug report and every suggestion doesn't go to a faceless ticketing system; it comes directly to me.", style = MaterialTheme.typography.bodyLarge)
        Text("Your support and feedback aren't just data; they are what fuel this entire project. Thank you for being a part of this journey.", style = MaterialTheme.typography.bodyLarge)
    }
}

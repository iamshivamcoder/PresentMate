package com.example.presentmate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

data class FaqItem(val question: String, val answer: String)

val faqs = listOf(
    FaqItem("Q1: How do I record my attendance?", "A1: Go to the 'Home' screen and tap the 'Time In' button to start a session. Tap 'Time Out' to end it."),
    FaqItem("Q2: Can I edit a past attendance record?", "A2: Yes, on the 'Home' screen, you can see your attendance log. Tap the edit icon next to a record to modify its time in or time out."),
    FaqItem("Q3: What happens when I delete a record?", "A3: Deleted records are moved to the 'Recycle Bin' accessible from the 'Settings' screen. You can restore them or permanently delete them from there."),
    FaqItem("Q4: How can I see my attendance overview?", "A4: Navigate to the 'Overview' screen from the bottom navigation bar. It shows your daily total attendance durations."),
    FaqItem("Q5: How does the Quick Settings Tile work?", "A5: If you add the Present Mate tile to your Quick Settings panel, you can quickly start or stop an attendance session without opening the app."),
    FaqItem("Q6: Where is my data stored?", "A6: Your attendance data is stored locally on your device and is not uploaded to any server.")
    // Add more FAQs as needed
)

@Composable
fun HelpScreen(navController: NavHostController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // Spacing between cards
    ) {
        item {
            Text(
                text = "Frequently Asked Questions (FAQ)",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(faqs) { faq ->
            CollapsibleCard(
                headerContent = {
                    Text(faq.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                },
                collapsibleContent = {
                    Text(faq.answer, style = MaterialTheme.typography.bodyLarge)
                }
            )
        }
    }
}

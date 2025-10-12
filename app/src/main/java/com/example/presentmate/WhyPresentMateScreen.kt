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

data class QnaItem(val question: String, val answer: String)

val qnaList = listOf(
    QnaItem("1. What is Present Mate?", "Present Mate is a mobile application designed to help users track their attendance effortlessly and gain insights into their presence over time."),
    QnaItem("2. Who is Present Mate for?", "It\'s for students, professionals, or anyone who needs to keep a personal record of their attendance for classes, work, meetings, or other commitments."),
    QnaItem("3. Why was Present Mate created?", "To provide a simple, user-friendly, and private way to manage attendance records without complex setups or data sharing concerns."),
    QnaItem("4. Is Present Mate free?", "Yes, Present Mate is currently offered as a free application."),
    QnaItem("5. Does Present Mate require an internet connection?", "No, core functionalities like recording and viewing attendance work completely offline. Data is stored locally."),
    QnaItem("6. How does the 'Time In/Time Out' feature work?", "Simply tap 'Time In' when you arrive and 'Time Out' when you leave. The app records these timestamps."),
    QnaItem("7. Can I manually add or edit past records?", "Yes, you can edit the time in/out for existing records directly from the attendance log on the main screen."),
    QnaItem("8. What is the 'Overview' screen for?", "The Overview screen provides a summary of your attendance, showing total hours/minutes logged per day."),
    QnaItem("9. How does the Recycle Bin work?", "When you delete an attendance record, it\'s moved to the Recycle Bin. You can restore it or delete it permanently from there."),
    QnaItem("10. Is my attendance data private?", "Absolutely. All attendance data is stored locally on your device only. We do not collect or store your personal attendance information on any servers."),
    QnaItem("11. What is the Quick Settings Tile?", "It\'s a shortcut for your phone\'s Quick Settings panel that lets you quickly 'Time In' or 'Time Out' without opening the app fully."),
    QnaItem("12. How accurate is the time tracking?", "The app uses your device\'s system time, so it\'s as accurate as your phone\'s clock."),
    QnaItem("13. Can I export my attendance data?", "Yes, you can export your attendance data to a .doc file and import it back from the Settings screen. This is useful for backups or transferring data."),
    QnaItem("14. What platforms does Present Mate support?", "Present Mate is an Android application."),
    QnaItem("15. How can I provide feedback or report a bug?", "We appreciate feedback! For now, please consider reaching out through the platform where you downloaded the app (if a feedback mechanism is provided there)."),
    QnaItem("16. Will there be more features in the future?", "Yes, we aim to continuously improve Present Mate and add new useful features based on user needs and feedback."),
    QnaItem("17. Why the name 'Present Mate'?", "It signifies being your reliable companion (mate) for keeping track of when you are present."),
    QnaItem("18. Does the app send notifications?", "Currently, Present Mate does not send proactive notifications for timing in or out, but the Quick Settings tile provides a visual reminder of your current session status."),
    QnaItem("19. What if I forget to 'Time Out'?", "You can edit the record later from the attendance log to input the correct 'Time Out'."),
    QnaItem("20. How is Present Mate different from other trackers?", "Present Mate focuses on simplicity, privacy (offline-first), and a clean user experience for personal attendance tracking without unnecessary bells and whistles.")
)

@Composable
fun WhyPresentMateScreen(navController: NavHostController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // Spacing between cards
    ) {
        item {
            Text(
                text = "About Present Mate: Your Questions Answered",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(qnaList) { qna ->
            CollapsibleCard(
                headerContent = {
                    Text(qna.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                },
                collapsibleContent = {
                    Text(qna.answer, style = MaterialTheme.typography.bodyLarge)
                }
            )
        }
    }
}

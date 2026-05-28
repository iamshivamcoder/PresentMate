package com.example.presentmate

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.presentmate.data.CalendarSyncPreferences
import com.example.presentmate.db.PresentMateDatabase
import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.ui.screens.*
import com.example.presentmate.ui.viewmodel.AuthViewModel
import com.example.presentmate.ui.viewmodel.AuthState
import com.example.presentmate.viewmodel.AIAssistantViewModel
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], manifest = Config.NONE, application = android.app.Application::class)
class PresentMateUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAIAssistantScreen_InputBarIsVisible() {
        // Mock dependencies for ViewModel to avoid hiltViewModel() lookup crash in unit tests
        val attendanceDao = mockk<AttendanceDao>(relaxed = true)
        val mockViewModel = AIAssistantViewModel(attendanceDao, ApplicationProvider.getApplicationContext())
        
        composeTestRule.setContent {
            AIAssistantScreen(viewModel = mockViewModel)
        }
        
        // Find the input field
        composeTestRule.onNodeWithText("Ask AI Assistant...").assertExists()
        // Find the send button
        composeTestRule.onNodeWithContentDescription("Send").assertExists()
    }

    @Test
    fun testOverviewScreen_TogglesGraphs() {
        mockkStatic(FirebaseAuth::class)
        val mockAuth = mockk<FirebaseAuth>(relaxed = true) {
            every { currentUser } returns null
        }
        every { FirebaseAuth.getInstance() } returns mockAuth

        val mockAttendanceDao = mockk<AttendanceDao>(relaxed = true) {
            every { getAllDeletedRecords() } returns flowOf(emptyList())
            every { getAllRecords() } returns flowOf(emptyList())
        }
        val mockDb = mockk<PresentMateDatabase>(relaxed = true) {
            every { attendanceDao() } returns mockAttendanceDao
        }
        val mockAuthViewModel = mockk<AuthViewModel>(relaxed = true) {
            every { authState } returns MutableStateFlow(AuthState.Idle)
        }
        
        composeTestRule.setContent {
            val navController = rememberNavController()
            SettingsScreen(
                navController = navController,
                db = mockDb,
                authViewModel = mockAuthViewModel
            )
        }
        
        composeTestRule.onAllNodesWithText("Preferences").onFirst().assertExists()
        composeTestRule.onNodeWithText("Recycle Bin").assertExists()
        composeTestRule.onNodeWithText("Export Data").assertExists()
        composeTestRule.onNodeWithText("Import Data").assertExists()
    }

    @Test
    fun testHelpScreen_DisplaysPlannedFeaturesCards() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            HelpScreen(_navController = navController)
        }
        
        composeTestRule.onNodeWithText("What's Planned: Feature Explanations").assertExists()
        
        // Assuming HelpScreen has sections. The new visual cards:
        composeTestRule.onNodeWithText("Location-Based Session").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cloud Sync & Backup").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI Chatbot Assistant").assertIsDisplayed()
    }

    @Test
    fun testChangelogScreen_DisplaysVersion1_6() {
        composeTestRule.setContent {
            ChangelogScreen()
        }
        
        composeTestRule.onNodeWithText("What's New").assertExists()
        composeTestRule.onNodeWithText("v1.6").assertExists()
        composeTestRule.onNodeWithText("LATEST").assertExists()
    }

    @Test
    fun testNotificationPreferencesScreen_TogglesExist() {
        composeTestRule.setContent {
            NotificationPreferencesScreen()
        }
        
        composeTestRule.onNodeWithText("Daily Reminder").assertExists()
        composeTestRule.onNodeWithText("Change Time").assertExists()
        composeTestRule.onNodeWithText("Progress Reports").assertExists()
        composeTestRule.onNodeWithText("Notify on event completions").assertExists()
    }

    @Test
    fun testCalendarSyncSettingsScreen_BasicElementsExist() {
        mockkObject(CalendarSyncPreferences)
        every { CalendarSyncPreferences.isCalendarSyncEnabled(any()) } returns true
        every { CalendarSyncPreferences.getDelayMinutes(any()) } returns 5
        every { CalendarSyncPreferences.getSelectedCalendarId(any()) } returns -1L

        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), Manifest.permission.READ_CALENDAR) } returns PackageManager.PERMISSION_GRANTED

        composeTestRule.setContent {
            CalendarSyncSettingsScreen()
        }
        
        composeTestRule.onNodeWithText("Sync with Google Calendar to automatically track study sessions. Notifications appear after sessions end.").assertExists()
        composeTestRule.onNodeWithText("Notification Delay").assertExists()
        composeTestRule.onNodeWithText("Auto-Sync Frequency").assertExists()
        
        // Whitelist keywords should NOT exist
        composeTestRule.onNodeWithText("Whitelist Keywords").assertDoesNotExist()
    }

    @org.junit.After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testWhyPresentMateScreen_Exists() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            WhyPresentMateScreen(_navController = navController)
        }
        
        composeTestRule.onNodeWithText("About Present Mate: Your Questions Answered").assertExists()
        composeTestRule.onNodeWithText("1. What is Present Mate?").assertExists()
    }
    
    @Test
    fun testAboutDeveloperScreen_Exists() {
        composeTestRule.setContent {
            AboutDeveloperScreen()
        }
        
        composeTestRule.onNodeWithText("Shivam").assertExists()
    }

    @Test
    fun testThemePreferencesScreen_Exists() {
        composeTestRule.setContent {
            ThemePreferencesScreen()
        }
        
        composeTestRule.onNodeWithText("App Theme").assertExists()
        composeTestRule.onNodeWithText("Select Theme Mode").assertExists()
        composeTestRule.onNodeWithText("System Default").assertExists()
        composeTestRule.onNodeWithText("Light Mode").assertExists()
        composeTestRule.onNodeWithText("Dark Mode").assertExists()
    }
}

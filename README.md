# PresentMate

PresentMate is an Android application designed to help you effortlessly track your attendance for classes, work, meetings, or any other commitments. 
It focuses on simplicity, privacy (with offline-first data storage), and a clean user experience.

## Key Features

*   **Effortless Attendance Tracking:** Easily record 'Time In' and 'Time Out' with a simple tap.
*   **Manual Record Management:** Add or edit past attendance records as needed directly from the log.
*   **Attendance Overview:** Get a summary of your attendance, showing total hours/minutes logged.
*   **Recycle Bin:** Deleted records are moved to a recycle bin, allowing for restoration or permanent deletion.
*   **Quick Settings Tile:** Log 'Time In/Out' quickly via a shortcut in your phone's Quick Settings panel without opening the app.
*   **Automatic Session Tracking with Geofencing:** Automatically starts and stops study sessions when you enter and exit a predefined location (e.g., a library). This feature is power-efficient, works in the background, and ensures hands-free, accurate attendance logging.
*   **Centralized Location Hub:** A dedicated 'Location' tab that groups all location-related functionalities, including the automatic location picker, geofence management, and real-time location status.
*   **Data Export & Import:** Backup your attendance data to a `.doc` file and restore it when needed, accessible via the Settings screen.
*   **Privacy Focused:** All attendance data is stored locally on your device. No data is collected or stored on external servers.
*   **Offline Functionality:** Core features work completely offline.

## Tech Stack

*   **Kotlin:** Primary programming language.
*   **Jetpack Compose:** For building the user interface.
*   **Room Persistence Library:** For local data storage (attendance records).
*   **Hilt:** For dependency injection (as inferred from build files, though not explicitly seen in the 5 files read for this specific task).
*   **Coroutines:** For managing background tasks and asynchronous operations.
*   **Apache POI:** Used for exporting and importing data to/from `.doc` files.
*   **Google Play Services - Location:** Used for the geofencing feature.

## Setup

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Build and run the application on an Android device or emulator.

## How to Use

1.  **Basic Attendance Tracking:**
    *   Use the main screen to tap 'Time In' when you start an activity.
    *   Tap 'Time Out' when you finish.
    *   Your attendance records are automatically saved.

2.  **Managing Records:**
    *   Go to the 'Overview' tab to see a summary of your attendance.
    *   You can manually add, edit, or delete records from the log.
    *   Deleted records go to the 'Recycle Bin', found in the Settings screen, where you can restore or permanently delete them.

3.  **Location-Based Features:**
    *   Navigate to the 'Location' tab to access all location-related features.
    *   **Automatic Session Tracking:** Set up a geofence around a location (like your workplace or university). The app will automatically start and stop a session when you enter and leave the area.
    *   **Location Status:** View your real-time position relative to the defined geofence to ensure you are within the designated area.
    *   **Location Picker:** Manually select or adjust locations on a map.

4.  **Quick Access:**
    *   Add the **Quick Settings Tile** to your device's notification shade for instant 'Time In' and 'Time Out' logging without opening the app.

5.  **Data Management:**
    *   In the 'Settings' screen, you can find options to **Export** your data to a `.doc` file for backup or sharing.
    *   Use the **Import** feature to restore your data from a previously exported file.

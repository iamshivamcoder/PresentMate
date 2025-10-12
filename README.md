# PresentMate

PresentMate is an Android application designed to help you effortlessly track your attendance for classes, work, meetings, or any other commitments. 
It focuses on simplicity, privacy (with offline-first data storage), and a clean user experience.

## Key Features

*   **Effortless Attendance Tracking:** Easily record 'Time In' and 'Time Out' with a simple tap.
*   **Manual Record Management:** Add or edit past attendance records as needed directly from the log.
*   **Attendance Overview:** Get a summary of your attendance, showing total hours/minutes logged.
*   **Recycle Bin:** Deleted records are moved to a recycle bin, allowing for restoration or permanent deletion.
*   **Quick Settings Tile:** Log 'Time In/Out' quickly via a shortcut in your phone's Quick Settings panel without opening the app.
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

## Setup

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Build and run the application on an Android device or emulator.

## How to Use

Use the main screen to log your 'Time In' and 'Time Out'. Edit records as needed. View summaries in the 'Overview' section.
Export and import your data via the options available in the Settings screen. Utilize the Quick Settings Tile for even faster logging.

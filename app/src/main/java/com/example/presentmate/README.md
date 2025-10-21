
# Location Picker Screen

The `LocationPickerActivity` provides a user interface for selecting a location on a map. This screen is designed to be launched from another activity and returns the selected geographical coordinates (latitude and longitude) to the calling activity.

## Features

- **Interactive Map:** Displays an interactive map using osmdroid, allowing users to pan and zoom to find a specific location.
- **Current Location:** On launch, it attempts to fetch the user's current location and centers the map on it.
- **Search Functionality:** A search bar at the top allows users to search for locations by name or address.
- **Search History and Suggestions:** The search functionality includes suggestions based on typing and a history of recent searches.
- **Confirm Location:** A "Confirm Location" button at the bottom of the screen allows the user to finalize their selection.
- **Address Display:** The address corresponding to the selected map center is displayed above the confirm button.

## How It Works

1.  **Initialization:** The activity initializes the osmdroid configuration and the `SearchHistoryRepository`. It then requests the user's current location.
2.  **UI State:** While the location is being fetched, a loading indicator is displayed. Once the initial location is available, the `LocationPickerScreen` composable is displayed.
3.  **User Interaction:**
    *   The user can pan and zoom the map to select a location. The address is updated in real-time.
    *   The user can use the search bar to find a location. Selecting a search result will move the map to that location.
4.  **Location Confirmation:** When the user clicks the "Confirm Location" button, the latitude and longitude of the selected `GeoPoint` are put into an `Intent` as extras.
5.  **Returning a Result:** The activity then finishes and returns `RESULT_OK` along with the `Intent` containing the location data to the calling activity.

## Composables

-   **`LocationPickerActivity`:** The main entry point for the screen. It handles location permissions, fetching the initial location, and setting up the UI.
-   **`LocationPickerScreen`:** The main composable for the screen, which handles the overall layout and state management.
-   **`LocationPickerContent`:**  This composable contains the core UI components: the search bar, the map, and the confirmation button.
-   **`MapViewContainer`:** A wrapper for the osmdroid `MapView`, handling its lifecycle and user interactions.
-   **`LocationSearchBar`:** The search bar component with integrated search and clear functionality.
-   **`SearchOverlay`:** Displays search suggestions and history.
-   **`CenterPin`:** A pin fixed at the center of the map to indicate the selected point.
-   **`SelectedAddressCard`:** Displays the address of the selected location.
-   **`ConfirmLocationButton`:** The button to confirm the location selection.

## Usage

To use this activity, launch it using `registerForActivityResult` and handle the returned location data in the callback.

```kotlin
val locationPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val latitude = result.data?.getDoubleExtra("latitude", 0.0)
        val longitude = result.data?.getDoubleExtra("longitude", 0.0)
        // Use the returned latitude and longitude
    }
}

fun launchLocationPicker() {
    val intent = Intent(this, LocationPickerActivity::class.java)
    locationPickerLauncher.launch(intent)
}
```

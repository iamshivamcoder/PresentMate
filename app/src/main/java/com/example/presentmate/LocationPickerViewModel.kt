package com.example.presentmate

import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.presentmate.data.SavedPlace
import com.example.presentmate.data.SavedPlacesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.util.GeoPoint
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Represents the UI state for the location picker screen.
 */
data class LocationPickerUiState(
    val selectedLocation: GeoPoint? = null,
    val addressText: String = "Move the map to select a location",
    val searchQuery: String = "",
    val suggestions: List<Address> = emptyList(),
    val history: List<String> = emptyList(),
    val savedPlaces: List<SavedPlace> = emptyList(),
    val isSearching: Boolean = false,
    val isSearchFocused: Boolean = false,
    val isMapMoving: Boolean = false,
    val isInitializing: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel for the location picker screen.
 *
 * @param geocoder The geocoder to use for reverse geocoding.
 * @param searchHistoryRepository The repository for search history.
 * @param savedPlacesRepository The repository for saved places.
 * @param initialLocation The initial location to display on the map.
 */
class LocationPickerViewModel(
    private val geocoder: Geocoder,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val savedPlacesRepository: SavedPlacesRepository,
    val initialLocation: GeoPoint?
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationPickerUiState())
    val uiState = _uiState.asStateFlow()

    private val searchCache = mutableMapOf<String, List<Address>>()
    private val addressCache = mutableMapOf<GeoPoint, String>()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    history = searchHistoryRepository.getSearchHistory()
                )
            }
            savedPlacesRepository.getAll().collect { savedPlaces ->
                _uiState.update { it.copy(savedPlaces = savedPlaces) }
            }
        }

        viewModelScope.launch {
            val startingLocation = initialLocation ?: GeoPoint(20.5937, 78.9629) // Default to India
            onMapMove(startingLocation)
            val address = getAddressText(startingLocation)
            _uiState.update { it.copy(addressText = address, isInitializing = false, isMapMoving = false) }
        }
    }

    /**
     * Called when the search query changes.
     *
     * @param query The new search query.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isEmpty()) {
            _uiState.update {
                it.copy(
                    suggestions = emptyList(),
                    history = searchHistoryRepository.getSearchHistory()
                )
            }
        } else {
            searchJob = viewModelScope.launch {
                delay(300) // Debounce
                onPerformSearch(query)
            }
        }
    }

    /**
     * Called when the search bar focus changes.
     *
     * @param isFocused Whether the search bar is focused.
     */
    fun onSearchFocusChanged(isFocused: Boolean) {
        _uiState.update { it.copy(isSearchFocused = isFocused) }
        if (isFocused) {
            _uiState.update { it.copy(history = searchHistoryRepository.getSearchHistory()) }
        }
    }

    /**
     * Performs a search for the given query.
     *
     * @param query The query to search for.
     */
    fun onPerformSearch(query: String = _uiState.value.searchQuery) {
        if (query.trim().isEmpty()) return

        addSearchToHistory(query)

        if (searchCache.containsKey(query)) {
            _uiState.update { it.copy(suggestions = searchCache.getValue(query)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            try {
                val addresses = suspendCancellableCoroutine<List<Address>?> { continuation ->
                    if (Build.VERSION.SDK_INT >= 33) {
                        geocoder.getFromLocationName(query, 5) { addressList ->
                            continuation.resume(addressList)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        continuation.resume(geocoder.getFromLocationName(query, 5))
                    }
                }

                if (addresses?.isNotEmpty() == true) {
                    searchCache[query] = addresses
                    _uiState.update { it.copy(suggestions = addresses) }
                } else {
                    _uiState.update {
                        it.copy(
                            suggestions = emptyList(),
                            error = "No results found for \"$query\""
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LocationPickerVM", "Search failed", e)
                _uiState.update {
                    it.copy(
                        suggestions = emptyList(),
                        error = "Search failed: ${e.message}"
                    )
                }
            } finally {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    /**
     * Called when the map is moved.
     *
     * @param geoPoint The new center of the map.
     */
    fun onMapMove(geoPoint: GeoPoint) {
        _uiState.update { it.copy(selectedLocation = geoPoint, isMapMoving = true) }
    }

    /**
     * Called when the map movement is finished.
     */
    fun onMapMoveFinished() {
        viewModelScope.launch {
            _uiState.value.selectedLocation?.let { geoPoint ->
                val address = getAddressText(geoPoint)
                _uiState.update { it.copy(addressText = address, isMapMoving = false) }
            }
        }
    }

    /**
     * Called when a search suggestion is clicked.
     *
     * @param address The selected address.
     */
    fun onSuggestionClicked(address: Address) {
        val newLocation = GeoPoint(address.latitude, address.longitude)
        val addressText = address.getAddressLine(0) ?: ""
        _uiState.update {
            it.copy(
                selectedLocation = newLocation,
                searchQuery = addressText,
                isSearchFocused = false,
                suggestions = emptyList()
            )
        }
        addSearchToHistory(addressText)
        onMapMoveFinished() // Update address text
    }

    /**
     * Called when a search history item is clicked.
     *
     * @param query The selected query.
     */
    fun onHistoryItemClicked(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        onPerformSearch(query)
    }

    /**
     * Called when a saved place is clicked.
     *
     * @param place The selected place.
     */
    fun onSavedPlaceClicked(place: SavedPlace) {
        val newLocation = GeoPoint(place.latitude, place.longitude)
        _uiState.update {
            it.copy(
                selectedLocation = newLocation,
                searchQuery = place.address,
                isSearchFocused = false
            )
        }
        onMapMoveFinished() // Update address text
    }

    /**
     * Clears the search query.
     */
    fun onClearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                suggestions = emptyList()
            )
        }
    }

    /**
     * Removes a query from the search history.
     *
     * @param query The query to remove.
     */
    fun onRemoveFromHistory(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.removeFromSearchHistory(query)
            _uiState.update { it.copy(history = searchHistoryRepository.getSearchHistory()) }
        }
    }

    /**
     * Adds a query to the search history.
     *
     * @param query The query to add.
     */
    private fun addSearchToHistory(query: String) {
        if (query.isNotEmpty()) {
            searchHistoryRepository.addToSearchHistory(query)
        }
    }

    /**
     * Clears the error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Gets the address text for a given GeoPoint.
     *
     * @param geoPoint The GeoPoint to get the address for.
     * @return The address text.
     */
    private suspend fun getAddressText(geoPoint: GeoPoint): String {
        val roundedLat = String.format(Locale.US, "%.6f", geoPoint.latitude).toDouble()
        val roundedLon = String.format(Locale.US, "%.6f", geoPoint.longitude).toDouble()
        val cacheKey = GeoPoint(roundedLat, roundedLon)

        addressCache[cacheKey]?.let { return it }

        val newAddress = try {
            suspendCancellableCoroutine<String> { continuation ->
                if (Build.VERSION.SDK_INT >= 33) {
                    geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()?.getAddressLine(0)
                            ?: String.format(
                                Locale.US,
                                "Lat: %.6f, Lng: %.6f",
                                geoPoint.latitude,
                                geoPoint.longitude
                            )
                        continuation.resume(address)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses =
                        geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                    val address = addresses?.firstOrNull()?.getAddressLine(0)
                        ?: String.format(
                            Locale.US,
                            "Lat: %.6f, Lng: %.6f",
                            geoPoint.latitude,
                            geoPoint.longitude
                        )
                    continuation.resume(address)
                }
            }
        } catch (e: Exception) {
            Log.e("LocationPickerVM", "Get address failed", e)
            _uiState.update { it.copy(error = "Failed to get address: ${e.message}") }
            return String.format(
                Locale.US,
                "Lat: %.6f, Lng: %.6f",
                geoPoint.latitude,
                geoPoint.longitude
            )
        }

        addressCache[cacheKey] = newAddress
        return newAddress
    }

    companion object {
        fun provideFactory(
            geocoder: Geocoder,
            searchHistoryRepository: SearchHistoryRepository,
            savedPlacesRepository: SavedPlacesRepository, // Add this
            initialLocation: GeoPoint?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LocationPickerViewModel(
                    geocoder,
                    searchHistoryRepository,
                    savedPlacesRepository, // Pass this
                    initialLocation
                ) as T
            }
        }
    }
}

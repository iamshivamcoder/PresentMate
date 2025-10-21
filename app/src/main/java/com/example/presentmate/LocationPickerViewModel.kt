package com.example.presentmate

import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.util.GeoPoint
import java.util.Locale
import kotlin.coroutines.resume

data class LocationPickerUiState(
    val selectedLocation: GeoPoint? = null,
    val addressText: String = "Move the map to select a location",
    val searchQuery: String = "",
    val suggestions: List<Address> = emptyList(),
    val history: List<String> = emptyList(),
    val savedPlaces: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val isSearchFocused: Boolean = false,
    val isMapMoving: Boolean = false,
    val error: String? = null
)

class LocationPickerViewModel(
    private val geocoder: Geocoder,
    private val searchHistoryRepository: SearchHistoryRepository,
    val initialLocation: GeoPoint?
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationPickerUiState())
    val uiState = _uiState.asStateFlow()

    private val searchCache = mutableMapOf<String, List<Address>>()
    private var searchJob: Job? = null

    init {
        _uiState.update {
            it.copy(
                history = searchHistoryRepository.getSearchHistory(),
                savedPlaces = listOf("Home", "Work") // Placeholder
            )
        }
        initialLocation?.let {
            onMapMove(it)
            onMapMoveFinished()
        }
    }

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

    fun onSearchFocusChanged(isFocused: Boolean) {
        _uiState.update { it.copy(isSearchFocused = isFocused) }
        if (isFocused) {
            _uiState.update { it.copy(history = searchHistoryRepository.getSearchHistory()) }
        }
    }

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

    fun onMapMove(geoPoint: GeoPoint) {
        _uiState.update { it.copy(selectedLocation = geoPoint, isMapMoving = true) }
    }

    fun onMapMoveFinished() {
        viewModelScope.launch {
            _uiState.value.selectedLocation?.let { geoPoint ->
                val address = getAddressText(geoPoint)
                _uiState.update { it.copy(addressText = address, isMapMoving = false) }
            }
        }
    }

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

    fun onHistoryItemClicked(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        onPerformSearch(query)
    }

    fun onSavedPlaceClicked(place: String) {
        _uiState.update { it.copy(searchQuery = place) }
        onPerformSearch(place)
    }

    fun onClearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                suggestions = emptyList()
            )
        }
    }

    fun onRemoveFromHistory(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.removeFromSearchHistory(query)
            _uiState.update { it.copy(history = searchHistoryRepository.getSearchHistory()) }
        }
    }

    private fun addSearchToHistory(query: String) {
        if (query.isNotEmpty()) {
            searchHistoryRepository.addToSearchHistory(query)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun getAddressText(geoPoint: GeoPoint): String {
        return try {
            suspendCancellableCoroutine { continuation ->
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
    }

    companion object {
        fun provideFactory(
            geocoder: Geocoder,
            searchHistoryRepository: SearchHistoryRepository,
            initialLocation: GeoPoint?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LocationPickerViewModel(
                    geocoder,
                    searchHistoryRepository,
                    initialLocation
                ) as T
            }
        }
    }
}

package com.example.presentmate

import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.util.GeoPoint
import java.util.Locale
import kotlin.coroutines.resume

// This data class will hold all your UI state
data class LocationPickerUiState(
    val selectedLocation: GeoPoint? = null,
    val addressText: String = "Tap on map to select location",
    val searchQuery: String = "",
    val suggestions: List<Address> = emptyList(),
    val history: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val showSuggestions: Boolean = false
)

class LocationPickerViewModel(
    private val geocoder: Geocoder,
    private val searchHistoryRepository: SearchHistoryRepository,
    initialLocation: GeoPoint?
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationPickerUiState())
    val uiState = _uiState.asStateFlow()

    // In-memory cache survives configuration changes
    private val searchCache = mutableMapOf<String, List<Address>>()

    init {
        // Set initial location if provided
        initialLocation?.let {
            onLocationSelected(it)
        }
        // Load initial search history
        _uiState.update { it.copy(history = searchHistoryRepository.getSearchHistory()) }
    }

    // --- Event Handlers ---

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isEmpty()) {
            _uiState.update { it.copy(
                showSuggestions = false,
                suggestions = emptyList(),
                history = searchHistoryRepository.getSearchHistory()
            )}
        }
    }

    fun onPerformSearch() {
        val query = _uiState.value.searchQuery
        if (query.trim().isEmpty()) return

        if (searchCache.containsKey(query)) {
            _uiState.update { it.copy(
                suggestions = searchCache.getValue(query),
                showSuggestions = true
            )}
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
                    _uiState.update { it.copy(
                        suggestions = addresses,
                        showSuggestions = true
                    )}
                } else {
                    _uiState.update { it.copy(
                        suggestions = emptyList(),
                        showSuggestions = false
                    )}
                }
            } catch (e: Exception) {
                Log.e("LocationPickerVM", "Search failed", e)
                _uiState.update { it.copy(
                    suggestions = emptyList(),
                    showSuggestions = false
                )}
            } finally {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    fun onLocationSelected(geoPoint: GeoPoint) {
        _uiState.update { it.copy(selectedLocation = geoPoint) }
        viewModelScope.launch {
            val address = getAddressText(geoPoint)
            _uiState.update { it.copy(addressText = address) }
        }
    }

    fun onSuggestionClicked(address: Address) {
        val newLocation = GeoPoint(address.latitude, address.longitude)
        onLocationSelected(newLocation) // This will update addressText
        _uiState.update { it.copy(
            searchQuery = address.getAddressLine(0) ?: "",
            showSuggestions = false,
            suggestions = emptyList()
        )}
    }

    fun onHistoryItemClicked(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        onPerformSearch()
    }

    fun onClearSearch() {
        _uiState.update { it.copy(
            searchQuery = "",
            showSuggestions = false,
            suggestions = emptyList()
        )}
    }

    fun onRemoveFromHistory(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.removeFromSearchHistory(query)
            _uiState.update { it.copy(
                history = searchHistoryRepository.getSearchHistory()
            )}
        }
    }

    fun addSearchToHistory() {
        if (_uiState.value.searchQuery.isNotEmpty()) {
            searchHistoryRepository.addToSearchHistory(_uiState.value.searchQuery)
        }
    }

    // --- Private suspend function ---

    private suspend fun getAddressText(geoPoint: GeoPoint): String {
        return try {
            suspendCancellableCoroutine { continuation ->
                if (Build.VERSION.SDK_INT >= 33) {
                    geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()?.getAddressLine(0)
                            ?: String.format(Locale.US, "Lat: %.6f, Lng: %.6f", geoPoint.latitude, geoPoint.longitude)
                        continuation.resume(address)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                    val address = addresses?.firstOrNull()?.getAddressLine(0)
                        ?: String.format(Locale.US, "Lat: %.6f, Lng: %.6f", geoPoint.latitude, geoPoint.longitude)
                    continuation.resume(address)
                }
            }
        } catch (e: Exception) {
            Log.e("LocationPickerVM", "Get address failed", e)
            String.format(Locale.US, "Lat: %.6f, Lng: %.6f", geoPoint.latitude, geoPoint.longitude)
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
                return LocationPickerViewModel(geocoder, searchHistoryRepository, initialLocation) as T
            }
        }
    }
}
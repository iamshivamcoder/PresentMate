package com.example.presentmate

import android.location.Geocoder
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.util.GeoPoint
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    searchHistoryRepository: SearchHistoryRepository,
    onLocationConfirmed: (GeoPoint) -> Unit,
    initialLocation: GeoPoint?
) {
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val viewModel: LocationPickerViewModel = viewModel(
        factory = LocationPickerViewModel.provideFactory(geocoder, searchHistoryRepository, initialLocation)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            MapViewContainer(
                modifier = Modifier.fillMaxSize(),
                initialLocation = viewModel.initialLocation,
                onMapMove = viewModel::onMapMove,
                onMapMoveFinished = viewModel::onMapMoveFinished
            )

            CenterPin(isMapMoving = uiState.isMapMoving)

            LocationSearchBar(
                modifier = Modifier.align(Alignment.TopCenter),
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onPerformSearch = { viewModel.onPerformSearch() },
                onClearSearch = viewModel::onClearSearch,
                onFocusChanged = viewModel::onSearchFocusChanged,
                onGoToCurrentLocation = {
                    viewModel.initialLocation?.let { viewModel.onMapMove(it) }
                }
            )

            if (uiState.isSearchFocused) {
                SearchOverlay(
                    modifier = Modifier.fillMaxSize(),
                    suggestions = uiState.suggestions,
                    history = uiState.history,
                    onSuggestionClicked = viewModel::onSuggestionClicked,
                    onHistoryItemClicked = viewModel::onHistoryItemClicked,
                    onRemoveFromHistory = viewModel::onRemoveFromHistory
                )
            } else {
                SelectedAddressCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp), // Space for the button
                    addressText = uiState.addressText,
                    isVisible = true
                )
                ConfirmLocationButton(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onClick = { uiState.selectedLocation?.let(onLocationConfirmed) }
                )
            }
        }
    }
}

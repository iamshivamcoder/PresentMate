package com.example.presentmate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentmate.LocationPickerUiState
import com.example.presentmate.LocationPickerViewModel
import org.osmdroid.util.GeoPoint

@Composable
internal fun LocationPickerContent(
    viewModel: LocationPickerViewModel,
    uiState: LocationPickerUiState,
    onLocationConfirmed: (GeoPoint) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            )
    ) {
        // Top: Search bar and optional overlay (keeps search UI at the top)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            LocationSearchBar(
                modifier = Modifier.fillMaxWidth(),
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onPerformSearch = viewModel::onPerformSearch,
                onClearSearch = viewModel::onClearSearch,
                onFocusChanged = viewModel::onSearchFocusChanged,
                onGoToCurrentLocation = {
                    viewModel.initialLocation?.let {
                        viewModel.onMapMove(it)
                    }
                }
            )

            AnimatedVisibility(
                visible = uiState.isSearchFocused,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(300, easing = androidx.compose.animation.core.Easing { it * it })
                ) + fadeIn() + scaleIn(initialScale = 0.95f),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(250)
                ) + fadeOut() + scaleOut(targetScale = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                // Limit overlay height so it doesn't cover the entire screen and keeps bottom actions reachable
                SearchOverlay(
                    modifier = Modifier.fillMaxWidth(),
                    suggestions = uiState.suggestions,
                    history = uiState.history,
                    savedPlaces = uiState.savedPlaces,
                    onSuggestionClicked = viewModel::onSuggestionClicked,
                    onHistoryItemClicked = viewModel::onHistoryItemClicked,
                    onRemoveFromHistory = viewModel::onRemoveFromHistory,
                    onSavedPlaceClicked = viewModel::onSavedPlaceClicked
                )
            }
        }

        // Middle: Map that takes the remaining space
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            MapViewContainer(
                modifier = Modifier.fillMaxSize(),
                initialLocation = viewModel.initialLocation,
                onMapMove = viewModel::onMapMove,
                onMapMoveFinished = viewModel::onMapMoveFinished
            )

            // Center Pin
            CenterPin(isMapMoving = uiState.isMapMoving)
        }

        // Loading indicator overlay (positioned over the map area)
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Box {
                this@Column.AnimatedVisibility(
                    visible = uiState.isMapMoving,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(56.dp)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            }
        }

        // Bottom: Selected address + Confirm button (kept separate from map)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = !uiState.isSearchFocused && uiState.addressText.isNotEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(400, easing = androidx.compose.animation.core.Easing { it * it * it })
                ) + fadeIn() + scaleIn(initialScale = 0.9f),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(250)
                ) + fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
            ) {
                SelectedAddressCard(
                    addressText = uiState.addressText,
                    isVisible = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = !uiState.isSearchFocused,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(350, delayMillis = 100)
                ) + fadeIn() + scaleIn(initialScale = 0.8f),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(200)
                ) + fadeOut() + scaleOut(targetScale = 0.8f)
            ) {
                ConfirmLocationButton(
                    onClick = {
                        uiState.selectedLocation?.let(onLocationConfirmed)
                    },
                    enabled = uiState.selectedLocation != null && !uiState.isMapMoving
                )
            }
        }
    }
}
package com.example.presentmate.ui.components.location

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.presentmate.viewmodel.LocationPickerUiState
import com.example.presentmate.viewmodel.LocationPickerViewModel
import com.example.presentmate.ui.components.LocationSearchBar
import com.example.presentmate.ui.components.SearchOverlay
import com.example.presentmate.ui.components.common.ConfirmLocationButton
import org.osmdroid.util.GeoPoint

@Composable
internal fun SelectedAddressCard(
    modifier: Modifier = Modifier,
    addressText: String,
    isVisible: Boolean,
    elevation: Dp = 4.dp
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Selected address: $addressText" },
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = addressText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun LocationPickerContent(
    viewModel: LocationPickerViewModel,
    uiState: LocationPickerUiState,
    onLocationConfirmed: (GeoPoint) -> Unit,
    onGoToCurrentLocation: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(uiState.searchQuery) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        MapViewContainer(
            modifier = Modifier.fillMaxSize(),
            selectedLocation = uiState.selectedLocation,
            onMapMove = viewModel::onMapMove,
            onMapMoveFinished = viewModel::onMapMoveFinished
        )

        // Center Icon
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            CenterIcon(
                isMapMoving = uiState.isMapMoving,
                isFetchingLocation = uiState.isFetchingLocation,
            )
        }

        // Top Search Bar and Overlay
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            val isWideScreen = maxWidth > 600.dp
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = if (isWideScreen) 500.dp else Dp.Unspecified)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(bottom = 60.dp)
            ) {
                LocationSearchBar(
                    modifier = Modifier.fillMaxWidth(),
                    searchQuery = searchQuery,
                    onSearchQueryChanged = {
                        searchQuery = it
                        viewModel.onSearchQueryChanged(it)
                    },
                    onPerformSearch = {
                        viewModel.onPerformSearch(searchQuery)
                    },
                    onClearSearch = {
                        searchQuery = ""
                        viewModel.onClearSearch()
                    },
                    onGoToCurrentLocation = onGoToCurrentLocation
                )

                AnimatedVisibility(
                    visible = uiState.isSearchFocused,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(300, easing = Easing { it * it })
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
                    SearchOverlay(
                        modifier = Modifier.fillMaxWidth(),
                        suggestions = uiState.suggestions,
                        history = uiState.history,
                        savedPlaces = uiState.savedPlaces,
                        onSuggestionClicked = {
                            searchQuery = it.getAddressLine(0) ?: ""
                            viewModel.onSuggestionClicked(it)
                        },
                        onHistoryItemClicked = {
                            searchQuery = it
                            viewModel.onHistoryItemClicked(it)
                        },
                        onRemoveFromHistory = viewModel::onRemoveFromHistory,
                        onSavedPlaceClicked = {
                            searchQuery = it.address
                            viewModel.onSavedPlaceClicked(it)
                        }
                    )
                }
            }
        }

        // Bottom Confirmation Area
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 1f)
                        )
                    )
                )
                .padding(top = 60.dp) // Add padding to avoid overlapping with content
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SelectedAddressCard(
                    addressText = uiState.addressText,
                    isVisible = uiState.addressText.isNotEmpty(),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                ConfirmLocationButton(
                    onClick = {
                        uiState.selectedLocation?.let(onLocationConfirmed)
                    },
                    enabled = uiState.selectedLocation != null && !uiState.isMapMoving && uiState.addressText.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth() // Make button fill width
                )
            }
        }
    }
}

private val PIN_MOVING_ELEVATION = 12.dp
private val PIN_STOPPED_ELEVATION = 4.dp
private val PIN_MOVING_SIZE = 52.dp
private val PIN_STOPPED_SIZE = 44.dp
private const val ANIMATION_DURATION = 300

@Composable
internal fun CenterIcon(isMapMoving: Boolean, isFetchingLocation: Boolean) {
    val isLoading = isMapMoving || isFetchingLocation

    val elevation by animateDpAsState(
        targetValue = if (isLoading) PIN_MOVING_ELEVATION else PIN_STOPPED_ELEVATION,
        animationSpec = tween(ANIMATION_DURATION, easing = LinearEasing),
        label = "pin_elevation"
    )
    val size by animateDpAsState(
        targetValue = if (isLoading) PIN_MOVING_SIZE else PIN_STOPPED_SIZE,
        animationSpec = tween(ANIMATION_DURATION, easing = LinearEasing),
        label = "pin_size"
    )

    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = elevation,
                shape = CircleShape,
                clip = false,
                spotColor = MaterialTheme.colorScheme.primary
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .semantics {
                contentDescription = if (isLoading) "Loading location" else "Location marker"
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size * 0.6f),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = null,
                modifier = Modifier.size(size * 0.6f),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

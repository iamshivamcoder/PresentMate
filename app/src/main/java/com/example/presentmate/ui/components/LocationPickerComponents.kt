package com.example.presentmate.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentmate.LocationPickerUiState
import com.example.presentmate.LocationPickerViewModel
import com.example.presentmate.data.SavedPlace
import org.osmdroid.util.GeoPoint

@Composable
internal fun SelectedAddressCard(
    modifier: Modifier = Modifier,
    addressText: String,
    isVisible: Boolean,
    elevation: Dp = 8.dp
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
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = addressText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
    onLocationConfirmed: (GeoPoint) -> Unit
) {
    var searchQuery by remember { mutableStateOf(uiState.searchQuery) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        MapViewContainer(
            modifier = Modifier.fillMaxSize(),
            initialLocation = viewModel.initialLocation,
            onMapMove = viewModel::onMapMove,
            onMapMoveFinished = viewModel::onMapMoveFinished
        )

        // Center Pin
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            CenterPin(isMapMoving = uiState.isMapMoving)
        }

        // Loading indicator overlay
        if (uiState.isMapMoving) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }

        // Top Search Bar and Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .fillMaxWidth()
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

        // Bottom Confirmation Area
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SelectedAddressCard(
                addressText = uiState.addressText,
                isVisible = uiState.addressText.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(8.dp))

            ConfirmLocationButton(
                onClick = {
                    uiState.selectedLocation?.let(onLocationConfirmed)
                },
                enabled = uiState.selectedLocation != null && !uiState.isMapMoving
            )
        }
    }
}

@Composable
internal fun ConfirmLocationButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val view = LocalView.current

    // Scale animation for button press feedback
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.95f,
        animationSpec = tween(durationMillis = 150),
        label = "button_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .semantics { contentDescription = "Confirm selected location" },
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onClick()
            },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp)),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (enabled) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                },
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp,
                disabledElevation = 2.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = if (enabled) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            )
                        },
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Confirm Location",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

private val PIN_MOVING_ELEVATION = 12.dp
private val PIN_STOPPED_ELEVATION = 4.dp
private val PIN_MOVING_SIZE = 48.dp
private val PIN_STOPPED_SIZE = 40.dp
private val PULSE_SIZE = 80.dp
private const val ANIMATION_DURATION = 400
private const val PULSE_DURATION = 2000

@Composable
internal fun CenterPin(isMapMoving: Boolean) {
    val elevation by animateDpAsState(
        targetValue = if (isMapMoving) PIN_MOVING_ELEVATION else PIN_STOPPED_ELEVATION,
        animationSpec = tween(ANIMATION_DURATION, easing = LinearEasing),
        label = "pin_elevation"
    )
    val size by animateDpAsState(
        targetValue = if (isMapMoving) PIN_MOVING_SIZE else PIN_STOPPED_SIZE,
        animationSpec = tween(ANIMATION_DURATION, easing = LinearEasing),
        label = "pin_size"
    )

    // Pulsing animation for the outer ring
    val pulseScale by animateFloatAsState(
        targetValue = if (isMapMoving) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = PULSE_DURATION
                1f at 0
                1.3f at PULSE_DURATION / 2
                1f at PULSE_DURATION
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_animation"
    )

    Box(
        modifier = Modifier
            .size(if (isMapMoving) PULSE_SIZE else size)
            .semantics { contentDescription = "Location marker pin" },
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring when moving
        if (isMapMoving) {
            Box(
                modifier = Modifier
                    .size(PULSE_SIZE)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Main pin container
        Box(
            modifier = Modifier
                .size(size)
                .shadow(elevation, CircleShape, spotColor = MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
internal fun InitialLoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .semantics { contentDescription = "Loading location picker" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading map...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
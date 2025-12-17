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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
 fun SelectedAddressCard(
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
    onGoToCurrentLocation: () -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Map View - Full screen
        MapViewContainer(
            modifier = Modifier.fillMaxSize(),
            selectedLocation = uiState.selectedLocation,
            onMapMove = viewModel::onMapMove,
            onMapMoveFinished = viewModel::onMapMoveFinished
        )

        // Center Icon (location marker)
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            CenterIcon(
                isMapMoving = uiState.isMapMoving,
                isFetchingLocation = uiState.isFetchingLocation,
            )
        }

        // Floating Action Button for current location - bottom right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 200.dp) // Position above the address card
                .size(48.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .semantics { contentDescription = "Go to current location" },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.IconButton(
                onClick = onGoToCurrentLocation,
                modifier = Modifier.size(48.dp)
            ) {
                if (uiState.isFetchingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Current location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
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
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(top = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SelectedAddressCard(
                    addressText = uiState.addressText,
                    isVisible = uiState.addressText.isNotEmpty(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                ConfirmLocationButton(
                    onClick = {
                        uiState.selectedLocation?.let(onLocationConfirmed)
                    },
                    enabled = uiState.selectedLocation != null && !uiState.isMapMoving && uiState.addressText.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
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

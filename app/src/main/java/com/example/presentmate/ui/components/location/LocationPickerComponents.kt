package com.example.presentmate.ui.components.location

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.presentmate.viewmodel.LocationPickerUiState
import com.example.presentmate.viewmodel.LocationPickerViewModel
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
    onLocationConfirmed: (GeoPoint) -> Unit
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

        // Bottom Navigation Bar Style - Address + Confirm Button
        androidx.compose.material3.Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Address info - takes remaining space
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.addressText.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.addressText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Move map to select location",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Confirm button
                androidx.compose.material3.Button(
                    onClick = { uiState.selectedLocation?.let(onLocationConfirmed) },
                    enabled = uiState.selectedLocation != null && !uiState.isMapMoving && uiState.addressText.isNotEmpty(),
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "Confirm",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
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

package com.example.presentmate.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

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
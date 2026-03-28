package com.example.presentmate.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun LeapingFrogTimer(
    startTimeMillis: Long,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // Update timer every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val elapsedMillis = (currentTime - startTimeMillis).coerceAtLeast(0)
    val elapsedHours = elapsedMillis / (1000 * 60 * 60)
    val elapsedMinutes = (elapsedMillis / (1000 * 60)) % 60
    val elapsedSeconds = (elapsedMillis / 1000) % 60

    // Number of lily pads (e.g., 10 pads for a 10-hour day)
    val totalPads = 10
    val progress = (elapsedMillis.toFloat() / (10 * 60 * 60 * 1000f)).coerceIn(0f, 1f)
    val currentPadIndex = (progress * (totalPads - 1)).toInt()

    // Animation for frog hopping
    val frogPositionState = rememberInfiniteTransition(label = "frog_hop")
    val frogHopY by frogPositionState.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y_hop"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Session Time",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = String.format("%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress tracking with lily pads
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Background river
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // Lily pads
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalPads) { index ->
                        val isReached = index <= currentPadIndex
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isReached) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == 0) {
                                Text("S", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            if (index == totalPads - 1) {
                                Text("G", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // The Leaping Frog
                val frogOffset = progress * 100 // This is a simplification for visual positioning
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceAtLeast(0.05f))
                        .offset(y = frogHopY.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (elapsedHours >= 4) {
                            Text("👑", fontSize = 14.sp) // Crown for 4+ hours
                        }
                        Text("🐸", fontSize = 28.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when {
                    elapsedHours >= 4 -> "Incredible focus! Keep it up! 👑"
                    elapsedHours >= 1 -> "Off to a great start! Hop along! 🐸"
                    else -> "Focusing at the library... 📚"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

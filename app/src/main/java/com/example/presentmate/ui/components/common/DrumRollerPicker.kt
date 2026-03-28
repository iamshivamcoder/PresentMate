package com.example.presentmate.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * A vertical drum-roller (scroll-wheel) picker that snaps to items.
 *
 * @param items          List of display strings
 * @param selectedIndex  Currently selected index (0-based)
 * @param onItemSelected Called with the new index when the user settles on an item
 * @param itemHeight     Height of each item cell
 * @param visibleItems   How many items are visible at once (must be odd for centre alignment)
 */
@Composable
fun DrumRollerPicker(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 44.dp,
    visibleItems: Int = 3
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val halfVisible = visibleItems / 2

    // Scroll to selection when external selectedIndex changes
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }

    // Snap to nearest item when scroll settles
    val isScrollInProgress = listState.isScrollInProgress
    LaunchedEffect(isScrollInProgress) {
        if (!isScrollInProgress) {
            val centreIndex = listState.firstVisibleItemIndex +
                    (if (listState.firstVisibleItemScrollOffset > 0) 1 else 0)
            val clamped = centreIndex.coerceIn(0, items.lastIndex)
            scope.launch { listState.animateScrollToItem(clamped) }
            onItemSelected(clamped)
        }
    }

    val totalHeight = itemHeight * visibleItems

    Box(
        modifier = modifier
            .height(totalHeight)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                // Fade top and bottom edges for the "drum roll" effect
                val fadeHeight = (totalHeight / visibleItems * halfVisible).toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to Color.Black,
                        0.6f to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = itemHeight * halfVisible),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val centreVisibleIndex = listState.firstVisibleItemIndex +
                        if (listState.firstVisibleItemScrollOffset > 0) 1 else 0
                val distance = (index - centreVisibleIndex).let { kotlin.math.abs(it) }
                val scale = (1f - distance * 0.12f).coerceAtLeast(0.7f)
                val alpha = (1f - distance * 0.3f).coerceAtLeast(0.2f)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                ) {
                    Text(
                        text = items[index],
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (16 * scale).sp,
                            fontWeight = if (index == centreVisibleIndex) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (index == centreVisibleIndex)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

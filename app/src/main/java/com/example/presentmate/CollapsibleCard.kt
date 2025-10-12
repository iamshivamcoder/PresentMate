package com.example.presentmate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A stateless, customizable collapsible card.
 *
 * @param expanded Whether the card is currently expanded.
 * @param onToggle The callback to be invoked when the header is clicked to toggle the state.
 * @param modifier The modifier to be applied to the card.
 * @param enterTransition The animation for the content appearing.
 * @param exitTransition The animation for the content disappearing.
 * @param indicatorIcon A composable that provides the expand/collapse indicator icon.
 * @param headerContent The content to display in the header, always visible.
 * @param collapsibleContent The content to display when the card is expanded.
 */
@Composable
fun CollapsibleCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = fadeIn() + expandVertically(),
    exitTransition: ExitTransition = fadeOut() + shrinkVertically(),
    indicatorIcon: @Composable (isExpanded: Boolean) -> Unit = { isExpanded ->
        Icon(
            imageVector = if (isExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand"
        )
    },
    headerContent: @Composable () -> Unit,
    collapsibleContent: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 8.dp, vertical = 32.dp), // Adjusted vertical padding for thickness
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    headerContent()
                }
                Spacer(modifier = Modifier.width(8.dp))
                indicatorIcon(expanded)
            }
            AnimatedVisibility(
                visible = expanded,
                enter = enterTransition,
                exit = exitTransition
            ) {
                Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)) {
                    collapsibleContent()
                }
            }
        }
    }
}

/**
 * A stateful convenience wrapper for the CollapsibleCard that manages its own expanded state.
 * Useful for simple cases where the state does not need to be controlled externally.
 */
@Composable
fun CollapsibleCard(
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit,
    collapsibleContent: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    CollapsibleCard(
        expanded = expanded,
        onToggle = { expanded = !expanded },
        modifier = modifier,
        headerContent = headerContent,
        collapsibleContent = collapsibleContent
    )
}

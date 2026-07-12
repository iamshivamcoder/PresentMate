package com.example.presentmate.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentmate.db.DeveloperIdea
import com.example.presentmate.viewmodel.DeveloperIdeasSort
import com.example.presentmate.viewmodel.DeveloperIdeasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperIdeasScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeveloperIdeasViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Portal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by Newest") },
                            onClick = {
                                viewModel.setSortBy(DeveloperIdeasSort.NEWEST)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (uiState.sortBy == DeveloperIdeasSort.NEWEST) {
                                    Icon(Icons.Default.Check, null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Priority") },
                            onClick = {
                                viewModel.setSortBy(DeveloperIdeasSort.PRIORITY)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (uiState.sortBy == DeveloperIdeasSort.PRIORITY) {
                                    Icon(Icons.Default.Check, null)
                                }
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Idea")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Dashboard
            StatsDashboard(uiState)

            // Search and Filters
            SearchBarAndFilters(uiState, viewModel)

            // Idea Checklist
            if (uiState.ideas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No plans found. Log your first future idea!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.ideas, key = { it.id }) { idea ->
                        IdeaListItem(
                            idea = idea,
                            onToggleStatus = { viewModel.toggleIdeaStatus(idea) },
                            onDelete = { viewModel.deleteIdea(idea) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddIdeaDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, cat, prio ->
                viewModel.addIdea(title, desc, cat, prio)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun StatsDashboard(uiState: com.example.presentmate.viewmodel.DeveloperIdeasUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem("Total", uiState.totalCount.toString())
            StatItem("Active", uiState.todoCount.toString())
            StatItem("Bugs", uiState.bugCount.toString())
            StatItem("Done", uiState.doneCount.toString())
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarAndFilters(
    uiState: com.example.presentmate.viewmodel.DeveloperIdeasUiState,
    viewModel: DeveloperIdeasViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search future ideas & bugs...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, null)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("All", "Idea", "Bug", "Feature", "Chore")
            categories.forEach { cat ->
                val isSelected = (cat == "All" && uiState.selectedCategory == null) ||
                        (uiState.selectedCategory == cat)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.setCategoryFilter(if (cat == "All") null else cat)
                    },
                    label = { Text(cat) }
                )
            }
        }

        // Status Filter Row
        TabRow(
            selectedTabIndex = when (uiState.selectedStatus) {
                null -> 0
                "Todo" -> 1
                "Done" -> 2
                else -> 0
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = uiState.selectedStatus == null,
                onClick = { viewModel.setStatusFilter(null) },
                text = { Text("All Status") }
            )
            Tab(
                selected = uiState.selectedStatus == "Todo",
                onClick = { viewModel.setStatusFilter("Todo") },
                text = { Text("Todo") }
            )
            Tab(
                selected = uiState.selectedStatus == "Done",
                onClick = { viewModel.setStatusFilter("Done") },
                text = { Text("Done") }
            )
        }
    }
}

@Composable
fun IdeaListItem(
    idea: DeveloperIdea,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = idea.status == "Done",
                    onCheckedChange = { onToggleStatus() }
                )
                
                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = idea.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (idea.status == "Done") TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryChip(idea.category)
                        PriorityBadge(idea.priority)
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp)) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = idea.description.ifEmpty { "No description provided." },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(category: String) {
    val (color, icon) = when (category) {
        "Bug" -> MaterialTheme.colorScheme.errorContainer to Icons.Default.BugReport
        "Feature" -> MaterialTheme.colorScheme.primaryContainer to Icons.Default.AutoAwesome
        "Idea" -> MaterialTheme.colorScheme.tertiaryContainer to Icons.Default.Lightbulb
        else -> MaterialTheme.colorScheme.secondaryContainer to Icons.Default.Settings
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = contentColorFor(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = category,
                style = MaterialTheme.typography.labelSmall,
                color = contentColorFor(color)
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val color = when (priority) {
        "High" -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        "Medium" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    }
    val textColor = when (priority) {
        "High" -> MaterialTheme.colorScheme.error
        "Medium" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = priority,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIdeaDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, category: String, priority: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Idea") }
    var priority by remember { mutableStateOf("Medium") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Future Idea / Bug") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Category Dropdown
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    var expandedCat by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCat,
                        onExpandedChange = { expandedCat = !expandedCat }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false }
                        ) {
                            listOf("Idea", "Bug", "Feature", "Chore").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        category = option
                                        expandedCat = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Priority Segmented Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf("Low", "Medium", "High")
                        options.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = priority == option,
                                onClick = { priority = option },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                            ) {
                                Text(option)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, desc, category, priority) },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

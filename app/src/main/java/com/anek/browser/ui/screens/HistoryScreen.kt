package com.anek.browser.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anek.browser.database.entity.HistoryEntity
import com.anek.browser.utils.isToday
import com.anek.browser.utils.isYesterday
import com.anek.browser.utils.toDateString

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    history: List<HistoryEntity>,
    onItemClick: (HistoryEntity) -> Unit,
    onDeleteItem: (HistoryEntity) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(setOf<Long>()) }

    val filtered = if (searchQuery.isBlank()) history
    else history.filter { it.title.contains(searchQuery, true) || it.url.contains(searchQuery, true) }

    val grouped = filtered.groupBy {
        when {
            it.timestamp.isToday() -> "Today"
            it.timestamp.isYesterday() -> "Yesterday"
            else -> it.timestamp.toDateString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (selectedItems.isNotEmpty()) {
                        IconButton(onClick = {
                            selectedItems.forEach { id -> filtered.find { it.id == id }?.let { onDeleteItem(it) } }
                            selectedItems = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Chrome-like search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search history") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = MaterialTheme.shapes.extraLarge
            )

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        Text("No history", style = MaterialTheme.typography.titleMedium)
                        Text("Pages you visit will appear here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    grouped.forEach { (date, items) ->
                        stickyHeader {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    date,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                        items(items, key = { it.id }) { item ->
                            HistoryItemRow(
                                item = item,
                                isSelected = selectedItems.contains(item.id),
                                onClick = { onItemClick(item) },
                                onDelete = { onDeleteItem(item) },
                                onToggleSelect = {
                                    selectedItems = if (selectedItems.contains(item.id)) selectedItems - item.id else selectedItems + item.id
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear browsing history?") },
                text = { Text("This will permanently delete all browsing history. This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        onClearAll()
                        showClearDialog = false
                    }) { Text("Clear") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItemRow(
    item: HistoryEntity,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleSelect: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .combinedClickable(onClick = onClick, onLongClick = { showMenu = true }),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        ListItem(
            headlineContent = { Text(item.title.ifBlank { item.url }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium) },
            supportingContent = {
                Column {
                    Text(item.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${item.visitCount} visits • ${item.timestamp.toDateString()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            leadingContent = {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(item.title.take(1).uppercase().ifBlank { "W" }, style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
            trailingContent = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Open in new tab") }, onClick = { showMenu = false; onClick() }, leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) })
                        DropdownMenuItem(text = { Text("Copy link") }, onClick = { showMenu = false }, leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) })
                    }
                }
            }
        )
    }
}

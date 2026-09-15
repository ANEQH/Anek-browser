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
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
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
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(12.dp))
                        Text("No history found", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    grouped.forEach { (date, items) ->
                        stickyHeader {
                            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
                                Text(
                                    date,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                        items(items, key = { it.id }) { item ->
                            HistoryItemRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onDelete = { onDeleteItem(item) }
                            )
                        }
                    }
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear history?") },
                text = { Text("This will permanently delete all browsing history.") },
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
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(item.title.ifBlank { item.url }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(item.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        },
        trailingContent = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) })
            }
        },
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = { showMenu = true })
    )
    HorizontalDivider()
}

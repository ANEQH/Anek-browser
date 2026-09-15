package com.anek.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anek.browser.browser.Tab

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TabsScreen(
    tabs: List<Tab>,
    currentTabId: String,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onCloseAll: () -> Unit,
    onDuplicate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPrivate by remember { mutableStateOf(false) }
    var showCloseAllDialog by remember { mutableStateOf(false) }

    val filtered = tabs.filter { it.isIncognito == showPrivate }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (showPrivate) "Private tabs" else "Tabs")
                        Text(
                            "${filtered.size} ${if (filtered.size == 1) "tab" else "tabs"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = "Close") }
                },
                actions = {
                    if (filtered.isNotEmpty()) {
                        IconButton(onClick = { showCloseAllDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Close all")
                        }
                    }
                    IconButton(onClick = if (showPrivate) onNewPrivateTab else onNewTab) {
                        Icon(Icons.Default.Add, contentDescription = "New tab")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (showPrivate) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showPrivate,
                        onClick = { showPrivate = false },
                        label = { Text("Normal (${tabs.count { !it.isIncognito }})") },
                        leadingIcon = { Icon(Icons.Default.Tab, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    FilterChip(
                        selected = showPrivate,
                        onClick = { showPrivate = true },
                        label = { Text("Private (${tabs.count { it.isIncognito }})") },
                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        },
        containerColor = if (showPrivate) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerLow
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (showPrivate) Icons.Default.Security else Icons.Default.Tab,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (showPrivate) "No private tabs" else "No tabs open",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (showPrivate) "Private tabs won't be saved after you close them"
                        else "Open a new tab to start browsing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = if (showPrivate) onNewPrivateTab else onNewTab) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("New ${if (showPrivate) "private " else ""}tab")
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filtered, key = { it.id }) { tab ->
                        SwipeToDismissTabCard(
                            tab = tab,
                            isSelected = tab.id == currentTabId,
                            onSelect = { onTabSelected(tab.id) },
                            onClose = { onTabClosed(tab.id) },
                            onDuplicate = { onDuplicate(tab.id) }
                        )
                    }
                }
            }

            // FAB for new tab - Chrome-like
            FloatingActionButton(
                onClick = if (showPrivate) onNewPrivateTab else onNewTab,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New tab")
            }
        }
    }

    if (showCloseAllDialog) {
        AlertDialog(
            onDismissRequest = { showCloseAllDialog = false },
            title = { Text("Close all ${if (showPrivate) "private " else ""}tabs?") },
            text = { Text("${filtered.size} tabs will be closed. ${if (showPrivate) "" else "You can restore them from closed tabs."}") },
            confirmButton = {
                TextButton(onClick = { onCloseAll(); showCloseAllDialog = false }) { Text("Close all") }
            },
            dismissButton = {
                TextButton(onClick = { showCloseAllDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissTabCard(
    tab: Tab,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onDuplicate: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                onClose()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Close", tint = MaterialTheme.colorScheme.error)
            }
        },
        content = {
            TabCard(
                tab = tab,
                isSelected = isSelected,
                onSelect = onSelect,
                onClose = onClose,
                onDuplicate = onDuplicate,
                showOptions = showOptions,
                onShowOptionsChange = { showOptions = it }
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabCard(
    tab: Tab,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onDuplicate: () -> Unit,
    showOptions: Boolean = false,
    onShowOptionsChange: (Boolean) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onSelect,
                onLongClick = { onShowOptionsChange(true) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                tab.isIncognito -> MaterialTheme.colorScheme.surfaceContainerHigh
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (tab.isIncognito) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (tab.isIncognito) Icons.Default.Security else Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (tab.isPinned) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.PushPin, contentDescription = "Pinned", modifier = Modifier.size(14.dp))
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                tab.displayTitle(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tab.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    tab.host(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            if (showOptions) {
                DropdownMenu(expanded = true, onDismissRequest = { onShowOptionsChange(false) }) {
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { onShowOptionsChange(false); onDuplicate() }, leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) })
                    DropdownMenuItem(text = { Text("Close") }, onClick = { onShowOptionsChange(false); onClose() }, leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) })
                    DropdownMenuItem(text = { Text("Close others") }, onClick = { onShowOptionsChange(false) }, leadingIcon = { Icon(Icons.Default.Tab, contentDescription = null) })
                }
            }
        }
    }
}

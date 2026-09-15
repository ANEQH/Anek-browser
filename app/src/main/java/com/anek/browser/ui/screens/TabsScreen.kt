package com.anek.browser.ui.screens

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

    val filtered = tabs.filter { it.isIncognito == showPrivate }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showPrivate) "Private tabs" else "Tabs (${tabs.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = "Close") }
                },
                actions = {
                    IconButton(onClick = onCloseAll) { Icon(Icons.Default.Delete, contentDescription = "Close all") }
                    IconButton(onClick = if (showPrivate) onNewPrivateTab else onNewTab) {
                        Icon(Icons.Default.Add, contentDescription = "New tab")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = !showPrivate,
                        onClick = { showPrivate = false },
                        label = { Text("Normal") },
                        leadingIcon = { Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    FilterChip(
                        selected = showPrivate,
                        onClick = { showPrivate = true },
                        label = { Text("Private") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
    ) { padding ->
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("No ${if (showPrivate) "private " else ""}tabs open", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = if (showPrivate) onNewPrivateTab else onNewTab) {
                        Text("New ${if (showPrivate) "private " else ""}tab")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier.padding(padding).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { tab ->
                    TabCard(
                        tab = tab,
                        isSelected = tab.id == currentTabId,
                        onSelect = { onTabSelected(tab.id) },
                        onClose = { onTabClosed(tab.id) },
                        onDuplicate = { onDuplicate(tab.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabCard(
    tab: Tab,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onDuplicate: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onSelect,
                onLongClick = { showOptions = true }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (tab.isIncognito) Icons.Default.Lock else Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                tab.title.ifBlank { "New tab" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                tab.url,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showOptions) {
                DropdownMenu(expanded = true, onDismissRequest = { showOptions = false }) {
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { showOptions = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Close") }, onClick = { showOptions = false; onClose() })
                }
            }
        }
    }
}

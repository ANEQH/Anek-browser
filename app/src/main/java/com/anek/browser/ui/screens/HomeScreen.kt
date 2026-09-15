package com.anek.browser.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anek.browser.database.entity.BookmarkEntity
import com.anek.browser.database.entity.HistoryEntity
import com.anek.browser.database.entity.ShortcutEntity
import com.anek.browser.ui.theme.SeedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onSearch: (String) -> Unit,
    shortcuts: List<ShortcutEntity>,
    recentHistory: List<HistoryEntity>,
    bookmarks: List<BookmarkEntity>,
    showShortcuts: Boolean,
    showRecent: Boolean,
    showWallpaper: Boolean,
    onAddShortcut: () -> Unit,
    onShortcutClick: (ShortcutEntity) -> Unit,
    onShortcutLongPress: (ShortcutEntity) -> Unit,
    onHistoryClick: (HistoryEntity) -> Unit,
    onBookmarkClick: (BookmarkEntity) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (showWallpaper) Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                ) else Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            // Logo / Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = SeedColor,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("A", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text("Anek Browser", style = MaterialTheme.typography.headlineMedium)
            }

            Spacer(Modifier.height(24.dp))

            // Search bar
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search or type web address") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    if (searchText.isNotBlank()) {
                        IconButton(onClick = {
                            onSearch(searchText)
                            searchText = ""
                        }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Go")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Quick access
            if (showShortcuts) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quick access", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onAddShortcut) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (shortcuts.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No shortcuts yet", style = MaterialTheme.typography.bodyMedium)
                            Text("Tap Add to create shortcuts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(shortcuts) { shortcut ->
                            ShortcutItem(
                                shortcut = shortcut,
                                onClick = { onShortcutClick(shortcut) },
                                onLongPress = { onShortcutLongPress(shortcut) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // Recently visited
            if (showRecent && recentHistory.isNotEmpty()) {
                Text("Recently visited", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentHistory.take(10)) { item ->
                        RecentItem(item = item, onClick = { onHistoryClick(item) })
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Bookmarks
            if (bookmarks.isNotEmpty()) {
                Text("Bookmarks", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bookmarks.take(5).forEach { bm ->
                        BookmarkHomeItem(bookmark = bm, onClick = { onBookmarkClick(bm) })
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutItem(
    shortcut: ShortcutEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    shortcut.title.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            shortcut.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecentItem(
    item: HistoryEntity,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(item.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BookmarkHomeItem(
    bookmark: BookmarkEntity,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bookmark.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Text(bookmark.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

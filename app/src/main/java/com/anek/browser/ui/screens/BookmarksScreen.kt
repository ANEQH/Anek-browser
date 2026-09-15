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
import com.anek.browser.database.entity.BookmarkEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    onItemClick: (BookmarkEntity) -> Unit,
    onDelete: (BookmarkEntity) -> Unit,
    onEdit: (BookmarkEntity) -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }

    val filtered = if (searchQuery.isBlank()) bookmarks
    else bookmarks.filter { it.title.contains(searchQuery, true) || it.url.contains(searchQuery, true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = onExport) { Icon(Icons.Default.Share, contentDescription = "Export") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search bookmarks") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                singleLine = true
            )

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(12.dp))
                        Text("No bookmarks yet", style = MaterialTheme.typography.titleMedium)
                        Text("Bookmark pages to see them here", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { bm ->
                        BookmarkRow(
                            bookmark = bm,
                            onClick = { onItemClick(bm) },
                            onEdit = { editingBookmark = bm },
                            onDelete = { onDelete(bm) }
                        )
                    }
                }
            }
        }

        editingBookmark?.let { bm ->
            var title by remember { mutableStateOf(bm.title) }
            var url by remember { mutableStateOf(bm.url) }
            AlertDialog(
                onDismissRequest = { editingBookmark = null },
                title = { Text("Edit bookmark") },
                text = {
                    Column {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, singleLine = true)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onEdit(bm.copy(title = title, url = url))
                        editingBookmark = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { editingBookmark = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarkRow(
    bookmark: BookmarkEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(bookmark.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(bookmark.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) })
            }
        },
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = { showMenu = true })
    )
    HorizontalDivider()
}

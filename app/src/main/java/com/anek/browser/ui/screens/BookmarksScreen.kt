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
import com.anek.browser.database.entity.BookmarkFolderEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    folders: List<BookmarkFolderEntity> = emptyList(),
    onItemClick: (BookmarkEntity) -> Unit,
    onDelete: (BookmarkEntity) -> Unit,
    onEdit: (BookmarkEntity) -> Unit,
    onMoveToFolder: ((BookmarkEntity, Long?) -> Unit)? = null,
    onCreateFolder: ((String) -> Unit)? = null,
    onExport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }

    val filtered = if (searchQuery.isBlank()) bookmarks
    else bookmarks.filter { it.title.contains(searchQuery, true) || it.url.contains(searchQuery, true) }

    val displayBookmarks = filtered.filter { it.folderId == selectedFolderId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = { showFolderDialog = true }) { Icon(Icons.Default.CreateNewFolder, contentDescription = "New folder") }
                    IconButton(onClick = onExport) { Icon(Icons.Default.Share, contentDescription = "Export") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Folder chips - Chrome-like
            if (folders.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = 0,
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Tab(selected = selectedFolderId == null, onClick = { selectedFolderId = null }, text = { Text("All") })
                    folders.forEach { folder ->
                        Tab(
                            selected = selectedFolderId == folder.id,
                            onClick = { selectedFolderId = folder.id },
                            text = { Text(folder.name) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search bookmarks") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge
            )

            if (displayBookmarks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        Text(if (searchQuery.isNotBlank()) "No results" else "No bookmarks", style = MaterialTheme.typography.titleMedium)
                        Text("Bookmark pages to see them here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (searchQuery.isBlank()) {
                            Spacer(Modifier.height(16.dp))
                            FilledTonalButton(onClick = { /* hint */ }) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("How to bookmark")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(displayBookmarks, key = { it.id }) { bm ->
                        BookmarkRow(
                            bookmark = bm,
                            onClick = { onItemClick(bm) },
                            onEdit = { editingBookmark = bm },
                            onDelete = { onDelete(bm) },
                            onMove = onMoveToFolder
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
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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

        if (showFolderDialog) {
            var folderName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showFolderDialog = false },
                title = { Text("New folder") },
                text = {
                    OutlinedTextField(value = folderName, onValueChange = { folderName = it }, label = { Text("Folder name") }, singleLine = true)
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (folderName.isNotBlank()) onCreateFolder?.invoke(folderName)
                        showFolderDialog = false
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showFolderDialog = false }) { Text("Cancel") }
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
    onDelete: () -> Unit,
    onMove: ((BookmarkEntity, Long?) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .combinedClickable(onClick = onClick, onLongClick = { showMenu = true }),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        ListItem(
            headlineContent = { Text(bookmark.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium) },
            supportingContent = { Text(bookmark.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingContent = {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            },
            trailingContent = {
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Open in new tab") }, onClick = { showMenu = false; onClick() }, leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) })
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) })
                    }
                }
            }
        )
    }
}

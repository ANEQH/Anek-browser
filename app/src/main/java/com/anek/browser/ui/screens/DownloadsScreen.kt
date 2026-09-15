package com.anek.browser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anek.browser.database.entity.DownloadEntity
import com.anek.browser.utils.shareText
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloads: List<DownloadEntity>,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } },
                actions = {
                    if (downloads.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (downloads.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))
                    Text("No downloads yet", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(downloads, key = { it.id }) { dl ->
                    DownloadItemRow(
                        download = dl,
                        onOpen = {
                            // Try to open file
                            try {
                                val file = File(dl.filePath ?: "")
                                if (file.exists()) {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, dl.mimeType ?: "*/*")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        onShare = { context.shareText(dl.url, "Share download") },
                        onDelete = { onDelete(dl.id) }
                    )
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear downloads?") },
                text = { Text("This will clear download history. Files will remain on device.") },
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

@Composable
fun DownloadItemRow(
    download: DownloadEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        ListItem(
            headlineContent = { Text(download.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Column {
                    Text(download.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${download.status} • ${java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date(download.timestamp))}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            leadingContent = {
                Icon(
                    when {
                        download.mimeType?.startsWith("image") == true -> Icons.Default.Face
                        download.mimeType?.startsWith("video") == true -> Icons.Default.Info
                        download.mimeType?.startsWith("audio") == true -> Icons.Default.Info
                        else -> Icons.Default.Info
                    },
                    contentDescription = null
                )
            },
            trailingContent = {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Open") }, onClick = { showMenu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Share") }, onClick = { showMenu = false; onShare() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
                }
            }
        )
    }
}

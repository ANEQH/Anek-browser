package com.anek.browser.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BrowserMenuSheet(
    isDesktopMode: Boolean,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onFindInPage: () -> Unit,
    onDesktopMode: () -> Unit,
    onShare: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onBookmarks: () -> Unit,
    onSettings: () -> Unit,
    onAddShortcut: () -> Unit,
    onOpenExternally: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MenuActionButton(Icons.Default.Star, if (isBookmarked) "Bookmarked" else "Bookmark", onBookmark)
            MenuActionButton(Icons.Default.Search, "Find", onFindInPage)
            MenuActionButton(Icons.Default.Settings, if (isDesktopMode) "Mobile" else "Desktop", onDesktopMode)
            MenuActionButton(Icons.Default.Share, "Share", onShare)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        DropdownMenuItem(
            text = { Text("Add to shortcuts") },
            onClick = onAddShortcut,
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Open externally") },
            onClick = onOpenExternally,
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Downloads") },
            onClick = onDownloads,
            leadingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("History") },
            onClick = onHistory,
            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Bookmarks") },
            onClick = onBookmarks,
            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        DropdownMenuItem(
            text = { Text("Settings") },
            onClick = onSettings,
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
        )
    }
}

@Composable
private fun MenuActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column {
        FilledTonalIconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

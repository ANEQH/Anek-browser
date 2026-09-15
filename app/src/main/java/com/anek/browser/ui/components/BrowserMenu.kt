package com.anek.browser.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    // --- v1.2.0 additions ---
    onNewTab: () -> Unit = {},
    onNewIncognitoTab: () -> Unit = {},
    onReload: () -> Unit = {},
    onRestoreClosedTab: () -> Unit = {},
    canRestoreClosedTab: Boolean = false,
    onDevTools: () -> Unit = {},
    devToolsEnabled: Boolean = false,
    onUserScripts: () -> Unit = {},
    blockedCount: Int = 0,
    adBlockEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Chrome-like quick actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MenuActionButton(Icons.Default.Add, "New tab", onNewTab)
            MenuActionButton(
                icon = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                label = if (isBookmarked) "Bookmarked" else "Bookmark",
                onClick = onBookmark,
                isHighlighted = isBookmarked
            )
            MenuActionButton(Icons.Default.Search, "Find", onFindInPage)
            MenuActionButton(
                Icons.Default.DesktopWindows,
                if (isDesktopMode) "Mobile" else "Desktop",
                onDesktopMode,
                isHighlighted = isDesktopMode
            )
            MenuActionButton(Icons.Default.Share, "Share", onShare)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        if (adBlockEnabled) {
            AssistChipRow(
                icon = Icons.Default.Shield,
                label = if (blockedCount > 0) "$blockedCount requests blocked" else "Ad & tracker blocking on"
            )
            Spacer(Modifier.height(8.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                DropdownMenuItem(
                    text = { Text("New tab") },
                    onClick = onNewTab,
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("New incognito tab") },
                    onClick = onNewIncognitoTab,
                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Reload") },
                    onClick = onReload,
                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                )
                if (canRestoreClosedTab) {
                    DropdownMenuItem(
                        text = { Text("Reopen closed tab") },
                        onClick = onRestoreClosedTab,
                        leadingIcon = { Icon(Icons.Default.Undo, contentDescription = null) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        DropdownMenuItem(
            text = { Text("Add to shortcuts") },
            onClick = onAddShortcut,
            leadingIcon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Open in other app") },
            onClick = onOpenExternally,
            leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) }
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        DropdownMenuItem(
            text = { Text("Downloads") },
            onClick = onDownloads,
            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("History") },
            onClick = onHistory,
            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Bookmarks") },
            onClick = onBookmarks,
            leadingIcon = { Icon(Icons.Default.Bookmarks, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("User scripts") },
            onClick = onUserScripts,
            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
            supportingText = { Text("Inject JavaScript into pages") }
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        DropdownMenuItem(
            text = { Text("Settings") },
            onClick = onSettings,
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
        )
        if (devToolsEnabled) {
            DropdownMenuItem(
                text = { Text("Developer options") },
                onClick = onDevTools,
                leadingIcon = { Icon(Icons.Default.BugReport, contentDescription = null) }
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Anek Browser • Fast & Secure",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AssistChipRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}

@Composable
private fun MenuActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isHighlighted: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Icon(icon, contentDescription = label)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1
        )
    }
}

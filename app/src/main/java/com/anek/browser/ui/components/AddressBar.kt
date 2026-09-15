package com.anek.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.anek.browser.browser.Tab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressBar(
    tab: Tab?,
    onNavigate: (String) -> Unit,
    onSearchFocused: () -> Unit = {},
    modifier: Modifier = Modifier,
    showLockIcon: Boolean = true
) {
    var text by remember(tab?.id) { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(tab?.url, tab?.id) {
        if (!isEditing) {
            text = if (tab?.isHomePage() == true) "" else tab?.url ?: ""
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* secure indicator */ }) {
                Icon(
                    imageVector = when {
                        tab?.isHomePage() == true -> Icons.Default.Home
                        tab?.url?.startsWith("https") == true -> Icons.Default.Lock
                        else -> Icons.Default.Search
                    },
                    contentDescription = "Secure",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextField(
                value = if (isEditing) text else (tab?.title?.takeIf { tab.isHomePage().not() }?.let { if (text.isBlank()) tab.url else text } ?: text),
                onValueChange = {
                    text = it
                    isEditing = true
                },
                placeholder = {
                    Text(
                        "Search or type web address",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (text.isNotBlank()) {
                            onNavigate(text)
                            isEditing = false
                            focusManager.clearFocus()
                        }
                    }
                )
            )

            if (isEditing && text.isNotBlank()) {
                IconButton(onClick = {
                    text = ""
                }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            } else {
                IconButton(onClick = {
                    onSearchFocused()
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        }
    }
}

@Composable
fun BrowserTopBar(
    tab: Tab?,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onHome: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    tabCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AddressBar(
            tab = tab,
            onNavigate = onNavigate
        )
        // Secondary toolbar if needed
    }
}

@Composable
fun BrowserBottomBar(
    tab: Tab?,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onHome: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    tabCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = tab?.canGoBack == true) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            IconButton(onClick = onForward, enabled = tab?.canGoForward == true) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
            }
            IconButton(onClick = onReload) {
                Icon(
                    if (tab?.isLoading == true) Icons.Default.Close else Icons.Default.Refresh,
                    contentDescription = "Reload"
                )
            }
            IconButton(onClick = onHome) {
                Icon(Icons.Default.Home, contentDescription = "Home")
            }
            BadgedBox(
                badge = {
                    if (tabCount > 0) {
                        Badge { Text("$tabCount") }
                    }
                }
            ) {
                IconButton(onClick = onTabsClick) {
                    Icon(Icons.Default.List, contentDescription = "Tabs")
                }
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
        }
    }
}

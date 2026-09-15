package com.anek.browser.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anek.browser.browser.OmniboxSuggestion
import com.anek.browser.browser.Tab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromeOmnibox(
    tab: Tab?,
    text: String,
    onTextChange: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    suggestions: List<OmniboxSuggestion>,
    onSuggestionClick: (OmniboxSuggestion) -> Unit,
    isFocused: Boolean,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
            shadowElevation = if (isFocused) 4.dp else 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = when {
                            tab?.isIncognito == true -> Icons.Default.Lock
                            tab?.isHomePage() == true -> Icons.Default.Search
                            tab?.url?.startsWith("https") == true -> Icons.Default.Lock
                            else -> Icons.Default.Info
                        },
                        contentDescription = "Secure",
                        tint = if (tab?.url?.startsWith("https") == true) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                TextField(
                    value = text,
                    onValueChange = {
                        onTextChange(it)
                    },
                    placeholder = {
                        Text(
                            if (tab?.isHomePage() == true) "Search or type web address" else tab?.host() ?: "Search or type web address",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { state ->
                            onFocusChange(state.isFocused)
                        },
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
                                focusManager.clearFocus()
                                onFocusChange(false)
                            }
                        }
                    )
                )

                AnimatedVisibility(
                    visible = text.isNotBlank(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(onClick = {
                        onTextChange("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                    }
                }

                if (text.isBlank() && !isFocused) {
                    IconButton(onClick = { /* voice search placeholder */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Suggestions dropdown - Chrome-like
        AnimatedVisibility(
            visible = isFocused && suggestions.isNotEmpty(),
            enter = fadeIn(tween(150)) + expandVertically(tween(200)),
            exit = fadeOut(tween(100)) + shrinkVertically(tween(150))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .heightIn(max = 400.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LazyColumn {
                    items(suggestions, key = { it.url + it.title }) { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            onClick = {
                                onSuggestionClick(suggestion)
                                focusManager.clearFocus()
                                onFocusChange(false)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(
    suggestion: OmniboxSuggestion,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                suggestion.title.ifBlank { suggestion.url },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        supportingContent = {
            if (suggestion.title != suggestion.url) {
                Text(
                    suggestion.url,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                when (suggestion) {
                    is OmniboxSuggestion.History -> Icons.Default.History
                    is OmniboxSuggestion.Bookmark -> Icons.Default.Star
                    is OmniboxSuggestion.Search -> Icons.Default.Search
                    is OmniboxSuggestion.Url -> Icons.Default.Language
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromeTopBar(
    tab: Tab?,
    omniboxText: String,
    onOmniboxTextChange: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    isOmniboxFocused: Boolean,
    suggestions: List<OmniboxSuggestion>,
    onSuggestionClick: (OmniboxSuggestion) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onHome: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    tabCount: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chrome-like omnibox takes most space, with tab counter on right
                Box(modifier = Modifier.weight(1f)) {
                    ChromeOmnibox(
                        tab = tab,
                        text = omniboxText,
                        onTextChange = onOmniboxTextChange,
                        onNavigate = onNavigate,
                        onFocusChange = onFocusChange,
                        suggestions = suggestions,
                        onSuggestionClick = onSuggestionClick,
                        isFocused = isOmniboxFocused
                    )
                }

                BadgedBox(
                    badge = {
                        if (tabCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    if (tabCount > 99) "99+" else "$tabCount",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    IconButton(onClick = onTabsClick) {
                        Icon(
                            Icons.Default.FilterNone,
                            contentDescription = "Tabs",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Secondary toolbar when not focused - Chrome-like back/forward etc
            AnimatedVisibility(
                visible = !isOmniboxFocused,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .height(40.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, enabled = canGoBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    IconButton(onClick = onForward, enabled = canGoForward) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                    }
                    IconButton(onClick = onReload) {
                        Icon(
                            if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (isLoading) "Stop" else "Reload"
                        )
                    }
                    IconButton(onClick = onHome) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                }
            }
        }
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
                    Icon(Icons.Default.FilterNone, contentDescription = "Tabs")
                }
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
        }
    }
}

// Legacy compatibility wrappers
@Composable
fun AddressBar(
    tab: Tab?,
    onNavigate: (String) -> Unit,
    onSearchFocused: () -> Unit = {},
    modifier: Modifier = Modifier,
    showLockIcon: Boolean = true
) {
    ChromeOmnibox(
        tab = tab,
        text = tab?.url ?: "",
        onTextChange = {},
        onNavigate = onNavigate,
        onFocusChange = { if (it) onSearchFocused() },
        suggestions = emptyList(),
        onSuggestionClick = {},
        isFocused = false,
        modifier = modifier
    )
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
    ChromeTopBar(
        tab = tab,
        omniboxText = tab?.url ?: "",
        onOmniboxTextChange = {},
        onNavigate = onNavigate,
        onFocusChange = {},
        isOmniboxFocused = false,
        suggestions = emptyList(),
        onSuggestionClick = {},
        onBack = onBack,
        onForward = onForward,
        onReload = onReload,
        onHome = onHome,
        onTabsClick = onTabsClick,
        onMenuClick = onMenuClick,
        tabCount = tabCount,
        canGoBack = tab?.canGoBack ?: false,
        canGoForward = tab?.canGoForward ?: false,
        isLoading = tab?.isLoading ?: false,
        modifier = modifier
    )
}

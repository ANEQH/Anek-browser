package com.anek.browser.ui.screens

import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anek.browser.browser.BrowserViewModel
import com.anek.browser.browser.Tab
import com.anek.browser.data.datastore.BrowserSettings
import com.anek.browser.ui.components.*
import com.anek.browser.utils.Constants
import com.anek.browser.utils.openUrlExternally
import com.anek.browser.utils.shareText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    tab: Tab?,
    settings: BrowserSettings,
    viewModel: BrowserViewModel,
    onNavigateHome: () -> Unit,
    onTabsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Omnibox state from ViewModel for Chrome-like behavior
    val omniboxText by viewModel.omniboxText.collectAsState()
    val isOmniboxFocused by viewModel.isOmniboxFocused.collectAsState()
    val suggestions by viewModel.omniboxSuggestions.collectAsState()
    val isFindActive by viewModel.isFindInPageActive.collectAsState()
    val findQuery by viewModel.findInPageQuery.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showAddShortcutDialog by remember { mutableStateOf(false) }
    var findMatch by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val isBookmarked by viewModel.isBookmarked(tab?.url ?: "").collectAsState(initial = false)

    // Sync omnibox with current tab when tab changes
    LaunchedEffect(tab?.id, tab?.url) {
        if (!isOmniboxFocused) {
            viewModel.setOmniboxText(if (tab?.isHomePage() == true) "" else tab?.url ?: "")
        }
    }

    BackHandler(enabled = isOmniboxFocused) {
        viewModel.setOmniboxFocused(false)
    }

    Scaffold(
        topBar = {
            ChromeTopBar(
                tab = tab,
                omniboxText = omniboxText,
                onOmniboxTextChange = { viewModel.setOmniboxText(it) },
                onNavigate = { input ->
                    val resolved = viewModel.resolveInput(input)
                    tab?.let { viewModel.updateTabUrl(it.id, resolved) } ?: viewModel.addTab(resolved)
                    viewModel.setOmniboxFocused(false)
                },
                onFocusChange = { focused -> viewModel.setOmniboxFocused(focused) },
                isOmniboxFocused = isOmniboxFocused,
                suggestions = suggestions,
                onSuggestionClick = { suggestion ->
                    val url = when (suggestion) {
                        is com.anek.browser.browser.OmniboxSuggestion.Search -> viewModel.resolveInput(suggestion.query)
                        else -> suggestion.url
                    }
                    tab?.let { viewModel.updateTabUrl(it.id, url) } ?: viewModel.addTab(url)
                },
                onBack = {
                    webViewInstance?.let { wv ->
                        if (wv.canGoBack()) wv.goBack()
                    }
                },
                onForward = {
                    webViewInstance?.let { wv ->
                        if (wv.canGoForward()) wv.goForward()
                    }
                },
                onReload = {
                    webViewInstance?.let { wv ->
                        if (tab?.isLoading == true) wv.stopLoading()
                        else wv.reload()
                    } ?: run {
                        tab?.let { viewModel.updateTabUrl(it.id, it.url) }
                    }
                },
                onHome = onNavigateHome,
                onTabsClick = onTabsClick,
                onMenuClick = { showMenu = true },
                tabCount = viewModel.tabs.collectAsState().value.size,
                canGoBack = tab?.canGoBack ?: false,
                canGoForward = tab?.canGoForward ?: false,
                isLoading = tab?.isLoading ?: false
            )
        },
        bottomBar = {
            Column {
                // Find in page bar - Chrome-like
                AnimatedVisibility(
                    visible = isFindActive,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    FindInPageBar(
                        query = findQuery,
                        match = findMatch,
                        onQueryChange = { viewModel.setFindInPageQuery(it) },
                        onClose = { viewModel.setFindInPageActive(false) },
                        onNext = { webViewInstance?.findNext(true) },
                        onPrev = { webViewInstance?.findNext(false) }
                    )
                }

                // Progress bar - Chrome-like thin line
                if (tab?.progress != null && tab.progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { tab.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Bottom toolbar only when omnibox not focused and not homepage
                AnimatedVisibility(
                    visible = !isOmniboxFocused && tab?.isHomePage() == false,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    BrowserBottomBar(
                        tab = tab,
                        onBack = { webViewInstance?.let { if (it.canGoBack()) it.goBack() } },
                        onForward = { webViewInstance?.let { if (it.canGoForward()) it.goForward() } },
                        onReload = {
                            webViewInstance?.let { wv ->
                                if (tab?.isLoading == true) wv.stopLoading() else wv.reload()
                            }
                        },
                        onHome = onNavigateHome,
                        onTabsClick = onTabsClick,
                        onMenuClick = { showMenu = true },
                        tabCount = viewModel.tabs.collectAsState().value.size
                    )
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (tab == null || tab.isHomePage()) {
                HomeScreen(
                    onNavigate = { input ->
                        val resolved = viewModel.resolveInput(input)
                        if (tab != null) viewModel.updateTabUrl(tab.id, resolved)
                        else viewModel.addTab(resolved)
                    },
                    onSearch = { query ->
                        val resolved = viewModel.resolveInput(query)
                        if (tab != null) viewModel.updateTabUrl(tab.id, resolved)
                        else viewModel.addTab(resolved)
                    },
                    shortcuts = viewModel.shortcuts.collectAsState().value,
                    recentHistory = viewModel.recentHistory.collectAsState().value,
                    bookmarks = viewModel.bookmarks.collectAsState().value,
                    showShortcuts = settings.showShortcuts,
                    showRecent = settings.showRecentSites,
                    showWallpaper = settings.showWallpaper,
                    onAddShortcut = { showAddShortcutDialog = true },
                    onShortcutClick = { sc ->
                        viewModel.updateTabUrl(tab?.id ?: "", sc.url, sc.title)
                    },
                    onShortcutLongPress = { sc ->
                        viewModel.deleteShortcut(sc.id)
                    },
                    onHistoryClick = { hist ->
                        viewModel.updateTabUrl(tab?.id ?: "", hist.url, hist.title)
                    },
                    onBookmarkClick = { bm ->
                        viewModel.updateTabUrl(tab?.id ?: "", bm.url, bm.title)
                    },
                    onOpenSettings = onSettingsClick
                )
            } else {
                WebViewComponent(
                    tab = tab,
                    browserSettings = settings,
                    onUrlChanged = { url ->
                        viewModel.updateTabUrl(tab.id, url)
                    },
                    onTitleChanged = { title ->
                        viewModel.updateTabTitle(tab.id, title)
                    },
                    onProgressChanged = { progress, loading ->
                        viewModel.updateTabProgress(tab.id, progress, loading)
                    },
                    onNavigationStateChanged = { canBack, canForward ->
                        viewModel.updateTabNavigationState(tab.id, canBack, canForward)
                    },
                    onPageFinished = { url ->
                        if (!tab.isIncognito) {
                            viewModel.addHistory(url, tab.title)
                        }
                    },
                    onRequestPermission = { perm, cb ->
                        // In real Chrome-like, show permission prompt
                        // For now, deny and show toast with option to allow in settings
                        cb(false)
                        Toast.makeText(context, "Permission $perm blocked. Allow in site settings.", Toast.LENGTH_SHORT).show()
                    },
                    findQuery = findQuery,
                    isFindActive = isFindActive,
                    onFindResult = { active, total ->
                        findMatch = active to total
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Chrome-like menu bottom sheet
        if (showMenu) {
            ModalBottomSheet(
                onDismissRequest = { showMenu = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                BrowserMenuSheet(
                    isDesktopMode = tab?.isDesktopMode ?: false,
                    isBookmarked = isBookmarked,
                    onBookmark = {
                        tab?.let {
                            if (isBookmarked) {
                                val bm = viewModel.bookmarks.value.find { b -> b.url == it.url }
                                bm?.let { b -> viewModel.deleteBookmark(b.id) }
                                Toast.makeText(context, "Bookmark removed", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addBookmark(it.url, it.title)
                                Toast.makeText(context, "Bookmarked", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showMenu = false
                    },
                    onFindInPage = {
                        viewModel.setFindInPageActive(true)
                        showMenu = false
                    },
                    onDesktopMode = {
                        tab?.let { viewModel.toggleDesktopMode(it.id) }
                        showMenu = false
                    },
                    onShare = {
                        tab?.let { context.shareText(it.url) }
                        showMenu = false
                    },
                    onDownloads = {
                        showMenu = false
                        onDownloadsClick()
                    },
                    onHistory = {
                        showMenu = false
                        onHistoryClick()
                    },
                    onBookmarks = {
                        showMenu = false
                        onBookmarksClick()
                    },
                    onSettings = {
                        showMenu = false
                        onSettingsClick()
                    },
                    onAddShortcut = {
                        showAddShortcutDialog = true
                        showMenu = false
                    },
                    onOpenExternally = {
                        tab?.let { context.openUrlExternally(it.url) }
                        showMenu = false
                    },
                    onClose = { showMenu = false }
                )
            }
        }

        if (showAddShortcutDialog) {
            var title by remember { mutableStateOf(tab?.title ?: "") }
            var url by remember { mutableStateOf(tab?.url ?: "") }
            AlertDialog(
                onDismissRequest = { showAddShortcutDialog = false },
                title = { Text("Add shortcut") },
                text = {
                    Column {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, singleLine = true)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (url.isNotBlank() && title.isNotBlank()) {
                            viewModel.addShortcut(url, title)
                        }
                        showAddShortcutDialog = false
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddShortcutDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun FindInPageBar(
    query: String,
    match: Pair<Int, Int>?,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Find in page") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            )
            if (match != null) {
                Text(
                    "${match.first + 1}/${match.second}",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            IconButton(onClick = onPrev) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous") }
            IconButton(onClick = onNext) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next") }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
    }
}

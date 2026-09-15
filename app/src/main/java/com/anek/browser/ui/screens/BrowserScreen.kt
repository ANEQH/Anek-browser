package com.anek.browser.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
    blockedCount: Int = 0,
    lastLoadMillis: Long = 0L,
    customBlockDomains: Set<String> = emptySet(),
    onNavigateHome: () -> Unit,
    onTabsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDevToolsClick: () -> Unit = {},
    onUserScriptsClick: () -> Unit = {},
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

    // --- Site permission prompts -------------------------------------------
    // Previously every WebView permission request was denied outright, so
    // camera, microphone and geolocation never worked on any site.
    var pendingPermissionCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        pendingPermissionCallback?.invoke(result.values.isNotEmpty() && result.values.all { it })
        pendingPermissionCallback = null
    }

    val requestWebPermission: (String, (Boolean) -> Unit) -> Unit = { kind, cb ->
        val needed: Array<String> = when (kind) {
            "camera" -> arrayOf(Manifest.permission.CAMERA)
            "mic" -> arrayOf(Manifest.permission.RECORD_AUDIO)
            "camera_mic" -> arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            "geolocation" -> arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            else -> emptyArray()
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        when {
            needed.isEmpty() -> {
                cb(false)
                Toast.makeText(context, "$kind is not supported", Toast.LENGTH_SHORT).show()
            }
            missing.isEmpty() -> cb(true)
            else -> {
                pendingPermissionCallback = cb
                permissionLauncher.launch(missing.toTypedArray())
            }
        }
    }

    val isBookmarked by viewModel.isBookmarked(tab?.url ?: "").collectAsState(initial = false)
    val closedTabs by viewModel.closedTabs.collectAsState()

    // Sync omnibox with current tab when tab changes
    LaunchedEffect(tab?.id, tab?.url) {
        if (!isOmniboxFocused) {
            viewModel.setOmniboxText(if (tab?.isHomePage() == true) "" else tab?.url ?: "")
        }
    }

    BackHandler(enabled = isOmniboxFocused) {
        viewModel.setOmniboxFocused(false)
    }

    val toolbarAtBottom = settings.toolbarPosition.equals("BOTTOM", ignoreCase = true)

    /*
     * One definition of the omnibox + navigation cluster, dockable at the top or
     * the bottom. Previously ChromeTopBar and BrowserBottomBar *both* rendered
     * back / forward / reload / home / menu, which wasted about 56dp of a phone
     * screen and ignored the persisted toolbarPosition setting completely.
     */
    val omnibar: @Composable (Modifier) -> Unit = { chromeModifier ->
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
                isLoading = tab?.isLoading ?: false,
                modifier = chromeModifier
        )
    }

    Scaffold(
        topBar = {
            if (!toolbarAtBottom) omnibar(Modifier.statusBarsPadding())
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

                // Bottom-docked omnibox + navigation (thumb-reachable, Via style).
                if (toolbarAtBottom) {
                    omnibar(Modifier.navigationBarsPadding())
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
                        requestWebPermission(perm, cb)
                    },
                    findQuery = findQuery,
                    isFindActive = isFindActive,
                    onFindResult = { active, total ->
                        findMatch = active to total
                    },
                    // Without this the WebView reference stayed null forever, so
                    // Back / Forward / Reload / Find-next did nothing at all.
                    onWebViewReady = { wv ->
                        webViewInstance = wv
                        viewModel.setActiveWebView(wv)
                    },
                    onLoadStarted = { viewModel.onPageLoadStarted() },
                    onLoadFinished = { viewModel.onPageLoadFinished() },
                    onOpenNewTab = { url -> viewModel.addTab(url) },
                    customBlockDomains = customBlockDomains,
                    scriptEngine = viewModel.scriptEngine,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Developer-options performance overlay.
            if (settings.showPerfOverlay && tab?.isHomePage() == false) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = MaterialTheme.shapes.extraSmall,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        buildString {
                            if (lastLoadMillis > 0) append("${lastLoadMillis}ms")
                            else append("—")
                            if (blockedCount > 0) append("  •  $blockedCount blocked")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
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
                    onNewTab = {
                        viewModel.addTab()
                        showMenu = false
                    },
                    onNewIncognitoTab = {
                        viewModel.addPrivateTab()
                        showMenu = false
                    },
                    onReload = {
                        webViewInstance?.let { wv ->
                            if (tab?.isLoading == true) wv.stopLoading() else wv.reload()
                        }
                        showMenu = false
                    },
                    onRestoreClosedTab = {
                        viewModel.restoreLastClosedTab()
                        showMenu = false
                    },
                    canRestoreClosedTab = closedTabs.isNotEmpty(),
                    onDevTools = {
                        showMenu = false
                        onDevToolsClick()
                    },
                    devToolsEnabled = settings.devToolsEnabled,
                    onUserScripts = {
                        showMenu = false
                        onUserScriptsClick()
                    },
                    blockedCount = blockedCount,
                    adBlockEnabled = settings.adBlockEnabled,
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

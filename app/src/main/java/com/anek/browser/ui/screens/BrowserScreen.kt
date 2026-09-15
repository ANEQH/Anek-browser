package com.anek.browser.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.anek.browser.browser.BrowserViewModel
import com.anek.browser.browser.Tab
import com.anek.browser.data.datastore.BrowserSettings
import com.anek.browser.ui.components.*
import com.anek.browser.utils.Constants
import com.anek.browser.utils.openUrlExternally
import com.anek.browser.utils.shareText

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
    var addressText by remember(tab?.id, tab?.url) {
        mutableStateOf(if (tab?.isHomePage() == true) "" else tab?.url ?: "")
    }
    var isEditing by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAddShortcutDialog by remember { mutableStateOf(false) }
    var findMatch by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val isBookmarked by viewModel.isBookmarked(tab?.url ?: "").collectAsState(initial = false)

    // Handle back/forward from WebView via state? We'll use callbacks from WebViewComponent
    // For simplicity, we store webView canGoBack etc in Tab via ViewModel

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Address bar integrated
                    OutlinedTextField(
                        value = if (isEditing) addressText else (tab?.url?.takeIf { !tab.isHomePage() } ?: ""),
                        onValueChange = {
                            addressText = it
                            isEditing = true
                        },
                        placeholder = { Text("Search or type web address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                when {
                                    tab?.isHomePage() == true -> Icons.Default.Home
                                    tab?.url?.startsWith("https") == true -> Icons.Default.Lock
                                    else -> Icons.Default.Search
                                },
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (isEditing && addressText.isNotBlank()) {
                                IconButton(onClick = { addressText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            } else if (tab?.isLoading == true) {
                                IconButton(onClick = { /* stop */ }) {
                                    Icon(Icons.Default.Close, contentDescription = "Stop")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                if (addressText.isNotBlank()) {
                                    val resolved = viewModel.resolveInput(addressText)
                                    viewModel.updateTabUrl(tab?.id ?: "", resolved)
                                    isEditing = false
                                }
                            }
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    )
                },
                navigationIcon = {
                    if (tab?.canGoBack == true) {
                        IconButton(onClick = {
                            // Will be handled by WebView goBack via callback? For now we need to trigger via side effect
                            // We use a hack: update tab url? Better: expose webView controller via state
                            // For this version, we keep simple and rely on WebView's internal handling via back press in component? 
                            // We'll implement via ViewModel event bus later
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (tab?.isLoading == true) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (viewModel.isFindInPageActive.collectAsState().value) {
                    FindInPageBar(
                        query = viewModel.findInPageQuery.collectAsState().value,
                        match = findMatch,
                        onQueryChange = { viewModel.setFindInPageQuery(it) },
                        onClose = { viewModel.setFindInPageActive(false) },
                        onNext = { /* handled inside WebView via findNext */ },
                        onPrev = { }
                    )
                }

                if (tab?.progress != null && tab.progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { tab.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                BrowserBottomBar(
                    tab = tab,
                    onBack = { /* webView goBack */ },
                    onForward = { /* webView goForward */ },
                    onReload = {
                        if (tab?.isLoading == true) {
                            // stop
                        } else {
                            tab?.let { viewModel.updateTabUrl(it.id, it.url) }
                        }
                    },
                    onHome = onNavigateHome,
                    onTabsClick = onTabsClick,
                    onMenuClick = { showMenu = true },
                    tabCount = viewModel.tabs.collectAsState().value.size
                )
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
                        // For simplicity auto-deny, or could request
                        cb(false)
                        Toast.makeText(context, "Permission $perm denied (manage in settings)", Toast.LENGTH_SHORT).show()
                    },
                    findQuery = viewModel.findInPageQuery.collectAsState().value,
                    isFindActive = viewModel.isFindInPageActive.collectAsState().value,
                    onFindResult = { active, total ->
                        findMatch = active to total
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showMenu) {
            ModalBottomSheet(onDismissRequest = { showMenu = false }) {
                BrowserMenuSheet(
                    isDesktopMode = tab?.isDesktopMode ?: false,
                    isBookmarked = isBookmarked,
                    onBookmark = {
                        tab?.let {
                            if (isBookmarked) {
                                // remove
                                val bm = viewModel.bookmarks.value.find { b -> b.url == it.url }
                                bm?.let { b -> viewModel.deleteBookmark(b.id) }
                            } else {
                                viewModel.addBookmark(it.url, it.title)
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Find in page") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            if (match != null) {
                Text("${match.first + 1}/${match.second}", modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onPrev) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Prev") }
            IconButton(onClick = onNext) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next") }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
    }
}

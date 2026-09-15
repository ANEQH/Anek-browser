package com.anek.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anek.browser.browser.BrowserViewModel
import com.anek.browser.ui.screens.*
import com.anek.browser.ui.theme.AnekBrowserTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val isDark = isSystemInDarkTheme()

            AnekBrowserTheme(
                darkTheme = isDark,
                themeSetting = settings.theme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnekBrowserNavHost(viewModel = viewModel)
                }
            }
        }
    }
}

enum class Screen {
    BROWSER, TABS, HISTORY, BOOKMARKS, DOWNLOADS, SETTINGS
}

@Composable
fun AnekBrowserNavHost(viewModel: BrowserViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.BROWSER) }

    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val currentTabId by viewModel.currentTabId.collectAsStateWithLifecycle()
    val currentTab = tabs.find { it.id == currentTabId } ?: tabs.firstOrNull()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    when (currentScreen) {
        Screen.BROWSER -> {
            BrowserScreen(
                tab = currentTab,
                settings = settings,
                viewModel = viewModel,
                onNavigateHome = {
                    currentTab?.let { tab ->
                        viewModel.updateTabUrl(tab.id, com.anek.browser.utils.Constants.HOME_PAGE_URL, "Home")
                    }
                },
                onTabsClick = { currentScreen = Screen.TABS },
                onHistoryClick = { currentScreen = Screen.HISTORY },
                onBookmarksClick = { currentScreen = Screen.BOOKMARKS },
                onDownloadsClick = { currentScreen = Screen.DOWNLOADS },
                onSettingsClick = { currentScreen = Screen.SETTINGS }
            )
        }
        Screen.TABS -> {
            TabsScreen(
                tabs = tabs,
                currentTabId = currentTabId,
                onTabSelected = { id ->
                    viewModel.selectTab(id)
                    currentScreen = Screen.BROWSER
                },
                onTabClosed = { id -> viewModel.closeTab(id) },
                onNewTab = {
                    viewModel.addTab()
                    currentScreen = Screen.BROWSER
                },
                onNewPrivateTab = {
                    viewModel.addPrivateTab()
                    currentScreen = Screen.BROWSER
                },
                onCloseAll = { viewModel.closeAllTabs() },
                onDuplicate = { id ->
                    viewModel.duplicateTab(id)
                    currentScreen = Screen.BROWSER
                },
                onBack = { currentScreen = Screen.BROWSER }
            )
        }
        Screen.HISTORY -> {
            HistoryScreen(
                history = history,
                onItemClick = { item ->
                    currentTab?.let { tab ->
                        viewModel.updateTabUrl(tab.id, item.url, item.title)
                    } ?: viewModel.addTab(item.url)
                    currentScreen = Screen.BROWSER
                },
                onDeleteItem = { item -> viewModel.deleteHistoryItem(item.id) },
                onClearAll = { viewModel.clearAllHistory() },
                onBack = { currentScreen = Screen.BROWSER }
            )
        }
        Screen.BOOKMARKS -> {
            BookmarksScreen(
                bookmarks = bookmarks,
                onItemClick = { bm ->
                    currentTab?.let { tab ->
                        viewModel.updateTabUrl(tab.id, bm.url, bm.title)
                    } ?: viewModel.addTab(bm.url)
                    currentScreen = Screen.BROWSER
                },
                onDelete = { bm -> viewModel.deleteBookmark(bm.id) },
                onEdit = { bm -> viewModel.updateBookmark(bm) },
                onExport = { },
                onBack = { currentScreen = Screen.BROWSER }
            )
        }
        Screen.DOWNLOADS -> {
            DownloadsScreen(
                downloads = downloads,
                onDelete = { },
                onClearAll = { viewModel.clearDownloads() },
                onBack = { currentScreen = Screen.BROWSER }
            )
        }
        Screen.SETTINGS -> {
            SettingsScreen(
                settings = settings,
                onUpdateSearchEngine = { engine -> viewModel.updateSearchEngine(engine) },
                onUpdateTheme = { theme -> viewModel.updateTheme(theme) },
                onUpdateJavaScript = { enabled -> viewModel.updateJavaScriptEnabled(enabled) },
                onUpdateBlockThirdParty = { block -> viewModel.updateBlockThirdPartyCookies(block) },
                onUpdateDoNotTrack = { enabled -> viewModel.updateDoNotTrack(enabled) },
                onUpdateDesktopMode = { enabled -> viewModel.updateDesktopMode(enabled) },
                onUpdateTextScaling = { scaling -> viewModel.updateTextScaling(scaling) },
                onUpdateShowShortcuts = { show -> viewModel.updateShowShortcuts(show) },
                onUpdateShowRecent = { show -> viewModel.updateShowRecentSites(show) },
                onUpdateSafeBrowsing = { enabled -> viewModel.updateSafeBrowsing(enabled) },
                onUpdateHomepage = { url -> viewModel.updateHomepage(url) },
                onUpdateToolbarPosition = { pos -> viewModel.updateToolbarPosition(pos) },
                onClearCookies = { viewModel.clearCookies() },
                onClearCache = { viewModel.clearCache() },
                onClearWebStorage = { viewModel.clearWebStorage() },
                onClearAllData = { viewModel.clearAllBrowsingData() },
                onResetSettings = { viewModel.resetSettings() },
                onBack = { currentScreen = Screen.BROWSER }
            )
        }
    }
}

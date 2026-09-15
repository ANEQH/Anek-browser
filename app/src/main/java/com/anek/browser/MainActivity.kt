package com.anek.browser

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.anek.browser.browser.BrowserViewModel
import com.anek.browser.ui.screens.*
import com.anek.browser.ui.theme.AnekBrowserTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Honour the persisted preference. This used to be hard-coded to `true`
        // in AnekBrowserApp.onCreate, which left every release build inspectable
        // over USB — see Developer options.
        lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                viewModel.applyRemoteDebugging(settings.remoteDebugging)
            }
        }

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

        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Opens links sent by other apps. The manifest declared VIEW / WEB_SEARCH
     * intent filters but nothing consumed them, so tapping a link while the
     * browser was already open silently did nothing.
     */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val raw = when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEARCH, Intent.ACTION_WEB_SEARCH ->
                intent.getStringExtra(SearchManager.QUERY) ?: intent.dataString
            else -> null
        }
        if (raw.isNullOrBlank()) return

        // Ignore our own internal home pseudo-URL.
        if (raw == com.anek.browser.utils.Constants.HOME_PAGE_URL) return

        val resolved = try {
            viewModel.resolveInput(raw)
        } catch (_: Exception) {
            raw
        }
        viewModel.addTab(resolved, select = true)
    }
}

enum class Screen {
    BROWSER, TABS, HISTORY, BOOKMARKS, DOWNLOADS, SETTINGS, DEVTOOLS, USERSCRIPTS
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
    val bookmarkFolders by viewModel.bookmarkFolders.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val userScripts by viewModel.userScripts.collectAsStateWithLifecycle()
    val console by viewModel.consoleEntries.collectAsStateWithLifecycle()
    val pageSource by viewModel.pageSource.collectAsStateWithLifecycle()
    val blockedCount by viewModel.blockedCount.collectAsStateWithLifecycle()
    val lastLoadMillis by viewModel.lastLoadMillis.collectAsStateWithLifecycle()
    val customBlockDomains by viewModel.customBlockDomains.collectAsStateWithLifecycle()

    /*
     * BrowserScreen is composed unconditionally and the other screens are drawn
     * on top of it.
     *
     * The previous `when (currentScreen) { ... }` removed BrowserScreen from the
     * composition entirely, so opening Settings, Tabs or History destroyed the
     * WebView and every page had to reload from scratch on the way back. Keeping
     * it mounted is what makes navigation feel instant.
     */
    Box(modifier = Modifier.fillMaxSize()) {

        BrowserScreen(
            tab = currentTab,
            settings = settings,
            viewModel = viewModel,
            blockedCount = blockedCount,
            lastLoadMillis = lastLoadMillis,
            customBlockDomains = customBlockDomains,
            onNavigateHome = {
                currentTab?.let { tab ->
                    viewModel.updateTabUrl(
                        tab.id,
                        com.anek.browser.utils.Constants.HOME_PAGE_URL,
                        "Home"
                    )
                }
            },
            onTabsClick = { currentScreen = Screen.TABS },
            onHistoryClick = { currentScreen = Screen.HISTORY },
            onBookmarksClick = { currentScreen = Screen.BOOKMARKS },
            onDownloadsClick = { currentScreen = Screen.DOWNLOADS },
            onSettingsClick = { currentScreen = Screen.SETTINGS },
            onDevToolsClick = { currentScreen = Screen.DEVTOOLS },
            onUserScriptsClick = { currentScreen = Screen.USERSCRIPTS }
        )

        if (currentScreen != Screen.BROWSER) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when (currentScreen) {
                    Screen.TABS -> TabsScreen(
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

                    Screen.HISTORY -> HistoryScreen(
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

                    Screen.BOOKMARKS -> BookmarksScreen(
                        bookmarks = bookmarks,
                        folders = bookmarkFolders,
                        onItemClick = { bm ->
                            currentTab?.let { tab ->
                                viewModel.updateTabUrl(tab.id, bm.url, bm.title)
                            } ?: viewModel.addTab(bm.url)
                            currentScreen = Screen.BROWSER
                        },
                        onDelete = { bm -> viewModel.deleteBookmark(bm.id) },
                        onEdit = { bm -> viewModel.updateBookmark(bm) },
                        onCreateFolder = { name -> viewModel.addBookmarkFolder(name) },
                        onExport = { },
                        onBack = { currentScreen = Screen.BROWSER }
                    )

                    Screen.DOWNLOADS -> DownloadsScreen(
                        downloads = downloads,
                        onDelete = { },
                        onClearAll = { viewModel.clearDownloads() },
                        onBack = { currentScreen = Screen.BROWSER }
                    )

                    Screen.SETTINGS -> SettingsScreen(
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
                        onUpdateSuggestions = { enabled -> viewModel.updateSuggestionsEnabled(enabled) },
                        onUpdateTabRestore = { enabled -> viewModel.updateTabRestoreEnabled(enabled) },
                        onUpdateImages = { enabled -> viewModel.updateImagesEnabled(enabled) },
                        onUpdateDataSaver = { enabled -> viewModel.updateDataSaver(enabled) },
                        onUpdateAdBlock = { enabled -> viewModel.updateAdBlockEnabled(enabled) },
                        onUpdateTrackerBlock = { enabled -> viewModel.updateTrackerBlockEnabled(enabled) },
                        onUpdateFullscreenVideo = { enabled -> viewModel.updateFullscreenVideo(enabled) },
                        onUpdateKeepScreenOn = { enabled -> viewModel.updateKeepScreenOnVideo(enabled) },
                        onUpdateBackgroundAudio = { enabled -> viewModel.updateBackgroundAudio(enabled) },
                        onUpdateForceDarkWeb = { enabled -> viewModel.updateForceDarkWebContent(enabled) },
                        onUpdateUserScripts = { enabled -> viewModel.updateUserScriptsEnabled(enabled) },
                        onUpdateStartup = { behavior -> viewModel.updateStartupBehavior(behavior) },
                        onUpdateHomepageSetting = { url -> viewModel.updateHomepage(url) },
                        onUpdateUserAgent = { ua -> viewModel.updateCustomUserAgent(ua) },
                        onUpdateDevTools = { enabled -> viewModel.updateDevToolsEnabled(enabled) },
                        blockedCount = blockedCount,
                        onClearCookies = { viewModel.clearCookies() },
                        onClearCache = { viewModel.clearCache() },
                        onClearWebStorage = { viewModel.clearWebStorage() },
                        onClearAllData = { viewModel.clearAllBrowsingData() },
                        onResetSettings = { viewModel.resetSettings() },
                        onOpenUserScripts = { currentScreen = Screen.USERSCRIPTS },
                        onOpenDevTools = { currentScreen = Screen.DEVTOOLS },
                        onBack = { currentScreen = Screen.BROWSER }
                    )

                    Screen.DEVTOOLS -> DevToolsScreen(
                        settings = settings,
                        console = console,
                        pageSource = pageSource,
                        actions = DevToolsActions(
                            onDevToolsEnabled = viewModel::updateDevToolsEnabled,
                            onRemoteDebugging = viewModel::updateRemoteDebugging,
                            onCaptureConsole = viewModel::updateCaptureConsole,
                            onShowPerfOverlay = viewModel::updateShowPerfOverlay,
                            onImagesEnabled = viewModel::updateImagesEnabled,
                            onDataSaver = viewModel::updateDataSaver,
                            onPrefetch = viewModel::updatePrefetchEnabled,
                            onAdBlock = viewModel::updateAdBlockEnabled,
                            onTrackerBlock = viewModel::updateTrackerBlockEnabled,
                            onForceDarkWeb = viewModel::updateForceDarkWebContent,
                            onCustomUserAgent = viewModel::updateCustomUserAgent,
                            onCustomBlocklist = viewModel::updateCustomBlocklist,
                            onClearCache = viewModel::clearCache,
                            onClearConsole = viewModel::clearConsole,
                            onViewSource = viewModel::capturePageSource,
                            onHardReload = viewModel::hardReload,
                            onRunJs = viewModel::runJavaScript,
                            onOpenDevToolsUrl = { url ->
                                currentTab?.let { tab -> viewModel.updateTabUrl(tab.id, url) }
                                    ?: viewModel.addTab(url)
                                currentScreen = Screen.BROWSER
                            }
                        ),
                        onBack = {
                            viewModel.clearPageSource()
                            currentScreen = Screen.BROWSER
                        }
                    )

                    Screen.USERSCRIPTS -> UserScriptsScreen(
                        scripts = userScripts,
                        globalEnabled = settings.userscriptsEnabled,
                        onGlobalEnabledChange = viewModel::updateUserScriptsEnabled,
                        onSave = viewModel::saveUserScript,
                        onDelete = viewModel::deleteUserScript,
                        onToggle = viewModel::setUserScriptEnabled,
                        onBack = { currentScreen = Screen.BROWSER }
                    )

                    Screen.BROWSER -> Unit
                }
            }
        }
    }
}

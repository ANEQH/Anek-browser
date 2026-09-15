package com.anek.browser.browser

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anek.browser.data.datastore.BrowserSettings
import com.anek.browser.data.datastore.SettingsRepository
import com.anek.browser.database.AppDatabase
import com.anek.browser.database.entity.BookmarkEntity
import com.anek.browser.database.entity.HistoryEntity
import com.anek.browser.database.entity.ShortcutEntity
import com.anek.browser.database.entity.UserScriptEntity
import com.anek.browser.utils.Constants
import com.anek.browser.web.AdBlocker
import com.anek.browser.web.ConsoleEntry
import com.anek.browser.web.ConsoleLog
import com.anek.browser.web.UserScriptEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val settingsRepo = SettingsRepository(application)

    private val _tabs = MutableStateFlow<List<Tab>>(listOf(Tab()))
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()

    private val _currentTabId = MutableStateFlow<String>(_tabs.value.first().id)
    val currentTabId: StateFlow<String> = _currentTabId.asStateFlow()

    private val _closedTabs = MutableStateFlow<List<ClosedTab>>(emptyList())
    val closedTabs: StateFlow<List<ClosedTab>> = _closedTabs.asStateFlow()

    private val _findInPageQuery = MutableStateFlow("")
    val findInPageQuery: StateFlow<String> = _findInPageQuery.asStateFlow()

    private val _isFindInPageActive = MutableStateFlow(false)
    val isFindInPageActive: StateFlow<Boolean> = _isFindInPageActive.asStateFlow()

    private val _omniboxText = MutableStateFlow("")
    val omniboxText: StateFlow<String> = _omniboxText.asStateFlow()

    private val _isOmniboxFocused = MutableStateFlow(false)
    val isOmniboxFocused: StateFlow<Boolean> = _isOmniboxFocused.asStateFlow()

    val currentTab: StateFlow<Tab?> = combine(_tabs, _currentTabId) { tabs, currentId ->
        tabs.find { it.id == currentId } ?: tabs.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _tabs.value.firstOrNull())

    val settings: StateFlow<BrowserSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, BrowserSettings())

    val history: StateFlow<List<HistoryEntity>> = db.historyDao().getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory: StateFlow<List<HistoryEntity>> = db.historyDao().getRecentHistory(Constants.RECENT_HISTORY_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = db.bookmarkDao().getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkFolders = db.bookmarkDao().getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortcuts: StateFlow<List<ShortcutEntity>> = db.shortcutDao().getAllShortcuts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads = db.downloadDao().getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- User scripts (the Android stand-in for extensions) ----------------
    val userScripts: StateFlow<List<UserScriptEntity>> = db.userScriptDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val userScriptEngine = UserScriptEngine(db.userScriptDao(), viewModelScope)

    /** Exposed so WebViewComponent can inject without touching Room itself. */
    val scriptEngine: UserScriptEngine get() = userScriptEngine

    // --- Developer options -------------------------------------------------
    val consoleEntries: StateFlow<List<ConsoleEntry>> = ConsoleLog.entries

    private val _pageSource = MutableStateFlow<String?>(null)
    val pageSource: StateFlow<String?> = _pageSource.asStateFlow()

    private val _blockedCount = MutableStateFlow(0)
    val blockedCount: StateFlow<Int> = _blockedCount.asStateFlow()

    /** Last completed page load duration in ms (0 while loading). */
    private val _lastLoadMillis = MutableStateFlow(0L)
    val lastLoadMillis: StateFlow<Long> = _lastLoadMillis.asStateFlow()

    private var loadStartNanos = 0L

    /**
     * The WebView currently on screen. Held weakly: the view is owned by the
     * composition and must not be leaked by the ViewModel.
     */
    private var activeWebViewRef = WeakReference<WebView>(null)

    fun setActiveWebView(webView: WebView?) {
        activeWebViewRef = WeakReference(webView)
    }

    fun activeWebView(): WebView? = activeWebViewRef.get()

    // Omnibox suggestions combining history + bookmarks + search
    val omniboxSuggestions: StateFlow<List<OmniboxSuggestion>> = combine(
        _omniboxText,
        history,
        bookmarks,
        settings
    ) { text, hist, bms, sett ->
        if (text.isBlank() || !sett.suggestionsEnabled || _isOmniboxFocused.value.not()) emptyList()
        else generateSuggestions(text, hist, bms)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            /*
             * 1. Restore persisted tabs BEFORE the auto-saver starts.
             *
             * The previous implementation ran both collectors concurrently and
             * left the restore collector running forever, so every write from
             * the auto-saver re-triggered a restore. On a cold start that also
             * raced: the saver could persist the default single empty tab and
             * then the restore would overwrite live tabs with stale data.
             */
            if (settings.value.tabRestoreEnabled) {
                try {
                    val (persistedTabs, currentId) = settingsRepo.tabsFlow.first()
                    val restored = persistedTabs
                        .filterNot { it.isIncognito } // never restore incognito
                        .map { it.toTab() }
                        .take(Constants.MAX_TABS)
                    if (restored.isNotEmpty()) {
                        _tabs.value = restored
                        _currentTabId.value =
                            currentId?.takeIf { id -> restored.any { it.id == id } }
                                ?: restored.last().id
                    }
                } catch (_: Exception) { /* keep the default tab */ }
            }

            // 2. Persist on every change from here on.
            combine(_tabs, _currentTabId) { tabs, currentId -> tabs to currentId }
                .collect { (tabs, currentId) ->
                    withContext(Dispatchers.IO) {
                        val toPersist = tabs
                            .filterNot { it.isIncognito }
                            .map { PersistedTab.fromTab(it) }
                        settingsRepo.saveTabs(toPersist, currentId)
                    }
                }
        }

        userScriptEngine.startCaching()

        // Publish the blocked-request counter on a slow tick. AdBlocker mutates
        // an AtomicInteger from the network thread; polling once a second keeps
        // recomposition off the hot path entirely.
        viewModelScope.launch {
            while (true) {
                _blockedCount.value = AdBlocker.blockedCount.get()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun generateSuggestions(input: String, hist: List<HistoryEntity>, bms: List<BookmarkEntity>): List<OmniboxSuggestion> {
        val lower = input.lowercase()
        val suggestions = mutableListOf<OmniboxSuggestion>()

        // History suggestions
        hist.filter { it.url.contains(lower, true) || it.title.contains(lower, true) }
            .take(4)
            .forEach {
                suggestions.add(OmniboxSuggestion.History(it.title, it.url))
            }

        // Bookmark suggestions
        bms.filter { it.url.contains(lower, true) || it.title.contains(lower, true) }
            .take(3)
            .forEach {
                suggestions.add(OmniboxSuggestion.Bookmark(it.title, it.url))
            }

        // Search suggestion
        if (input.isNotBlank() && !UrlUtils.isValidUrl(input)) {
            suggestions.add(0, OmniboxSuggestion.Search(input))
        }

        // URL suggestion if valid
        if (UrlUtils.isValidUrl(input)) {
            suggestions.add(0, OmniboxSuggestion.Url(UrlUtils.normalizeUrl(input)))
        }

        return suggestions.distinctBy { it.url }.take(8)
    }

    // Omnibox
    fun setOmniboxText(text: String) { _omniboxText.value = text }
    fun setOmniboxFocused(focused: Boolean) { _isOmniboxFocused.value = focused }

    // Tab operations - Chrome-like
    fun addTab(url: String = Constants.HOME_PAGE_URL, isIncognito: Boolean = false, select: Boolean = true) {
        if (_tabs.value.size >= Constants.MAX_TABS) return
        val newTab = Tab(
            url = url,
            title = if (url == Constants.HOME_PAGE_URL) Constants.HOME_PAGE_TITLE else url,
            isIncognito = isIncognito || settings.value.incognitoDefault,
            isDesktopMode = settings.value.desktopMode
        )
        _tabs.value = _tabs.value + newTab
        if (select) {
            _currentTabId.value = newTab.id
            _omniboxText.value = if (newTab.isHomePage()) "" else newTab.url
        }
    }

    fun addPrivateTab() {
        addTab(isIncognito = true)
    }

    fun selectTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _currentTabId.value = tabId
            updateTabLastAccessed(tabId)
            currentTab.value?.let { tab ->
                _omniboxText.value = if (tab.isHomePage()) "" else tab.url
            }
        }
    }

    fun closeTab(tabId: String) {
        val tabToClose = _tabs.value.find { it.id == tabId } ?: return
        _closedTabs.value = (listOf(ClosedTab(tabToClose)) + _closedTabs.value).take(20)

        val newTabs = _tabs.value.filterNot { it.id == tabId }
        _tabs.value = if (newTabs.isEmpty()) {
            listOf(Tab(isIncognito = tabToClose.isIncognito))
        } else {
            newTabs
        }

        if (_currentTabId.value == tabId) {
            _currentTabId.value = _tabs.value.last().id
        }
    }

    fun closeAllTabs(includePrivate: Boolean = true) {
        val toClose = if (includePrivate) _tabs.value else _tabs.value.filterNot { it.isIncognito }
        if (toClose.isNotEmpty()) {
            _closedTabs.value = (toClose.map { ClosedTab(it) } + _closedTabs.value).take(20)
        }
        val remaining = if (includePrivate) emptyList() else _tabs.value.filter { it.isIncognito }
        _tabs.value = if (remaining.isEmpty()) listOf(Tab()) else remaining
        _currentTabId.value = _tabs.value.first().id
    }

    fun closeOtherTabs(keepId: String) {
        val keep = _tabs.value.find { it.id == keepId } ?: return
        val toClose = _tabs.value.filterNot { it.id == keepId }
        _closedTabs.value = (toClose.map { ClosedTab(it) } + _closedTabs.value).take(20)
        _tabs.value = listOf(keep)
        _currentTabId.value = keep.id
    }

    fun duplicateTab(tabId: String) {
        val original = _tabs.value.find { it.id == tabId } ?: return
        if (_tabs.value.size >= Constants.MAX_TABS) return
        val duplicate = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            lastAccessed = System.currentTimeMillis()
        )
        _tabs.value = _tabs.value + duplicate
        _currentTabId.value = duplicate.id
    }

    fun restoreLastClosedTab() {
        val last = _closedTabs.value.firstOrNull() ?: return
        if (_tabs.value.size >= Constants.MAX_TABS) return
        _tabs.value = _tabs.value + last.tab.copy(lastAccessed = System.currentTimeMillis())
        _currentTabId.value = last.tab.id
        _closedTabs.value = _closedTabs.value.drop(1)
    }

    fun reopenClosedTab(closedTab: ClosedTab) {
        if (_tabs.value.size >= Constants.MAX_TABS) return
        _tabs.value = _tabs.value + closedTab.tab.copy(lastAccessed = System.currentTimeMillis())
        _currentTabId.value = closedTab.tab.id
        _closedTabs.value = _closedTabs.value.filterNot { it.tab.id == closedTab.tab.id }
    }

    fun updateTabUrl(tabId: String, url: String, title: String = url) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(url = url, title = title, lastAccessed = System.currentTimeMillis())
            else it
        }
        if (tabId == _currentTabId.value) {
            _omniboxText.value = if (url == Constants.HOME_PAGE_URL) "" else url
        }
    }

    fun updateTabProgress(tabId: String, progress: Int, isLoading: Boolean) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(progress = progress, isLoading = isLoading)
            else it
        }
    }

    fun updateTabNavigationState(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(canGoBack = canGoBack, canGoForward = canGoForward)
            else it
        }
    }

    fun updateTabTitle(tabId: String, title: String) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(title = title.ifBlank { it.url })
            else it
        }
    }

    fun updateTabFavicon(tabId: String, faviconUrl: String?) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(favicon = faviconUrl)
            else it
        }
    }

    fun toggleDesktopMode(tabId: String) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(isDesktopMode = !it.isDesktopMode)
            else it
        }
    }

    fun pinTab(tabId: String) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(isPinned = !it.isPinned)
            else it
        }
    }

    private fun updateTabLastAccessed(tabId: String) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(lastAccessed = System.currentTimeMillis())
            else it
        }
    }

    // History
    fun addHistory(url: String, title: String) {
        if (url == Constants.HOME_PAGE_URL || url.startsWith("about:") || url.isBlank()) return
        val currentTabIncognito = _tabs.value.find { it.id == _currentTabId.value }?.isIncognito ?: false
        if (currentTabIncognito) return

        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.historyDao().getHistoryByUrl(url)
            if (existing != null) {
                db.historyDao().incrementVisitCount(url, title, System.currentTimeMillis())
            } else {
                db.historyDao().insert(HistoryEntity(url = url, title = title))
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { db.historyDao().deleteById(id) }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) { db.historyDao().clearAll() }
    }

    fun deleteHistoryByUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) { db.historyDao().deleteByUrl(url) }
    }

    // Bookmarks
    fun addBookmark(url: String, title: String, folderId: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.bookmarkDao().getBookmarkByUrl(url)
            if (existing == null) {
                db.bookmarkDao().insert(BookmarkEntity(url = url, title = title, folderId = folderId))
            }
        }
    }

    fun updateBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch(Dispatchers.IO) { db.bookmarkDao().update(bookmark) }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { db.bookmarkDao().deleteById(id) }
    }

    fun addBookmarkFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.bookmarkDao().insertFolder(com.anek.browser.database.entity.BookmarkFolderEntity(name = name))
        }
    }

    fun deleteBookmarkFolder(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { db.bookmarkDao().deleteFolderById(id) }
    }

    fun isBookmarked(url: String): Flow<Boolean> {
        return bookmarks.map { list -> list.any { it.url == url } }
    }

    // Shortcuts
    fun addShortcut(url: String, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (shortcuts.value.size >= Constants.SHORTCUT_LIMIT) return@launch
            db.shortcutDao().insert(ShortcutEntity(url = url, title = title, position = shortcuts.value.size))
        }
    }

    fun deleteShortcut(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { db.shortcutDao().deleteById(id) }
    }

    fun updateShortcutPosition(shortcuts: List<ShortcutEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            shortcuts.forEachIndexed { index, shortcut ->
                db.shortcutDao().update(shortcut.copy(position = index))
            }
        }
    }

    // Downloads
    fun clearDownloads() {
        viewModelScope.launch(Dispatchers.IO) { db.downloadDao().clearAll() }
    }

    // Privacy
    fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().cacheDir.deleteRecursively()
            } catch (_: Exception) {}
        }
    }

    fun clearWebStorage() {
        WebStorage.getInstance().deleteAllData()
    }

    fun clearAllBrowsingData() {
        clearCookies()
        clearCache()
        clearWebStorage()
        clearAllHistory()
        viewModelScope.launch(Dispatchers.IO) {
            db.shortcutDao().clearAll()
        }
    }

    // Find in page
    fun setFindInPageActive(active: Boolean) {
        _isFindInPageActive.value = active
        if (!active) _findInPageQuery.value = ""
    }

    fun setFindInPageQuery(query: String) {
        _findInPageQuery.value = query
    }

    // --- Page load timing (performance overlay) ---------------------------

    fun onPageLoadStarted() {
        loadStartNanos = System.nanoTime()
        _lastLoadMillis.value = 0L
    }

    fun onPageLoadFinished() {
        if (loadStartNanos != 0L) {
            _lastLoadMillis.value = (System.nanoTime() - loadStartNanos) / 1_000_000L
            loadStartNanos = 0L
        }
    }

    // --- Developer options actions ----------------------------------------

    /** Grabs the live DOM (not the original HTML) for the current page. */
    fun capturePageSource() {
        val wv = activeWebView() ?: run { _pageSource.value = null; return }
        if (wv.url.isNullOrBlank() || wv.url!!.startsWith("about:")) {
            _pageSource.value = "<!-- internal page, no source available -->"
            return
        }
        wv.evaluateJavascript(
            "'<!DOCTYPE html>\\n' + document.documentElement.outerHTML"
        ) { result ->
            _pageSource.value = result
                ?.removeSurrounding("\"")
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                ?.replace("\\/", "/")
                ?.replace("\\t", "\t")
        }
    }

    fun clearPageSource() {
        _pageSource.value = null
    }

    fun hardReload() {
        activeWebView()?.let { wv ->
            wv.clearCache(false)
            wv.loadUrl(wv.url ?: return@let, mapOf("Cache-Control" to "no-cache"))
        }
    }

    fun runJavaScript(code: String) {
        if (code.isBlank()) return
        val wv = activeWebView() ?: run {
            ConsoleLog.add(com.anek.browser.web.ConsoleLevel.ERROR, "No active WebView")
            return
        }
        wv.evaluateJavascript(code) { result ->
            ConsoleLog.add(
                com.anek.browser.web.ConsoleLevel.TIP,
                "⇒ ${result ?: "null"}",
                "evaluateJavascript"
            )
        }
    }

    fun clearConsole() = ConsoleLog.clear()

    /**
     * Flips WebView remote debugging at runtime. This used to be hard-coded on
     * in Application.onCreate, which left every release build inspectable by
     * anyone with a USB cable.
     */
    fun applyRemoteDebugging(enabled: Boolean) {
        try {
            WebView.setWebContentsDebuggingEnabled(enabled)
        } catch (_: Throwable) { }
    }

    // --- User scripts ------------------------------------------------------

    fun saveUserScript(script: UserScriptEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (script.id == 0L) db.userScriptDao().insert(script)
            else db.userScriptDao().update(script)
        }
    }

    fun deleteUserScript(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { db.userScriptDao().deleteById(id) }
    }

    fun setUserScriptEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { db.userScriptDao().setEnabled(id, enabled) }
    }

    fun clearUserScripts() {
        viewModelScope.launch(Dispatchers.IO) { db.userScriptDao().clearAll() }
    }

    // --- Blocking ----------------------------------------------------------

    /** Parses the user blocklist once per settings change instead of per request. */
    val customBlockDomains: Set<String> = settings
        .map { AdBlocker.parseCustomList(it.customBlocklist) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun resetBlockedCount() = AdBlocker.resetCounter()

    // Settings delegates
    fun updateSearchEngine(engine: SearchEngine) {
        viewModelScope.launch { settingsRepo.updateSearchEngine(engine) }
    }

    fun updateCustomSearchUrl(url: String) {
        viewModelScope.launch { settingsRepo.updateCustomSearchUrl(url) }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch { settingsRepo.updateTheme(theme) }
    }

    fun updateJavaScriptEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateJavaScriptEnabled(enabled) }
    }

    fun updateBlockThirdPartyCookies(block: Boolean) {
        viewModelScope.launch { settingsRepo.updateBlockThirdPartyCookies(block) }
    }

    fun updateDoNotTrack(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateDoNotTrack(enabled) }
    }

    fun updateDesktopMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateDesktopMode(enabled) }
    }

    fun updateTextScaling(scaling: Int) {
        viewModelScope.launch { settingsRepo.updateTextScaling(scaling) }
    }

    fun updateShowShortcuts(show: Boolean) {
        viewModelScope.launch { settingsRepo.updateShowShortcuts(show) }
    }

    fun updateShowRecentSites(show: Boolean) {
        viewModelScope.launch { settingsRepo.updateShowRecentSites(show) }
    }

    fun updateSafeBrowsing(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateSafeBrowsing(enabled) }
    }

    fun updateHomepage(url: String) {
        viewModelScope.launch { settingsRepo.updateHomepage(url) }
    }

    fun updateToolbarPosition(position: String) {
        viewModelScope.launch { settingsRepo.updateToolbarPosition(position) }
    }

    fun updateSuggestionsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateSuggestionsEnabled(enabled) }
    }

    fun updateTabRestoreEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateTabRestoreEnabled(enabled) }
    }

    // Performance
    fun updateImagesEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateImagesEnabled(enabled) }
    }

    fun updateDataSaver(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateDataSaver(enabled) }
    }

    fun updatePrefetchEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updatePrefetchEnabled(enabled) }
    }

    // Blocking
    fun updateAdBlockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateAdBlockEnabled(enabled) }
    }

    fun updateTrackerBlockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateTrackerBlockEnabled(enabled) }
    }

    fun updateCustomBlocklist(list: String) {
        viewModelScope.launch { settingsRepo.updateCustomBlocklist(list) }
    }

    // Media
    fun updateFullscreenVideo(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateFullscreenVideo(enabled) }
    }

    fun updateKeepScreenOnVideo(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateKeepScreenOnVideo(enabled) }
    }

    fun updateBackgroundAudio(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateBackgroundAudio(enabled) }
    }

    // Appearance
    fun updateForceDarkWebContent(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateForceDarkWebContent(enabled) }
    }

    fun updateHideStatusBar(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateHideStatusBar(enabled) }
    }

    // Developer options
    fun updateDevToolsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateDevToolsEnabled(enabled) }
    }

    fun updateRemoteDebugging(enabled: Boolean) {
        applyRemoteDebugging(enabled)
        viewModelScope.launch { settingsRepo.updateRemoteDebugging(enabled) }
    }

    fun updateCaptureConsole(enabled: Boolean) {
        if (!enabled) ConsoleLog.clear()
        viewModelScope.launch { settingsRepo.updateCaptureConsole(enabled) }
    }

    fun updateShowPerfOverlay(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateShowPerfOverlay(enabled) }
    }

    // User scripts
    fun updateUserScriptsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateUserScriptsEnabled(enabled) }
    }

    fun updateCustomUserAgent(agent: String) {
        viewModelScope.launch { settingsRepo.updateCustomUserAgent(agent) }
    }

    fun updateStartupBehavior(behavior: String) {
        viewModelScope.launch { settingsRepo.updateStartupBehavior(behavior) }
    }

    fun resetSettings() {
        viewModelScope.launch { settingsRepo.clearAll() }
    }

    fun resolveInput(input: String): String {
        val s = settings.value
        return UrlUtils.resolveInputToUrl(input, s.searchEngine, s.customSearchUrl)
    }

    fun getCurrentTabUrl(): String {
        return currentTab.value?.url ?: Constants.HOME_PAGE_URL
    }
}

sealed class OmniboxSuggestion {
    abstract val title: String
    abstract val url: String
    data class History(override val title: String, override val url: String) : OmniboxSuggestion()
    data class Bookmark(override val title: String, override val url: String) : OmniboxSuggestion()
    data class Search(val query: String) : OmniboxSuggestion() {
        override val title: String = query
        override val url: String = query
    }
    data class Url(override val url: String) : OmniboxSuggestion() {
        override val title: String = url
    }
}

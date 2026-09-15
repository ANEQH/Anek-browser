package com.anek.browser.browser

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anek.browser.data.datastore.BrowserSettings
import com.anek.browser.data.datastore.SettingsRepository
import com.anek.browser.database.AppDatabase
import com.anek.browser.database.entity.BookmarkEntity
import com.anek.browser.database.entity.HistoryEntity
import com.anek.browser.database.entity.ShortcutEntity
import com.anek.browser.utils.Constants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    val shortcuts: StateFlow<List<ShortcutEntity>> = db.shortcutDao().getAllShortcuts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads = db.downloadDao().getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tab operations
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
        }
    }

    fun addPrivateTab() {
        addTab(isIncognito = true)
    }

    fun selectTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _currentTabId.value = tabId
            updateTabLastAccessed(tabId)
        }
    }

    fun closeTab(tabId: String) {
        val tabToClose = _tabs.value.find { it.id == tabId } ?: return
        _closedTabs.value = (listOf(ClosedTab(tabToClose)) + _closedTabs.value).take(10)

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
            _closedTabs.value = (toClose.map { ClosedTab(it) } + _closedTabs.value).take(10)
        }
        val remaining = if (includePrivate) emptyList() else _tabs.value.filter { it.isIncognito }
        _tabs.value = if (remaining.isEmpty()) listOf(Tab()) else remaining
        _currentTabId.value = _tabs.value.first().id
    }

    fun duplicateTab(tabId: String) {
        val original = _tabs.value.find { it.id == tabId } ?: return
        val duplicate = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            lastAccessed = System.currentTimeMillis()
        )
        _tabs.value = _tabs.value + duplicate
        _currentTabId.value = duplicate.id
    }

    fun restoreLastClosedTab() {
        val last = _closedTabs.value.firstOrNull() ?: return
        _tabs.value = _tabs.value + last.tab.copy(lastAccessed = System.currentTimeMillis())
        _currentTabId.value = last.tab.id
        _closedTabs.value = _closedTabs.value.drop(1)
    }

    fun updateTabUrl(tabId: String, url: String, title: String = url) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(url = url, title = title, lastAccessed = System.currentTimeMillis())
            else it
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

    fun toggleDesktopMode(tabId: String) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(isDesktopMode = !it.isDesktopMode)
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

        viewModelScope.launch {
            val existing = db.historyDao().getHistoryByUrl(url)
            if (existing != null) {
                db.historyDao().incrementVisitCount(url, title, System.currentTimeMillis())
            } else {
                db.historyDao().insert(HistoryEntity(url = url, title = title))
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { db.historyDao().deleteById(id) }
    }

    fun clearAllHistory() {
        viewModelScope.launch { db.historyDao().clearAll() }
    }

    fun searchHistory(query: String): Flow<List<HistoryEntity>> {
        return if (query.isBlank()) db.historyDao().getAllHistory()
        else db.historyDao().searchHistory(query)
    }

    // Bookmarks
    fun addBookmark(url: String, title: String, folderId: Long? = null) {
        viewModelScope.launch {
            val existing = db.bookmarkDao().getBookmarkByUrl(url)
            if (existing == null) {
                db.bookmarkDao().insert(BookmarkEntity(url = url, title = title, folderId = folderId))
            }
        }
    }

    fun updateBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch { db.bookmarkDao().update(bookmark) }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { db.bookmarkDao().deleteById(id) }
    }

    fun isBookmarked(url: String): Flow<Boolean> {
        return bookmarks.map { list -> list.any { it.url == url } }
    }

    // Shortcuts
    fun addShortcut(url: String, title: String) {
        viewModelScope.launch {
            if (shortcuts.value.size >= Constants.SHORTCUT_LIMIT) return@launch
            db.shortcutDao().insert(ShortcutEntity(url = url, title = title, position = shortcuts.value.size))
        }
    }

    fun deleteShortcut(id: Long) {
        viewModelScope.launch { db.shortcutDao().deleteById(id) }
    }

    fun updateShortcutPosition(shortcuts: List<ShortcutEntity>) {
        viewModelScope.launch {
            shortcuts.forEachIndexed { index, shortcut ->
                db.shortcutDao().update(shortcut.copy(position = index))
            }
        }
    }

    // Downloads handled via DownloadManager, but we track history
    fun clearDownloads() {
        viewModelScope.launch { db.downloadDao().clearAll() }
    }

    // Privacy
    fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    fun clearCache() {
        // WebView cache is cleared per WebView instance, but also clear app cache
        viewModelScope.launch {
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
        viewModelScope.launch {
            db.shortcutDao().clearAll()
            // Keep bookmarks unless user wants
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

    fun resetSettings() {
        viewModelScope.launch { settingsRepo.clearAll() }
    }

    fun resolveInput(input: String): String {
        val s = settings.value
        return com.anek.browser.browser.UrlUtils.resolveInputToUrl(input, s.searchEngine, s.customSearchUrl)
    }

    fun getCurrentTabUrl(): String {
        return currentTab.value?.url ?: Constants.HOME_PAGE_URL
    }
}

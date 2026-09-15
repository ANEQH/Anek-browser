package com.anek.browser.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.anek.browser.browser.PersistedTab
import com.anek.browser.browser.SearchEngine
import com.anek.browser.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

object SettingsKeys {
    val SEARCH_ENGINE = stringPreferencesKey("search_engine")
    val CUSTOM_SEARCH_URL = stringPreferencesKey("custom_search_url")
    val HOMEPAGE = stringPreferencesKey("homepage")
    val THEME = stringPreferencesKey("theme") // LIGHT, DARK, SYSTEM
    val TOOLBAR_POSITION = stringPreferencesKey("toolbar_position") // TOP, BOTTOM
    val SHOW_SHORTCUTS = booleanPreferencesKey("show_shortcuts")
    val SHOW_RECENT_SITES = booleanPreferencesKey("show_recent_sites")
    val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
    val BLOCK_THIRD_PARTY_COOKIES = booleanPreferencesKey("block_third_party_cookies")
    val DO_NOT_TRACK = booleanPreferencesKey("do_not_track")
    val DESKTOP_MODE = booleanPreferencesKey("desktop_mode")
    val TEXT_SCALING = intPreferencesKey("text_scaling") // percentage 50-200
    val SAFE_BROWSING = booleanPreferencesKey("safe_browsing")
    val INCOGNITO_DEFAULT = booleanPreferencesKey("incognito_default")
    val SHOW_WALLPAPER = booleanPreferencesKey("show_wallpaper")
    val WALLPAPER_TYPE = stringPreferencesKey("wallpaper_type")
    val STARTUP_BEHAVIOR = stringPreferencesKey("startup_behavior") // HOME, LAST_TAB, BLANK
    val DOWNLOAD_LOCATION = stringPreferencesKey("download_location")
    val AUTOFILL_ENABLED = booleanPreferencesKey("autofill_enabled")
    val ZOOM_CONTROLS = booleanPreferencesKey("zoom_controls")
    val CUSTOM_USER_AGENT = stringPreferencesKey("custom_user_agent")
    val TABS_JSON = stringPreferencesKey("tabs_json")
    val CURRENT_TAB_ID = stringPreferencesKey("current_tab_id")
    val SUGGESTIONS_ENABLED = booleanPreferencesKey("suggestions_enabled")
    val TAB_RESTORE_ENABLED = booleanPreferencesKey("tab_restore_enabled")
    val CLOSE_TABS_ON_EXIT = booleanPreferencesKey("close_tabs_on_exit")

    // --- Performance -------------------------------------------------------
    val IMAGES_ENABLED = booleanPreferencesKey("images_enabled")
    val DATA_SAVER = booleanPreferencesKey("data_saver")
    val PREFETCH_ENABLED = booleanPreferencesKey("prefetch_enabled")

    // --- Blocking ----------------------------------------------------------
    val AD_BLOCK_ENABLED = booleanPreferencesKey("ad_block_enabled")
    val TRACKER_BLOCK_ENABLED = booleanPreferencesKey("tracker_block_enabled")
    val CUSTOM_BLOCKLIST = stringPreferencesKey("custom_blocklist") // newline separated

    // --- Media -------------------------------------------------------------
    val FULLSCREEN_VIDEO = booleanPreferencesKey("fullscreen_video")
    val KEEP_SCREEN_ON_VIDEO = booleanPreferencesKey("keep_screen_on_video")
    val BACKGROUND_AUDIO = booleanPreferencesKey("background_audio")

    // --- Appearance --------------------------------------------------------
    val FORCE_DARK_WEB_CONTENT = booleanPreferencesKey("force_dark_web_content")
    val HIDE_STATUS_BAR = booleanPreferencesKey("hide_status_bar")

    // --- Developer options -------------------------------------------------
    val DEV_TOOLS_ENABLED = booleanPreferencesKey("dev_tools_enabled")
    val REMOTE_DEBUGGING = booleanPreferencesKey("remote_debugging")
    val CAPTURE_CONSOLE = booleanPreferencesKey("capture_console")
    val SHOW_PERF_OVERLAY = booleanPreferencesKey("show_perf_overlay")

    // --- User scripts ------------------------------------------------------
    val USERSCRIPTS_ENABLED = booleanPreferencesKey("userscripts_enabled")
}

data class BrowserSettings(
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val customSearchUrl: String = "",
    val homepage: String = Constants.DEFAULT_HOMEPAGE,
    val theme: String = "SYSTEM",
    val toolbarPosition: String = "BOTTOM",
    val showShortcuts: Boolean = true,
    val showRecentSites: Boolean = true,
    val javaScriptEnabled: Boolean = true,
    val blockThirdPartyCookies: Boolean = false,
    val doNotTrack: Boolean = false,
    val desktopMode: Boolean = false,
    val textScaling: Int = 100,
    val safeBrowsing: Boolean = true,
    val incognitoDefault: Boolean = false,
    val showWallpaper: Boolean = true,
    val wallpaperType: String = "DEFAULT",
    val startupBehavior: String = "RESTORE", // RESTORE, HOME, BLANK
    val downloadLocation: String = "",
    val autofillEnabled: Boolean = true,
    val zoomControls: Boolean = true,
    val customUserAgent: String = "",
    val suggestionsEnabled: Boolean = true,
    val tabRestoreEnabled: Boolean = true,
    val closeTabsOnExit: Boolean = false,

    // Performance
    val imagesEnabled: Boolean = true,
    val dataSaver: Boolean = false,
    val prefetchEnabled: Boolean = true,

    // Blocking
    val adBlockEnabled: Boolean = true,
    val trackerBlockEnabled: Boolean = true,
    val customBlocklist: String = "",

    // Media
    val fullscreenVideo: Boolean = true,
    val keepScreenOnVideo: Boolean = true,
    val backgroundAudio: Boolean = false,

    // Appearance
    val forceDarkWebContent: Boolean = false,
    val hideStatusBar: Boolean = false,

    // Developer options
    val devToolsEnabled: Boolean = false,
    val remoteDebugging: Boolean = false,
    val captureConsole: Boolean = false,
    val showPerfOverlay: Boolean = false,

    // User scripts
    val userscriptsEnabled: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val settingsFlow: Flow<BrowserSettings> = context.settingsDataStore.data.map { prefs ->
        BrowserSettings(
            searchEngine = SearchEngine.fromName(prefs[SettingsKeys.SEARCH_ENGINE] ?: SearchEngine.GOOGLE.name),
            customSearchUrl = prefs[SettingsKeys.CUSTOM_SEARCH_URL] ?: "",
            homepage = prefs[SettingsKeys.HOMEPAGE] ?: Constants.DEFAULT_HOMEPAGE,
            theme = prefs[SettingsKeys.THEME] ?: "SYSTEM",
            toolbarPosition = prefs[SettingsKeys.TOOLBAR_POSITION] ?: "BOTTOM",
            showShortcuts = prefs[SettingsKeys.SHOW_SHORTCUTS] ?: true,
            showRecentSites = prefs[SettingsKeys.SHOW_RECENT_SITES] ?: true,
            javaScriptEnabled = prefs[SettingsKeys.JAVASCRIPT_ENABLED] ?: true,
            blockThirdPartyCookies = prefs[SettingsKeys.BLOCK_THIRD_PARTY_COOKIES] ?: false,
            doNotTrack = prefs[SettingsKeys.DO_NOT_TRACK] ?: false,
            desktopMode = prefs[SettingsKeys.DESKTOP_MODE] ?: false,
            textScaling = prefs[SettingsKeys.TEXT_SCALING] ?: 100,
            safeBrowsing = prefs[SettingsKeys.SAFE_BROWSING] ?: true,
            incognitoDefault = prefs[SettingsKeys.INCOGNITO_DEFAULT] ?: false,
            showWallpaper = prefs[SettingsKeys.SHOW_WALLPAPER] ?: true,
            wallpaperType = prefs[SettingsKeys.WALLPAPER_TYPE] ?: "DEFAULT",
            startupBehavior = prefs[SettingsKeys.STARTUP_BEHAVIOR] ?: "RESTORE",
            downloadLocation = prefs[SettingsKeys.DOWNLOAD_LOCATION] ?: "",
            autofillEnabled = prefs[SettingsKeys.AUTOFILL_ENABLED] ?: true,
            zoomControls = prefs[SettingsKeys.ZOOM_CONTROLS] ?: true,
            customUserAgent = prefs[SettingsKeys.CUSTOM_USER_AGENT] ?: "",
            suggestionsEnabled = prefs[SettingsKeys.SUGGESTIONS_ENABLED] ?: true,
            tabRestoreEnabled = prefs[SettingsKeys.TAB_RESTORE_ENABLED] ?: true,
            closeTabsOnExit = prefs[SettingsKeys.CLOSE_TABS_ON_EXIT] ?: false,

            imagesEnabled = prefs[SettingsKeys.IMAGES_ENABLED] ?: true,
            dataSaver = prefs[SettingsKeys.DATA_SAVER] ?: false,
            prefetchEnabled = prefs[SettingsKeys.PREFETCH_ENABLED] ?: true,

            adBlockEnabled = prefs[SettingsKeys.AD_BLOCK_ENABLED] ?: true,
            trackerBlockEnabled = prefs[SettingsKeys.TRACKER_BLOCK_ENABLED] ?: true,
            customBlocklist = prefs[SettingsKeys.CUSTOM_BLOCKLIST] ?: "",

            fullscreenVideo = prefs[SettingsKeys.FULLSCREEN_VIDEO] ?: true,
            keepScreenOnVideo = prefs[SettingsKeys.KEEP_SCREEN_ON_VIDEO] ?: true,
            backgroundAudio = prefs[SettingsKeys.BACKGROUND_AUDIO] ?: false,

            forceDarkWebContent = prefs[SettingsKeys.FORCE_DARK_WEB_CONTENT] ?: false,
            hideStatusBar = prefs[SettingsKeys.HIDE_STATUS_BAR] ?: false,

            devToolsEnabled = prefs[SettingsKeys.DEV_TOOLS_ENABLED] ?: false,
            remoteDebugging = prefs[SettingsKeys.REMOTE_DEBUGGING] ?: false,
            captureConsole = prefs[SettingsKeys.CAPTURE_CONSOLE] ?: false,
            showPerfOverlay = prefs[SettingsKeys.SHOW_PERF_OVERLAY] ?: false,

            userscriptsEnabled = prefs[SettingsKeys.USERSCRIPTS_ENABLED] ?: true
        )
    }

    val tabsFlow: Flow<Pair<List<PersistedTab>, String?>> = context.settingsDataStore.data.map { prefs ->
        val tabsJson = prefs[SettingsKeys.TABS_JSON] ?: ""
        val currentId = prefs[SettingsKeys.CURRENT_TAB_ID]
        val tabs = try {
            if (tabsJson.isNotBlank()) json.decodeFromString<List<PersistedTab>>(tabsJson)
            else emptyList()
        } catch (_: Exception) { emptyList() }
        tabs to currentId
    }

    suspend fun saveTabs(tabs: List<PersistedTab>, currentTabId: String?) {
        context.settingsDataStore.edit { prefs ->
            try {
                prefs[SettingsKeys.TABS_JSON] = json.encodeToString(tabs)
            } catch (_: Exception) {}
            if (currentTabId != null) prefs[SettingsKeys.CURRENT_TAB_ID] = currentTabId
            else prefs.remove(SettingsKeys.CURRENT_TAB_ID)
        }
    }

    suspend fun updateSearchEngine(engine: SearchEngine) {
        context.settingsDataStore.edit { it[SettingsKeys.SEARCH_ENGINE] = engine.name }
    }

    suspend fun updateCustomSearchUrl(url: String) {
        context.settingsDataStore.edit { it[SettingsKeys.CUSTOM_SEARCH_URL] = url }
    }

    suspend fun updateHomepage(url: String) {
        context.settingsDataStore.edit { it[SettingsKeys.HOMEPAGE] = url }
    }

    suspend fun updateTheme(theme: String) {
        context.settingsDataStore.edit { it[SettingsKeys.THEME] = theme }
    }

    suspend fun updateToolbarPosition(position: String) {
        context.settingsDataStore.edit { it[SettingsKeys.TOOLBAR_POSITION] = position }
    }

    suspend fun updateShowShortcuts(show: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.SHOW_SHORTCUTS] = show }
    }

    suspend fun updateShowRecentSites(show: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.SHOW_RECENT_SITES] = show }
    }

    suspend fun updateJavaScriptEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.JAVASCRIPT_ENABLED] = enabled }
    }

    suspend fun updateBlockThirdPartyCookies(block: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.BLOCK_THIRD_PARTY_COOKIES] = block }
    }

    suspend fun updateDoNotTrack(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.DO_NOT_TRACK] = enabled }
    }

    suspend fun updateDesktopMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.DESKTOP_MODE] = enabled }
    }

    suspend fun updateTextScaling(scaling: Int) {
        context.settingsDataStore.edit { it[SettingsKeys.TEXT_SCALING] = scaling.coerceIn(50, 200) }
    }

    suspend fun updateSafeBrowsing(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.SAFE_BROWSING] = enabled }
    }

    suspend fun updateIncognitoDefault(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.INCOGNITO_DEFAULT] = enabled }
    }

    suspend fun updateShowWallpaper(show: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.SHOW_WALLPAPER] = show }
    }

    suspend fun updateWallpaperType(type: String) {
        context.settingsDataStore.edit { it[SettingsKeys.WALLPAPER_TYPE] = type }
    }

    suspend fun updateStartupBehavior(behavior: String) {
        context.settingsDataStore.edit { it[SettingsKeys.STARTUP_BEHAVIOR] = behavior }
    }

    suspend fun updateDownloadLocation(location: String) {
        context.settingsDataStore.edit { it[SettingsKeys.DOWNLOAD_LOCATION] = location }
    }

    suspend fun updateAutofillEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.AUTOFILL_ENABLED] = enabled }
    }

    suspend fun updateZoomControls(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.ZOOM_CONTROLS] = enabled }
    }

    suspend fun updateCustomUserAgent(agent: String) {
        context.settingsDataStore.edit { it[SettingsKeys.CUSTOM_USER_AGENT] = agent }
    }

    suspend fun updateSuggestionsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.SUGGESTIONS_ENABLED] = enabled }
    }

    suspend fun updateTabRestoreEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.TAB_RESTORE_ENABLED] = enabled }
    }

    // --- Performance -------------------------------------------------------

    suspend fun updateImagesEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.IMAGES_ENABLED] = enabled }
    }

    suspend fun updateDataSaver(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.DATA_SAVER] = enabled }
    }

    suspend fun updatePrefetchEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.PREFETCH_ENABLED] = enabled }
    }

    // --- Blocking ----------------------------------------------------------

    suspend fun updateAdBlockEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.AD_BLOCK_ENABLED] = enabled }
    }

    suspend fun updateTrackerBlockEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.TRACKER_BLOCK_ENABLED] = enabled }
    }

    suspend fun updateCustomBlocklist(list: String) {
        context.settingsDataStore.edit { it[SettingsKeys.CUSTOM_BLOCKLIST] = list }
    }

    // --- Media -------------------------------------------------------------

    suspend fun updateFullscreenVideo(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.FULLSCREEN_VIDEO] = enabled }
    }

    suspend fun updateKeepScreenOnVideo(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.KEEP_SCREEN_ON_VIDEO] = enabled }
    }

    suspend fun updateBackgroundAudio(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.BACKGROUND_AUDIO] = enabled }
    }

    // --- Appearance --------------------------------------------------------

    suspend fun updateForceDarkWebContent(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.FORCE_DARK_WEB_CONTENT] = enabled }
    }

    suspend fun updateHideStatusBar(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.HIDE_STATUS_BAR] = enabled }
    }

    // --- Developer options -------------------------------------------------

    suspend fun updateDevToolsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.DEV_TOOLS_ENABLED] = enabled }
    }

    suspend fun updateRemoteDebugging(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.REMOTE_DEBUGGING] = enabled }
    }

    suspend fun updateCaptureConsole(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.CAPTURE_CONSOLE] = enabled }
    }

    suspend fun updateShowPerfOverlay(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.SHOW_PERF_OVERLAY] = enabled }
    }

    // --- User scripts ------------------------------------------------------

    suspend fun updateUserScriptsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.USERSCRIPTS_ENABLED] = enabled }
    }

    suspend fun clearAll() {
        context.settingsDataStore.edit { it.clear() }
    }
}

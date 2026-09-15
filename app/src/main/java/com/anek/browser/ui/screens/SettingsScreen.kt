package com.anek.browser.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anek.browser.browser.SearchEngine
import com.anek.browser.data.datastore.BrowserSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: BrowserSettings,
    onUpdateSearchEngine: (SearchEngine) -> Unit,
    onUpdateTheme: (String) -> Unit,
    onUpdateJavaScript: (Boolean) -> Unit,
    onUpdateBlockThirdParty: (Boolean) -> Unit,
    onUpdateDoNotTrack: (Boolean) -> Unit,
    onUpdateDesktopMode: (Boolean) -> Unit,
    onUpdateTextScaling: (Int) -> Unit,
    onUpdateShowShortcuts: (Boolean) -> Unit,
    onUpdateShowRecent: (Boolean) -> Unit,
    onUpdateSafeBrowsing: (Boolean) -> Unit,
    onUpdateHomepage: (String) -> Unit,
    onUpdateToolbarPosition: (String) -> Unit,
    onUpdateSuggestions: (Boolean) -> Unit = {},
    onUpdateTabRestore: (Boolean) -> Unit = {},
    // --- v1.2.0 ---
    onUpdateImages: (Boolean) -> Unit = {},
    onUpdateDataSaver: (Boolean) -> Unit = {},
    onUpdateAdBlock: (Boolean) -> Unit = {},
    onUpdateTrackerBlock: (Boolean) -> Unit = {},
    onUpdateFullscreenVideo: (Boolean) -> Unit = {},
    onUpdateKeepScreenOn: (Boolean) -> Unit = {},
    onUpdateBackgroundAudio: (Boolean) -> Unit = {},
    onUpdateForceDarkWeb: (Boolean) -> Unit = {},
    onUpdateUserScripts: (Boolean) -> Unit = {},
    onUpdateStartup: (String) -> Unit = {},
    onUpdateHomepageSetting: (String) -> Unit = {},
    onUpdateUserAgent: (String) -> Unit = {},
    onUpdateDevTools: (Boolean) -> Unit = {},
    blockedCount: Int = 0,
    onOpenUserScripts: () -> Unit = {},
    onOpenDevTools: () -> Unit = {},
    onClearCookies: () -> Unit,
    onClearCache: () -> Unit,
    onClearWebStorage: () -> Unit,
    onClearAllData: () -> Unit,
    onResetSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showStartupDialog by remember { mutableStateOf(false) }
    var showHomepageDialog by remember { mutableStateOf(false) }
    var showUaDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSectionCard(title = "General", icon = Icons.Default.Settings) {
                    SettingsRowClickable(title = "Search engine", subtitle = settings.searchEngine.displayName, onClick = { showSearchEngineDialog = true })
                    SettingsRowClickable(title = "Theme", subtitle = settings.theme, onClick = { showThemeDialog = true })
                    SettingsRowClickable(
                        title = "Homepage",
                        subtitle = if (settings.homepage == com.anek.browser.utils.Constants.HOME_PAGE_URL)
                            "New tab page" else settings.homepage,
                        onClick = { showHomepageDialog = true }
                    )
                    SettingsRowClickable(
                        title = "On startup",
                        subtitle = when (settings.startupBehavior) {
                            "RESTORE" -> "Restore tabs"
                            "HOME" -> "Open homepage"
                            else -> "Blank page"
                        },
                        onClick = { showStartupDialog = true }
                    )
                    SettingsRowClickable(title = "Toolbar position", subtitle = settings.toolbarPosition, onClick = {
                        onUpdateToolbarPosition(if (settings.toolbarPosition == "BOTTOM") "TOP" else "BOTTOM")
                    })
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    SettingsRowSwitch(title = "Show shortcuts", subtitle = "Show quick access on home", checked = settings.showShortcuts, onCheckedChange = onUpdateShowShortcuts)
                    SettingsRowSwitch(title = "Show recent sites", subtitle = "Show recently visited", checked = settings.showRecentSites, onCheckedChange = onUpdateShowRecent)
                    SettingsRowSwitch(title = "Search suggestions", subtitle = "Show history & bookmarks in address bar", checked = settings.suggestionsEnabled, onCheckedChange = onUpdateSuggestions)
                    SettingsRowSwitch(title = "Restore tabs", subtitle = "Restore tabs after restart", checked = settings.tabRestoreEnabled, onCheckedChange = onUpdateTabRestore)
                }
            }

            item {
                SettingsSectionCard(title = "Privacy & Security", icon = Icons.Default.Security) {
                    SettingsRowSwitch(title = "JavaScript", subtitle = "Allow JavaScript execution", checked = settings.javaScriptEnabled, onCheckedChange = onUpdateJavaScript)
                    SettingsRowSwitch(title = "Block third-party cookies", subtitle = "Prevent cross-site tracking", checked = settings.blockThirdPartyCookies, onCheckedChange = onUpdateBlockThirdParty)
                    SettingsRowSwitch(title = "Do Not Track", subtitle = "Request sites not to track", checked = settings.doNotTrack, onCheckedChange = onUpdateDoNotTrack)
                    SettingsRowSwitch(title = "Safe Browsing", subtitle = "Warn about malicious sites", checked = settings.safeBrowsing, onCheckedChange = onUpdateSafeBrowsing)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    SettingsRowClickable(title = "Clear cookies", subtitle = "Delete all cookies", onClick = onClearCookies)
                    SettingsRowClickable(title = "Clear cache", subtitle = "Delete cached files", onClick = onClearCache)
                    SettingsRowClickable(title = "Clear website data", subtitle = "Delete local storage", onClick = onClearWebStorage)
                    SettingsRowClickable(title = "Clear all browsing data", subtitle = "Cookies, cache, history", onClick = { showClearDialog = true })
                }
            }

            item {
                SettingsSectionCard(title = "Browser", icon = Icons.Default.Language) {
                    SettingsRowSwitch(title = "Desktop mode", subtitle = "Always request desktop site", checked = settings.desktopMode, onCheckedChange = onUpdateDesktopMode)
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Text scaling: ${settings.textScaling}%", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = settings.textScaling.toFloat(),
                            onValueChange = { onUpdateTextScaling(it.toInt()) },
                            valueRange = 50f..200f,
                            steps = 5
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("A-", style = MaterialTheme.typography.labelSmall)
                            Text("A+", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Speed & data", icon = Icons.Default.Speed) {
                    SettingsRowSwitch(
                        title = "Load images",
                        subtitle = "Turn off for the fastest possible pages on slow networks",
                        checked = settings.imagesEnabled,
                        onCheckedChange = onUpdateImages
                    )
                    SettingsRowSwitch(
                        title = "Data saver",
                        subtitle = "Strip utm_*, fbclid, gclid and other tracking parameters from links",
                        checked = settings.dataSaver,
                        onCheckedChange = onUpdateDataSaver
                    )
                    SettingsRowSwitch(
                        title = "Force dark web content",
                        subtitle = "Ask WebView to darken sites with no dark mode of their own",
                        checked = settings.forceDarkWebContent,
                        onCheckedChange = onUpdateForceDarkWeb
                    )
                }
            }

            item {
                SettingsSectionCard(title = "Ad & tracker blocking", icon = Icons.Default.Shield) {
                    SettingsRowSwitch(
                        title = "Block ads",
                        subtitle = "Ad networks, pop-unders and ad frames",
                        checked = settings.adBlockEnabled,
                        onCheckedChange = onUpdateAdBlock
                    )
                    SettingsRowSwitch(
                        title = "Block trackers",
                        subtitle = "Analytics and cross-site tracking",
                        checked = settings.trackerBlockEnabled,
                        onCheckedChange = onUpdateTrackerBlock
                    )
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    SettingsRowClickable(
                        title = "Requests blocked",
                        subtitle = "$blockedCount since app start • custom rules in Developer options",
                        onClick = {}
                    )
                }
            }

            item {
                SettingsSectionCard(title = "Media", icon = Icons.Default.PlayCircle) {
                    SettingsRowSwitch(
                        title = "Fullscreen video",
                        subtitle = "Let videos take over the whole screen",
                        checked = settings.fullscreenVideo,
                        onCheckedChange = onUpdateFullscreenVideo
                    )
                    SettingsRowSwitch(
                        title = "Keep screen on while playing",
                        subtitle = "Prevent the display sleeping during fullscreen video",
                        checked = settings.keepScreenOnVideo,
                        onCheckedChange = onUpdateKeepScreenOn
                    )
                    SettingsRowSwitch(
                        title = "Background audio",
                        subtitle = "Try to keep audio playing when the app is backgrounded",
                        checked = settings.backgroundAudio,
                        onCheckedChange = onUpdateBackgroundAudio
                    )
                }
            }

            item {
                SettingsSectionCard(title = "Extensions & scripts", icon = Icons.Default.Extension) {
                    SettingsRowSwitch(
                        title = "User scripts",
                        subtitle = "Inject your own JavaScript into matching pages",
                        checked = settings.userscriptsEnabled,
                        onCheckedChange = onUpdateUserScripts
                    )
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    SettingsRowClickable(
                        title = "Manage user scripts",
                        subtitle = "Chrome extensions cannot run in Android WebView — user scripts are the supported equivalent",
                        onClick = onOpenUserScripts
                    )
                }
            }

            item {
                val webViewVersion = try {
                    WebView.getCurrentWebViewPackage()?.versionName ?: "Unknown"
                } catch (_: Exception) { "Unknown" }
                val userAgent = settings.customUserAgent.ifBlank {
                    try {
                        android.webkit.WebSettings.getDefaultUserAgent(context)
                    } catch (_: Exception) { "Unknown" }
                }

                SettingsSectionCard(title = "Advanced", icon = Icons.Default.Build) {
                    SettingsRowClickable(title = "WebView version", subtitle = webViewVersion, onClick = {})
                    SettingsRowClickable(
                        title = "User agent",
                        subtitle = userAgent.take(80) + "…",
                        onClick = { showUaDialog = true }
                    )
                    SettingsRowSwitch(
                        title = "Developer options",
                        subtitle = "Unlock remote debugging, live console, feature flags and test pages",
                        checked = settings.devToolsEnabled,
                        onCheckedChange = onUpdateDevTools
                    )
                    if (settings.devToolsEnabled) {
                        SettingsRowClickable(
                            title = "Open Developer options",
                            subtitle = "Diagnostics and flags",
                            onClick = onOpenDevTools
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    SettingsRowClickable(title = "Reset settings", subtitle = "Restore defaults", onClick = { showResetDialog = true })
                }
            }

            item {
                SettingsSectionCard(title = "About", icon = Icons.Default.Info) {
                    SettingsRowClickable(title = "Anek Browser", subtitle = "Version 1.1.0 (Chrome-like)", onClick = {})
                    SettingsRowClickable(title = "GitHub", subtitle = "github.com/ANEQH/Anek-browser", onClick = {})
                    SettingsRowClickable(title = "Privacy policy", subtitle = "View privacy policy", onClick = {})
                    SettingsRowClickable(title = "Open source licenses", subtitle = "Third-party licenses", onClick = {})
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Incognito Mode", style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Incognito does not provide complete anonymity. Your ISP, employer, or visited sites may still track you. It only prevents local history saving.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all browsing data?") },
            text = { Text("This will delete cookies, cache, history and site data. Bookmarks will be kept.") },
            confirmButton = {
                TextButton(onClick = { onClearAllData(); showClearDialog = false }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset settings?") },
            text = { Text("All settings will be restored to default. Tabs and bookmarks will be kept.") },
            confirmButton = {
                TextButton(onClick = { onResetSettings(); showResetDialog = false }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSearchEngineDialog) {
        AlertDialog(
            onDismissRequest = { showSearchEngineDialog = false },
            title = { Text("Search engine") },
            text = {
                // The previous version rendered this list twice: once as static
                // ListItems and again as clickable Surfaces, so every engine
                // appeared two times.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SearchEngine.entries.forEach { engine ->
                        Surface(
                            onClick = {
                                onUpdateSearchEngine(engine)
                                showSearchEngineDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (engine == settings.searchEngine)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ) {
                            ListItem(
                                headlineContent = { Text(engine.displayName) },
                                supportingContent = {
                                    Text(engine.homepageUrl, style = MaterialTheme.typography.bodySmall)
                                },
                                trailingContent = {
                                    if (engine == settings.searchEngine) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchEngineDialog = false }) { Text("Close") }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme") },
            text = {
                Column {
                    listOf("LIGHT" to "Light", "DARK" to "Dark", "SYSTEM" to "System default").forEach { (value, label) ->
                        Surface(
                            onClick = { onUpdateTheme(value); showThemeDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (value == settings.theme) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ) {
                            ListItem(
                                headlineContent = { Text(label) },
                                trailingContent = {
                                    if (value == settings.theme) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }

    if (showStartupDialog) {
        AlertDialog(
            onDismissRequest = { showStartupDialog = false },
            title = { Text("On startup") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    listOf(
                        "RESTORE" to "Restore previous tabs",
                        "HOME" to "Open homepage",
                        "BLANK" to "Open blank page"
                    ).forEach { (value, label) ->
                        Surface(
                            onClick = {
                                // This used to be a no-op placeholder, so the
                                // setting was displayed but never saved.
                                onUpdateStartup(value)
                                showStartupDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (value == settings.startupBehavior)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ) {
                            ListItem(
                                headlineContent = { Text(label) },
                                trailingContent = {
                                    if (value == settings.startupBehavior) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStartupDialog = false }) { Text("Close") }
            }
        )
    }

    if (showHomepageDialog) {
        var homepage by remember { mutableStateOf(settings.homepage) }
        AlertDialog(
            onDismissRequest = { showHomepageDialog = false },
            title = { Text("Homepage") },
            text = {
                Column {
                    OutlinedTextField(
                        value = homepage,
                        onValueChange = { homepage = it },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Quick picks",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { homepage = com.anek.browser.utils.Constants.HOME_PAGE_URL }, label = { Text("New tab page") })
                        AssistChip(onClick = { homepage = "https://www.google.com" }, label = { Text("Google") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { homepage = "https://duckduckgo.com" }, label = { Text("DuckDuckGo") })
                        AssistChip(onClick = { homepage = "https://www.wikipedia.org" }, label = { Text("Wikipedia") })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateHomepageSetting(homepage.trim().ifBlank { com.anek.browser.utils.Constants.HOME_PAGE_URL })
                    showHomepageDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showHomepageDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showUaDialog) {
        var ua by remember { mutableStateOf(settings.customUserAgent) }
        AlertDialog(
            onDismissRequest = { showUaDialog = false },
            title = { Text("User agent") },
            text = {
                Column {
                    OutlinedTextField(
                        value = ua,
                        onValueChange = { ua = it },
                        label = { Text("Leave blank for the default") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Current default: ${
                            try { android.webkit.WebSettings.getDefaultUserAgent(context) } catch (_: Exception) { "unknown" }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateUserAgent(ua.trim())
                    showUaDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showUaDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
        }
    }
}

@Composable
fun SettingsRowClickable(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = androidx.compose.ui.graphics.Color.Transparent) {
        ListItem(
            headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
            supportingContent = { if (subtitle != null) Text(subtitle, maxLines = 2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        )
    }
}

@Composable
fun SettingsRowSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

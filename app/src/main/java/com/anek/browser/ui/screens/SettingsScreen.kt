package com.anek.browser.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
                    SettingsRowClickable(title = "Homepage", subtitle = settings.homepage, onClick = {})
                    SettingsRowClickable(title = "Toolbar position", subtitle = settings.toolbarPosition, onClick = {
                        onUpdateToolbarPosition(if (settings.toolbarPosition == "BOTTOM") "TOP" else "BOTTOM")
                    })
                    SettingsRowSwitch(title = "Show shortcuts", checked = settings.showShortcuts, onCheckedChange = onUpdateShowShortcuts)
                    SettingsRowSwitch(title = "Show recent sites", checked = settings.showRecentSites, onCheckedChange = onUpdateShowRecent)
                }
            }

            item {
                SettingsSectionCard(title = "Privacy", icon = Icons.Default.Lock) {
                    SettingsRowSwitch(title = "JavaScript", subtitle = "Allow JavaScript execution", checked = settings.javaScriptEnabled, onCheckedChange = onUpdateJavaScript)
                    SettingsRowSwitch(title = "Block third-party cookies", checked = settings.blockThirdPartyCookies, onCheckedChange = onUpdateBlockThirdParty)
                    SettingsRowSwitch(title = "Do Not Track", subtitle = "Request sites not to track", checked = settings.doNotTrack, onCheckedChange = onUpdateDoNotTrack)
                    SettingsRowSwitch(title = "Safe Browsing", subtitle = "Warn about malicious sites", checked = settings.safeBrowsing, onCheckedChange = onUpdateSafeBrowsing)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    SettingsRowClickable(title = "Clear cookies", onClick = onClearCookies)
                    SettingsRowClickable(title = "Clear cache", onClick = onClearCache)
                    SettingsRowClickable(title = "Clear website data", onClick = onClearWebStorage)
                    SettingsRowClickable(title = "Clear all browsing data", onClick = { showClearDialog = true })
                }
            }

            item {
                SettingsSectionCard(title = "Browser", icon = Icons.Default.Search) {
                    SettingsRowSwitch(title = "Desktop mode", subtitle = "Always request desktop site", checked = settings.desktopMode, onCheckedChange = onUpdateDesktopMode)
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Text scaling: ${settings.textScaling}%", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = settings.textScaling.toFloat(),
                            onValueChange = { onUpdateTextScaling(it.toInt()) },
                            valueRange = 50f..200f,
                            steps = 5
                        )
                    }
                }
            }

            item {
                val webViewVersion = try {
                    WebView.getCurrentWebViewPackage()?.versionName ?: "Unknown"
                } catch (_: Exception) { "Unknown" }
                val userAgent = try {
                    android.webkit.WebSettings.getDefaultUserAgent(context)
                } catch (_: Exception) { "Unknown" }

                SettingsSectionCard(title = "Advanced", icon = Icons.Default.Build) {
                    SettingsRowClickable(title = "WebView version", subtitle = webViewVersion, onClick = {})
                    SettingsRowClickable(title = "User agent", subtitle = userAgent.take(80) + "...", onClick = {})
                    SettingsRowClickable(title = "Reset settings", onClick = { showResetDialog = true })
                }
            }

            item {
                SettingsSectionCard(title = "About", icon = Icons.Default.Info) {
                    SettingsRowClickable(title = "Anek Browser", subtitle = "Version 1.0.0", onClick = {})
                    SettingsRowClickable(title = "GitHub", subtitle = "github.com/anek-browser", onClick = {})
                    SettingsRowClickable(title = "Privacy policy", subtitle = "View privacy policy", onClick = {})
                    SettingsRowClickable(title = "Open source licenses", onClick = {})
                    Text(
                        "Incognito does not provide perfect anonymity. Your ISP, employer, or visited sites may still track you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
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
            text = { Text("All settings will be restored to default.") },
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
                Column {
                    SearchEngine.values().forEach { engine ->
                        TextButton(
                            onClick = { onUpdateSearchEngine(engine); showSearchEngineDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(engine.displayName)
                                if (engine == settings.searchEngine) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
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
                    listOf("LIGHT", "DARK", "SYSTEM").forEach { theme ->
                        TextButton(
                            onClick = { onUpdateTheme(theme); showThemeDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(theme)
                                if (theme == settings.theme) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
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
            headlineContent = { Text(title) },
            supportingContent = { if (subtitle != null) Text(subtitle, maxLines = 2, style = MaterialTheme.typography.bodySmall) }
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
        headlineContent = { Text(title) },
        supportingContent = { if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

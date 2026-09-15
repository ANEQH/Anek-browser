package com.anek.browser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anek.browser.data.datastore.BrowserSettings
import com.anek.browser.utils.DeviceInfo
import com.anek.browser.web.AdBlocker
import com.anek.browser.web.ConsoleEntry
import com.anek.browser.web.ConsoleLevel
import com.anek.browser.web.WebViewWarmup

/** Everything Developer options can trigger, grouped so wiring stays readable. */
data class DevToolsActions(
    val onDevToolsEnabled: (Boolean) -> Unit,
    val onRemoteDebugging: (Boolean) -> Unit,
    val onCaptureConsole: (Boolean) -> Unit,
    val onShowPerfOverlay: (Boolean) -> Unit,
    val onImagesEnabled: (Boolean) -> Unit,
    val onDataSaver: (Boolean) -> Unit,
    val onPrefetch: (Boolean) -> Unit,
    val onAdBlock: (Boolean) -> Unit,
    val onTrackerBlock: (Boolean) -> Unit,
    val onForceDarkWeb: (Boolean) -> Unit,
    val onCustomUserAgent: (String) -> Unit,
    val onCustomBlocklist: (String) -> Unit,
    val onClearCache: () -> Unit,
    val onClearConsole: () -> Unit,
    val onViewSource: () -> Unit,
    val onHardReload: () -> Unit,
    val onRunJs: (String) -> Unit,
    val onOpenDevToolsUrl: (String) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsScreen(
    settings: BrowserSettings,
    console: List<ConsoleEntry>,
    pageSource: String?,
    actions: DevToolsActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showConsole by remember { mutableStateOf(true) }
    var showUaEditor by remember { mutableStateOf(false) }
    var showBlocklistEditor by remember { mutableStateOf(false) }
    var showJsConsole by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pageSource) {
        if (!pageSource.isNullOrBlank()) showSourceDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer options") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showJsConsole = true }) {
                        Icon(Icons.Default.Terminal, contentDescription = "Run JavaScript")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ---------------- Runtime ----------------
            item {
                DevCard("Runtime", Icons.Default.Memory) {
                    DevRow("App version", "${DeviceInfo.appVersionName(context)} (${DeviceInfo.appVersionCode(context)})")
                    DevRow("Build type", if (DeviceInfo.isDebugBuild(context)) "debug (debuggable)" else "release")
                    DevRow("Android", DeviceInfo.androidVersion())
                    DevRow("Device", DeviceInfo.deviceModel())
                    DevRow("RAM", "${DeviceInfo.totalRamMb(context)} MB total")
                    DevRow("JVM heap", DeviceInfo.jvmHeapUsedMb())
                    DevRow("Native heap", DeviceInfo.nativeHeapMb())
                    DevRow("Free storage", "${DeviceInfo.freeStorageMb(context)} MB")
                }
            }

            // ---------------- WebView ----------------
            item {
                DevCard("WebView", Icons.Default.Public) {
                    DevRow("Provider", WebViewWarmup.providerVersion())
                    DevRow(
                        "Warm-up cost",
                        if (WebViewWarmup.warmupMillis >= 0) "${WebViewWarmup.warmupMillis} ms" else "not run"
                    )
                    DevRow("Pre-warmed", if (WebViewWarmup.ready) "yes" else "no")
                    DevRow("Force-dark supported", if (WebViewWarmup.supportsForceDark()) "yes" else "no")
                    DevRow("Built-in block rules", AdBlocker.builtinRuleCount.toString())
                    DevRow("Requests blocked", AdBlocker.blockedCount.get().toString())
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { actions.onHardReload() }) { Text("Hard reload") }
                        OutlinedButton(onClick = { actions.onViewSource() }) { Text("View source") }
                        OutlinedButton(onClick = { AdBlocker.resetCounter() }) { Text("Reset counter") }
                    }
                }
            }

            // ---------------- Storage ----------------
            item {
                val cacheMb = remember { DeviceInfo.dirSizeMb(context.cacheDir) }
                val filesMb = remember { DeviceInfo.dirSizeMb(context.filesDir) }
                DevCard("Storage", Icons.Default.Storage) {
                    DevRow("Cache dir", "$cacheMb MB")
                    DevRow("Files dir", "$filesMb MB")
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    OutlinedButton(onClick = actions.onClearCache) { Text("Clear cache") }
                }
            }

            // ---------------- Debugging ----------------
            item {
                DevCard("Debugging", Icons.Default.BugReport) {
                    DevSwitch(
                        "Remote WebView debugging",
                        "Inspect pages from a desktop browser",
                        settings.remoteDebugging,
                        actions.onRemoteDebugging
                    )
                    if (settings.remoteDebugging) {
                        DevNote(
                            "1. Enable USB debugging on this phone.\n" +
                                "2. Connect it to a computer with a USB cable.\n" +
                                "3. On the computer open Chrome and go to chrome://inspect\n" +
                                "4. The open Anek Browser tabs appear under \"Remote Target\" — click inspect."
                        )
                    }
                    DevSwitch(
                        "Capture console output",
                        "Records page console.* messages below",
                        settings.captureConsole,
                        actions.onCaptureConsole
                    )
                    DevSwitch(
                        "Performance overlay",
                        "Show load time and blocked-request count on the toolbar",
                        settings.showPerfOverlay,
                        actions.onShowPerfOverlay
                    )
                }
            }

            // ---------------- Flags ----------------
            item {
                DevCard("Feature flags", Icons.Default.Tune) {
                    DevSwitch("Load images", "Disable to save data and speed up pages", settings.imagesEnabled, actions.onImagesEnabled)
                    DevSwitch("Data saver", "Strip tracking query params from URLs", settings.dataSaver, actions.onDataSaver)
                    DevSwitch("Prefetch", "Allow the page to prefetch linked resources", settings.prefetchEnabled, actions.onPrefetch)
                    DevSwitch("Block ads", "Built-in ad network list", settings.adBlockEnabled, actions.onAdBlock)
                    DevSwitch("Block trackers", "Analytics and cross-site tracking", settings.trackerBlockEnabled, actions.onTrackerBlock)
                    DevSwitch(
                        "Force dark web content",
                        "Ask WebView to darken pages that have no dark mode",
                        settings.forceDarkWebContent,
                        actions.onForceDarkWeb
                    )
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showUaEditor = true }) { Text("User agent") }
                        OutlinedButton(onClick = { showBlocklistEditor = true }) { Text("Blocklist") }
                    }
                }
            }

            // ---------------- Quick URLs ----------------
            item {
                DevCard("Test pages", Icons.Default.Science) {
                    val tests = listOf(
                        "HTML5 video" to "https://www.w3schools.com/html/html5_video.asp",
                        "WebGL" to "https://get.webgl.org/",
                        "Canvas perf" to "https://threejs.org/examples/webgl_animation_keyframes.html",
                        "Speed test" to "https://fast.com/",
                        "WebRTC" to "https://webrtc.github.io/samples/src/content/peerconnection/pc1/",
                        "Viewport fit" to "https://whatismyviewport.com/",
                        "What's my UA" to "https://www.whatismybrowser.com/detect/what-is-my-user-agent/"
                    )
                    tests.forEach { (label, url) ->
                        DevRowClickable(label, url) { actions.onOpenDevToolsUrl(url) }
                    }
                }
            }

            // ---------------- Console ----------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Console (${console.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showConsole = !showConsole }) {
                        Text(if (showConsole) "Hide" else "Show")
                    }
                    TextButton(onClick = actions.onClearConsole) { Text("Clear") }
                }
            }

            if (showConsole) {
                if (console.isEmpty()) {
                    item {
                        DevNote(
                            if (settings.captureConsole)
                                "No console output yet. Reload a page to capture messages."
                            else
                                "Console capture is off. Enable it above."
                        )
                    }
                } else {
                    items(console.takeLast(200).reversed()) { entry ->
                        ConsoleRow(entry)
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // ---------------- Dialogs ----------------

    if (showUaEditor) {
        var ua by remember { mutableStateOf(settings.customUserAgent) }
        AlertDialog(
            onDismissRequest = { showUaEditor = false },
            title = { Text("Custom user agent") },
            text = {
                Column {
                    OutlinedTextField(
                        value = ua,
                        onValueChange = { ua = it },
                        label = { Text("Leave blank to use the default") },
                        minLines = 3
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Overrides the user agent for every page. Reload after changing.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    actions.onCustomUserAgent(ua.trim())
                    showUaEditor = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    actions.onCustomUserAgent("")
                    showUaEditor = false
                }) { Text("Reset") }
            }
        )
    }

    if (showBlocklistEditor) {
        var list by remember { mutableStateOf(settings.customBlocklist) }
        AlertDialog(
            onDismissRequest = { showBlocklistEditor = false },
            title = { Text("Custom blocklist") },
            text = {
                Column {
                    OutlinedTextField(
                        value = list,
                        onValueChange = { list = it },
                        label = { Text("One domain per line") },
                        minLines = 6,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Prefix a line with @ to always allow that domain even if a built-in rule matches it. " +
                            "Lines starting with # are comments.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    actions.onCustomBlocklist(list)
                    showBlocklistEditor = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showBlocklistEditor = false }) { Text("Cancel") } }
        )
    }

    if (showJsConsole) {
        var js by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJsConsole = false },
            title = { Text("Run JavaScript") },
            text = {
                OutlinedTextField(
                    value = js,
                    onValueChange = { js = it },
                    label = { Text("document.title") },
                    minLines = 4,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    actions.onRunJs(js)
                    showJsConsole = false
                }) { Text("Run") }
            },
            dismissButton = { TextButton(onClick = { showJsConsole = false }) { Text("Cancel") } }
        )
    }

    if (showSourceDialog && pageSource != null) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Page source") },
            text = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        pageSource.take(20_000),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showSourceDialog = false }) { Text("Close") } }
        )
    }
}

// ---------------------------------------------------------------------------
// Small building blocks
// ---------------------------------------------------------------------------

@Composable
private fun DevCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DevRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
private fun DevRowClickable(label: String, subtitle: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DevSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun DevNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    )
}

@Composable
private fun ConsoleRow(entry: ConsoleEntry) {
    val color = when (entry.level) {
        ConsoleLevel.ERROR -> MaterialTheme.colorScheme.error
        ConsoleLevel.WARN -> MaterialTheme.colorScheme.tertiary
        ConsoleLevel.TIP -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            entry.level.name.take(1),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        Spacer(Modifier.width(6.dp))
        Text(
            entry.message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            color = color
        )
    }
}

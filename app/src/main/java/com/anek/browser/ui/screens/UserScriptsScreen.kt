package com.anek.browser.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anek.browser.database.entity.UserScriptEntity
import com.anek.browser.utils.toDateTimeString

private val SAMPLE_SCRIPT = """
// Remove cookie banners and newsletter pop-ups.
(function () {
  var selectors = [
    '[class*="cookie-banner"]', '[id*="cookie-banner"]',
    '[class*="consent"]', '[id*="gdpr"]',
    '[class*="newsletter-popup"]', '[class*="paywall"]'
  ];
  selectors.forEach(function (s) {
    document.querySelectorAll(s).forEach(function (el) { el.remove(); });
  });
  document.documentElement.style.overflow = 'auto';
  document.body.style.overflow = 'auto';
})();
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScriptsScreen(
    scripts: List<UserScriptEntity>,
    globalEnabled: Boolean,
    onGlobalEnabledChange: (Boolean) -> Unit,
    onSave: (UserScriptEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf<UserScriptEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<UserScriptEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User scripts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editing = null
                        showEditor = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New script")
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("User scripts enabled", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Inject JavaScript into matching pages",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = globalEnabled, onCheckedChange = onGlobalEnabledChange)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Android's WebView cannot run Chrome/Edge extensions — there is no extension " +
                                "runtime on the platform. User scripts cover most of what people use " +
                                "extensions for: removing banners, restyling pages, adding buttons, " +
                                "auto-fill helpers and download grabbers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (scripts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("No scripts yet", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tap + to write one, or start from the banner-remover sample.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = {
                                editing = UserScriptEntity(
                                    name = "Remove cookie banners",
                                    description = "Deletes common consent and newsletter overlays",
                                    matchPattern = "*",
                                    code = SAMPLE_SCRIPT
                                )
                                showEditor = true
                            }) { Text("Add sample script") }
                        }
                    }
                }
            } else {
                items(scripts, key = { it.id }) { script ->
                    ScriptCard(
                        script = script,
                        onToggle = { onToggle(script.id, it) },
                        onEdit = {
                            editing = script
                            showEditor = true
                        },
                        onDelete = { confirmDelete = script }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showEditor) {
        ScriptEditorDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = {
                onSave(it)
                showEditor = false
            }
        )
    }

    confirmDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete script?") },
            text = { Text("\"${target.name}\" will be removed permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    confirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ScriptCard(
    script: UserScriptEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (script.enabled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (script.enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(script.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    Text(
                        script.matchPattern.ifBlank { "*" },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Switch(checked = script.enabled, onCheckedChange = onToggle)
            }
            if (script.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    script.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${script.code.lines().size} lines" +
                    if (script.lastRunAt > 0) "  •  last run ${script.lastRunAt.toDateTimeString()}"
                    else "  •  never run",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun ScriptEditorDialog(
    initial: UserScriptEntity?,
    onDismiss: () -> Unit,
    onSave: (UserScriptEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var match by remember { mutableStateOf(initial?.matchPattern ?: "*") }
    var code by remember { mutableStateOf(initial?.code ?: "") }
    var runAtEnd by remember { mutableStateOf(initial?.runAtEnd ?: true) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New script" else "Edit script") },
        text = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = match,
                        onValueChange = { match = it },
                        label = { Text("Match") },
                        supportingText = {
                            Text("* = all sites • example.com • https://site.com/path • comma separated")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("JavaScript") },
                        minLines = if (expanded) 12 else 6,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    )
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Shrink editor" else "Expand editor")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Run at document end", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Off = inject as early as possible",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = runAtEnd, onCheckedChange = { runAtEnd = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank() || code.isBlank()) return@TextButton
                    onSave(
                        (initial ?: UserScriptEntity(name = "", code = "")).copy(
                            name = name.trim(),
                            description = description.trim(),
                            matchPattern = match.trim().ifBlank { "*" },
                            code = code,
                            runAtEnd = runAtEnd,
                            enabled = initial?.enabled ?: true
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

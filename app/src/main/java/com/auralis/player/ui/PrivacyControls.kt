package com.auralis.player.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PrivacyControls(vm: LibraryViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    var clear by remember { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let { vm.exportLocal(it, false) } }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { importUri = it }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Private by design", style = MaterialTheme.typography.headlineSmall)
        Text("Your music and listening history stay on this device. Auralis has no Internet permission, accounts, advertising, analytics, or cloud backup. Folder access is read-only. Android shows a foreground media notification during playback.")
        Text("History stores play duration, completion, skips and listening time for explainable recommendations. Mood tags describe music, never your health or emotional state. Local exports contain personal listening data; choose where to keep them.")
        SettingSwitch("Pause listening history", settings["historyPaused"] == "true") { vm.setting("historyPaused", it.toString()) }
        SettingSwitch("Enable recommendations", settings["recommendations"] != "false") { vm.setting("recommendations", it.toString()) }
        SettingSwitch("Reduce repetition", settings["reduceRepetition"] != "false") { vm.setting("reduceRepetition", it.toString()) }
        SettingSwitch("Restore queue, paused", settings["restoreQueue"] != "false") { vm.setting("restoreQueue", it.toString()) }
        SettingSwitch("Pause when headphones or USB disconnect", settings["pauseOnDisconnect"] != "false") { vm.setting("pauseOnDisconnect", it.toString()) }
        Text("Turning disconnect pause off can allow music to continue through the speaker.", style = MaterialTheme.typography.bodySmall)
        Text("Motion follows Android's system animation settings. No looping visualizers or custom page transitions are used.", style = MaterialTheme.typography.bodySmall)
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("System", "Light", "Dark", "AMOLED").forEach { theme -> FilterChip((settings["theme"] ?: "System") == theme, { vm.setting("theme", theme) }, label = { Text(theme) }) }
        }
        OutlinedButton(onClick = { clear = true }) { Text("Clear listening history") }
        OutlinedButton(onClick = { export.launch("auralis-data.json") }) { Text("Export app data locally") }
        OutlinedButton(onClick = { import.launch(arrayOf("application/json", "text/plain")) }) { Text("Import app data") }
        Text("Selected folders", style = MaterialTheme.typography.titleMedium)
        folders.forEach { folder ->
            Row(Modifier.fillMaxWidth()) { Text(folder.name, Modifier.weight(1f)); TextButton(onClick = { vm.removeFolder(folder) }) { Text("Forget folder") } }
        }
        Text("Forgetting a folder removes its library records and permission; your music files remain untouched. Use a song's menu to forget or exclude that song or artist.", style = MaterialTheme.typography.bodySmall)
    }
    if (clear) AlertDialog(onDismissRequest = { clear = false }, title = { Text("Clear all listening history?") }, text = { Text("Completed plays, skips and time patterns will be erased. Favourites, playlists and exclusion choices are kept.") }, confirmButton = { TextButton(onClick = { vm.clearHistory(); clear = false }) { Text("Clear history") } }, dismissButton = { TextButton(onClick = { clear = false }) { Text("Cancel") } })
    if (importUri != null) AlertDialog(onDismissRequest = { importUri = null }, title = { Text("Import local app data?") }, text = { Text("This merges history and playlists and replaces matching preferences and song annotations. Only songs already in your authorized library can be matched. Export a backup first if you want to undo changes.") }, confirmButton = { TextButton(onClick = { vm.importLocal(importUri!!, false); importUri = null }) { Text("Import") } }, dismissButton = { TextButton(onClick = { importUri = null }) { Text("Cancel") } })
}

@Composable
fun SettingSwitch(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, change, modifier = Modifier.semanticsLabel(label))
    }
}

private fun Modifier.semanticsLabel(label: String) = this.then(Modifier.semantics { contentDescription = label })

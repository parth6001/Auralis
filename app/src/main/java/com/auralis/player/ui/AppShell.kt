package com.auralis.player.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.player.data.Song

private val pages = listOf("Home", "Library", "Mixes", "EQ", "Search")

@Composable
fun AuralisScreen(vm: LibraryViewModel) {
    val songs by vm.songs.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scan by vm.scan.collectAsStateWithLifecycle()
    val player by vm.playback.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val audio by vm.audio.collectAsStateWithLifecycle()
    val recommendations by vm.recommendations.collectAsStateWithLifecycle()
    val mixes by vm.mixes.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val theme = settings["theme"] ?: "System"
    val dark = theme == "Dark" || theme == "AMOLED" || (theme == "System" && isSystemInDarkTheme())
    val view = LocalView.current
    SideEffect { (view.context as? Activity)?.window?.let { window -> WindowCompat.getInsetsController(window, view).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark } } }
    val colors = if (dark) darkColorScheme(primary = Color(0xFF91D5B6), background = if (theme == "AMOLED") Color.Black else Color(0xFF101714), surface = if (theme == "AMOLED") Color.Black else Color(0xFF17211D))
        else lightColorScheme(primary = Color(0xFF276A50), background = Color(0xFFF7FAF5), surface = Color(0xFFF0F5EE))
    MaterialTheme(colorScheme = colors) {
        var page by rememberSaveable { mutableStateOf("Home") }
        var query by rememberSaveable { mutableStateOf("") }
        var expanded by rememberSaveable { mutableStateOf(false) }
        var grouping by rememberSaveable { mutableStateOf("Songs") }
        var group by rememberSaveable { mutableStateOf<String?>(null) }
        var selectedMix by rememberSaveable { mutableStateOf<String?>(null) }
        var showHidden by rememberSaveable { mutableStateOf(false) }
        var draft by remember { mutableStateOf<PlaylistDraft?>(null) }
        var moodSong by remember { mutableStateOf<Song?>(null) }
        val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { it?.let(vm::addFolder) }
        val presetExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let { vm.exportLocal(it, true) } }
        val presetImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { vm.importLocal(it, true) } }
        val current = songs.find { it.id == player.id }
        BoxWithConstraints {
            val wide = maxWidth >= 840.dp
            Scaffold(topBar = {
                Surface {
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AURALIS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { page = if (page == "Privacy") "Home" else "Privacy" }) { Text(if (page == "Privacy") "Back home" else "Privacy & settings") }
                    }
                }
            }, bottomBar = {
                Column {
                    if (current != null) Surface(tonalElevation = 4.dp, modifier = Modifier.clickable(onClickLabel = "Open Now Playing") { expanded = true }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(current.title, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${current.artist} · ${audio.path}", style = MaterialTheme.typography.labelMedium) }
                            TextButton(onClick = vm.playback::toggle) { Text(if (player.playing) "Pause" else "Play") }
                            TextButton(onClick = vm.playback::next) { Text("Next") }
                        }
                    }
                    if (!wide) NavigationBar { pages.forEachIndexed { i, title -> NavigationBarItem(page == title, { page = title; selectedMix = null }, icon = { NavSymbol(i) }, label = { Text(title) }) } }
                }
            }) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    if (wide) NavigationRail { pages.forEachIndexed { i, title -> NavigationRailItem(page == title, { page = title; selectedMix = null }, icon = { NavSymbol(i) }, label = { Text(title) }) } }
                    LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(if (wide) 32.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        item {
                            Text(when (page) { "Home" -> "Your music.\nYour space."; "EQ" -> "Equalizer"; "Mixes" -> "Smart Mixes"; else -> page }, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Text("Local listening, beautifully private", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (message != null || player.error != null) item { Card { Column(Modifier.padding(16.dp)) { Text(message ?: player.error.orEmpty()); TextButton(onClick = { vm.message.value = null }) { Text("Dismiss") } } } }
                        if (page == "Home" || page == "Library") item {
                            Card { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${songs.size} songs · on your devices", style = MaterialTheme.typography.titleMedium)
                                Text(scan.message)
                                if (scan.running) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("${scan.found} files · ${scan.skipped} metadata warnings") }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { folderPicker.launch(null) }, enabled = !scan.running) { Text("Add folder") }
                                    OutlinedButton(onClick = { if (scan.running) vm.cancelScan() else vm.rescan() }) { Text(if (scan.running) "Cancel scan" else "Rescan") }
                                    if (songs.isNotEmpty()) OutlinedButton(onClick = { vm.playback.play(songs) }, enabled = player.connected) { Text("Play library") }
                                }
                            } }
                        }
                        if (page == "Home" && recommendations.isNotEmpty()) {
                            item { Text("Made from your listening", style = MaterialTheme.typography.titleLarge) }
                            items(recommendations.take(6), key = { "rec:${it.song.id}" }) { rec -> Card { Column(Modifier.padding(16.dp)) {
                                SongRow(vm, rec.song, { vm.playback.play(recommendations.map { it.song }, recommendations.indexOf(rec)) }, { moodSong = rec.song })
                                Text(rec.explanation, style = MaterialTheme.typography.bodySmall)
                            } } }
                        }
                        if (page == "Home" && songs.isEmpty()) item { Text("Choose a folder containing music you own. Auralis only reads files you select.") }
                        if (page == "Library") item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Songs", "Albums", "Artists", "Genres", "Folders", "Favourites", "Duplicates").forEach { mode -> FilterChip(grouping == mode, { grouping = mode; group = null }, label = { Text(mode) }) } }
                            if (group != null) TextButton(onClick = { group = null }) { Text("Back to $grouping") }
                        }
                        if (page == "Search") item { OutlinedTextField(query, { query = it }, label = { Text("Search songs, albums, artists") }, modifier = Modifier.fillMaxWidth()) }
                        if (page in listOf("Library", "Search")) {
                            val groups = when (grouping) { "Albums" -> songs.groupBy { "${it.album} · ${it.albumArtist.ifBlank { it.artist }}" }; "Artists" -> songs.groupBy { it.artist }; "Genres" -> songs.groupBy { it.genre.ifBlank { "Unknown genre" } }; "Folders" -> songs.groupBy { it.folder }; else -> emptyMap() }
                            if (page == "Library" && groups.isNotEmpty() && group == null) items(groups.keys.sorted()) { key -> ListItem(headlineContent = { Text(key) }, supportingContent = { Text("${groups[key]?.size} songs") }, modifier = Modifier.clickable { group = key }) }
                            else {
                                val duplicateIds = if (grouping == "Duplicates") songs.filter { it.fingerprint.isNotBlank() }.groupBy { it.fingerprint }.values.filter { it.size > 1 }.flatten().map { it.id }.toSet() else emptySet()
                                val visible = if (page == "Search") songs.filter { "${it.title} ${it.artist} ${it.album}".contains(query, true) } else when (grouping) { "Favourites" -> songs.filter { it.favourite }; "Duplicates" -> songs.filter { it.id in duplicateIds }; else -> group?.let { groups[it] } ?: songs }
                                if (visible.isEmpty()) item { Text("No matching songs. Add a folder or change this view.") }
                                if (grouping == "Duplicates" && page == "Library") item { Text("Probable copies share size and a partial fingerprint. Source files are never merged or deleted.") }
                                items(visible, key = { it.id }) { song -> SongRow(vm, song, { vm.playback.play(visible, visible.indexOf(song)) }, { moodSong = song }) }
                            }
                        }
                        if (page == "Mixes") {
                            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = vm::refreshMixes) { Text("Refresh mixes") }; OutlinedButton(onClick = { draft = PlaylistDraft() }) { Text("New playlist") }
                                FilterChip(showHidden, { showHidden = !showHidden }, label = { Text("Manage hidden mixes") })
                            } }
                            if (selectedMix != null) {
                                item { TextButton(onClick = { selectedMix = null }) { Text("Back to mixes") }; Text(selectedMix!!, style = MaterialTheme.typography.titleLarge) }
                                val tracks = mixes[selectedMix].orEmpty()
                                if (tracks.isEmpty()) item { Text("No songs match this mix yet. Listen more or add manual music mood tags.") }
                                items(tracks, key = { it.id }) { song -> SongRow(vm, song, { vm.playback.play(tracks, tracks.indexOf(song)) }, { moodSong = song }) }
                            } else items(mixes.keys.filter { showHidden || settings["mix:$it"].let { s -> s == null || s == "show" } }.sortedWith(compareByDescending<String> { settings["pin:$it"] == "true" }.thenBy { it })) { name ->
                                Card { Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text(name, style = MaterialTheme.typography.titleMedium); Text("${mixes[name]?.size ?: 0} songs · ${settings["mix:$name"] ?: "show"}")
                                    FlowRow {
                                        TextButton(onClick = { selectedMix = name }, enabled = settings["mix:$name"] != "disabled") { Text("Open") }
                                        TextButton(onClick = { vm.setting("pin:$name", (settings["pin:$name"] != "true").toString()) }) { Text(if (settings["pin:$name"] == "true") "Unpin" else "Pin") }
                                        TextButton(onClick = { vm.setting("mix:$name", if (settings["mix:$name"] == "hidden") "show" else "hidden") }) { Text(if (settings["mix:$name"] == "hidden") "Show" else "Hide") }
                                        TextButton(onClick = { vm.setting("mix:$name", if (settings["mix:$name"] == "disabled") "show" else "disabled") }) { Text(if (settings["mix:$name"] == "disabled") "Enable" else "Disable") }
                                    }
                                } }
                            }
                            item { Text("Your playlists", style = MaterialTheme.typography.titleLarge) }
                            items(playlists, key = { it.id }) { list -> Card { Column(Modifier.padding(16.dp)) {
                                Text(list.name, style = MaterialTheme.typography.titleMedium)
                                FlowRow {
                                    TextButton(onClick = { val map = songs.associateBy { it.id }; vm.playback.play(list.songIds.split('\n').mapNotNull { map[it] }) }) { Text("Play playlist") }
                                    TextButton(onClick = { draft = PlaylistDraft(list.id, list.name, list.songIds.split('\n').filter { it.isNotBlank() }) }) { Text("Edit") }
                                    TextButton(onClick = { vm.deletePlaylist(list.id) }) { Text("Delete") }
                                }
                            } } }
                        }
                        if (page == "EQ") item { AudioControls(audio, vm::profile); FlowRow { TextButton(onClick = { presetExport.launch("auralis-preset.json") }) { Text("Export preset") }; TextButton(onClick = { presetImport.launch(arrayOf("application/json", "text/plain")) }) { Text("Import preset") } } }
                        if (page == "Privacy") item { PrivacyControls(vm) }
                    }
                }
            }
        }
        if (expanded && current != null) NowPlaying(vm, current, { expanded = false }, { draft = PlaylistDraft(name = "Saved queue", ids = player.queue) })
        draft?.let { d -> PlaylistEditor(d, songs, { name, ids -> if (d.id == null) vm.savePlaylist(name, ids) else vm.savePlaylist(name, ids, d.id); draft = null }, { draft = null }) }
        moodSong?.let { song ->
            var mood by remember(song.id) { mutableStateOf(song.mood) }
            AlertDialog(onDismissRequest = { moodSong = null }, title = { Text("Music mood tag") }, text = { Column { Text("Describe the music, such as Acoustic or Energetic."); OutlinedTextField(mood, { mood = it.take(40) }, label = { Text("Mood tag") }) } }, confirmButton = { TextButton(onClick = { vm.mood(song.id, mood); moodSong = null }) { Text("Save tag") } }, dismissButton = { TextButton(onClick = { moodSong = null }) { Text("Cancel") } })
        }
    }
}

@Composable
private fun NavSymbol(index: Int) { Text(listOf("◈", "♫", "≋", "☷", "⌕")[index], modifier = Modifier.semantics { contentDescription = listOf("Home", "Library", "Smart Mixes", "Equalizer", "Search")[index] }) }

@Composable
fun SongRow(vm: LibraryViewModel, song: Song, play: () -> Unit, mood: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClickLabel = "Play ${song.title}", onClick = play), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) { Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("${song.artist} · ${song.album}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        TextButton(onClick = { vm.favourite(song.id) }, modifier = Modifier.semantics { contentDescription = if (song.favourite) "Remove favourite ${song.title}" else "Favourite ${song.title}" }) { Text(if (song.favourite) "♥" else "♡") }
        Box {
            TextButton(onClick = { menu = true }, modifier = Modifier.semantics { contentDescription = "More actions for ${song.title}" }) { Text("More") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Play next") }, onClick = { vm.playback.playNext(song); menu = false })
                DropdownMenuItem(text = { Text(if (song.excluded) "Allow recommendations" else "Never recommend song") }, onClick = { vm.exclude(song.id, !song.excluded); menu = false })
                DropdownMenuItem(text = { Text("Forget song history") }, onClick = { vm.forget(listOf(song.id)); menu = false })
                DropdownMenuItem(text = { Text("Music mood tag") }, onClick = { mood(); menu = false })
                DropdownMenuItem(text = { Text(if (settings["excludeArtist:${song.artist}"] == "true") "Allow artist recommendations" else "Never recommend artist") }, onClick = { vm.setting("excludeArtist:${song.artist}", (settings["excludeArtist:${song.artist}"] != "true").toString()); menu = false })
                DropdownMenuItem(text = { Text("Forget artist history") }, onClick = { vm.forget(vm.songs.value.filter { it.artist == song.artist }.map { it.id }); menu = false })
            }
        }
    }
}

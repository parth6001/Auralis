package com.auralis.player.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.player.data.Song

@Composable
fun NowPlaying(vm: LibraryViewModel, song: Song, close: () -> Unit, saveQueue: () -> Unit) {
    val player by vm.playback.state.collectAsStateWithLifecycle()
    val songs by vm.songs.collectAsStateWithLifecycle()
    val audio by vm.audio.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var queue by remember { mutableStateOf(false) }
    var seek by remember(song.id) { mutableStateOf<Float?>(null) }
    val lyricsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { vm.loadLyrics(it, song.id) } }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding().padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = close) { Text("Back") }; Text("NOW PLAYING", Modifier.padding(top = 14.dp), style = MaterialTheme.typography.labelLarge); TextButton(onClick = { queue = true }) { Text("Queue") }
                }
                BoxWithConstraints(Modifier.weight(1f)) {
                    val wide = maxWidth >= 720.dp
                    if (wide) Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Artwork(song.uri, Modifier.weight(1f).aspectRatio(1f))
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            PlayerDetails(song, audio.report())
                            Text(settings["lyrics:${song.id}"] ?: "No local lyrics loaded.")
                            TextButton(onClick = { lyricsPicker.launch(arrayOf("text/*", "application/octet-stream")) }) { Text("Load local lyrics") }
                        }
                    } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Artwork(song.uri, Modifier.fillMaxWidth().aspectRatio(1f))
                        PlayerDetails(song, "${audio.path} · ${audio.devices.firstOrNull()?.name ?: "System output"}\nActual routed format unavailable.")
                        Text(settings["lyrics:${song.id}"] ?: "No local lyrics loaded.")
                        TextButton(onClick = { lyricsPicker.launch(arrayOf("text/*", "application/octet-stream")) }) { Text("Load local lyrics") }
                    }
                }
                Slider(seek ?: player.position.toFloat().coerceIn(0f, player.duration.coerceAtLeast(1).toFloat()), { seek = it },
                    onValueChangeFinished = { seek?.let { vm.playback.seek(it.toLong()) }; seek = null }, valueRange = 0f..player.duration.coerceAtLeast(1).toFloat(), modifier = Modifier.semantics { contentDescription = "Playback position" })
                Text("${time(player.position)} / ${time(player.duration)}", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = vm.playback::previous) { Text("Previous") }
                    Button(onClick = vm.playback::toggle) { Text(if (player.playing) "Pause" else "Play") }
                    TextButton(onClick = vm.playback::next) { Text("Next") }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(player.shuffle, vm.playback::shuffle, label = { Text("Shuffle") })
                    FilterChip(player.repeat != 0, vm.playback::repeat, label = { Text("Repeat ${listOf("off", "one", "all")[player.repeat]}") })
                    FilterChip(song.favourite, { vm.favourite(song.id) }, label = { Text(if (song.favourite) "Favourited" else "Favourite") })
                }
            }
        }
    }
    if (queue) AlertDialog(onDismissRequest = { queue = false }, title = { Text("Play queue") }, text = {
        LazyColumn(Modifier.heightIn(max = 440.dp)) {
            itemsIndexed(player.queue) { index, id ->
                Column {
                    Text("${index + 1}. ${songs.find { it.id == id }?.title ?: "Unavailable song"}", style = MaterialTheme.typography.titleSmall)
                    Row {
                        TextButton(onClick = { vm.playback.move(index, index - 1) }, enabled = index > 0) { Text("Up") }
                        TextButton(onClick = { vm.playback.move(index, index + 1) }, enabled = index + 1 < player.queue.size) { Text("Down") }
                        TextButton(onClick = { vm.playback.remove(index) }) { Text("Remove") }
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { queue = false; saveQueue() }) { Text("Save as playlist") } }, dismissButton = { TextButton(onClick = { queue = false }) { Text("Close queue") } })
}

@Composable
private fun PlayerDetails(song: Song, route: String) {
    Text(song.title, style = MaterialTheme.typography.headlineSmall)
    Text("${song.artist} · ${song.album}")
    Text("${song.codec.ifBlank { song.mime.ifBlank { "Unknown codec" } }}\n${if (song.bitrate > 0) "${song.bitrate / 1000} kbps" else "Unknown bitrate"} · ${if (song.sampleRate > 0) "${song.sampleRate} Hz" else "Unknown sample rate"}\n${if (song.bitDepth > 0) "${song.bitDepth} bit" else "Unknown bit depth"} · ${if (song.channels > 0) "${song.channels} channels" else "Unknown channels"}", style = MaterialTheme.typography.bodyMedium)
    Text(route, style = MaterialTheme.typography.bodySmall)
}
private fun time(ms: Long): String = "%d:%02d".format(ms / 60000, (ms / 1000) % 60)

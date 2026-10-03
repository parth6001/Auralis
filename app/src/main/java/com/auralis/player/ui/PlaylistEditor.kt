package com.auralis.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auralis.player.data.Song

data class PlaylistDraft(val id: String? = null, val name: String = "", val ids: List<String> = emptyList())

@Composable
fun PlaylistEditor(draft: PlaylistDraft, songs: List<Song>, save: (String, List<String>) -> Unit, close: () -> Unit) {
    var name by remember(draft) { mutableStateOf(draft.name) }
    var ids by remember(draft) { mutableStateOf(draft.ids) }
    var search by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = close, title = { Text(if (draft.id == null) "Create playlist" else "Edit playlist") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { if (it.length <= 80) name = it }, label = { Text("Playlist name") })
            OutlinedTextField(search, { search = it }, label = { Text("Find songs to include") })
            Text("${ids.size} songs selected")
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(songs.filter { it.title.contains(search, true) || it.artist.contains(search, true) }, key = { it.id }) { song ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(song.id in ids, { checked -> ids = if (checked) ids + song.id else ids.filterNot { it == song.id } })
                        Text(song.title, Modifier.weight(1f))
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { save(name, ids) }, enabled = name.isNotBlank()) { Text("Save playlist") } }, dismissButton = { TextButton(onClick = close) { Text("Cancel") } })
}

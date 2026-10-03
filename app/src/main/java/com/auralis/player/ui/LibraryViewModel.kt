package com.auralis.player.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.player.AuralisApp
import com.auralis.player.data.Folder
import com.auralis.player.data.Setting
import com.auralis.player.data.ProfileCodec
import com.auralis.player.audio.AudioProfile
import com.auralis.player.data.Playlist
import com.auralis.player.data.QueueState
import com.auralis.player.data.LocalBackup
import com.auralis.player.domain.Recommendations
import com.auralis.player.domain.SmartMixes
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID
import com.auralis.player.playback.PlaybackConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AuralisApp
    val dao = app.database.dao()
    val songs = dao.observeSongs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders = dao.observeFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history = dao.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = dao.observePlaylists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = dao.observeSettings().map { entries -> entries.associate { it.key to it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val playback = PlaybackConnection(application, viewModelScope)
    val scan = app.scanner.state
    val audio = app.audioStatus.state
    private val refreshTime = MutableStateFlow(System.currentTimeMillis())
    val recommendations = combine(songs, history, settings, refreshTime) { songs, history, settings, now ->
        if (settings["recommendations"] == "false") emptyList() else Recommendations.rank(songs, history, now, Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            settings["reduceRepetition"] != "false", settings.filter { it.key.startsWith("excludeArtist:") && it.value == "true" }.keys.map { it.removePrefix("excludeArtist:") }.toSet())
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mixes = combine(songs, history, settings, refreshTime) { songs, history, settings, now ->
        val allowed = songs.filter { settings["excludeArtist:${it.artist}"] != "true" }
        (SmartMixes.names + allowed.map { it.mood }.filter { it.isNotBlank() }.distinct().map { "Mood: $it" }).associateWith { if (settings["mix:$it"] == "disabled") emptyList() else SmartMixes.songs(it, allowed, history, now) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    fun refreshMixes() { refreshTime.value = System.currentTimeMillis() }
    fun exclude(id: String, value: Boolean) { viewModelScope.launch { dao.exclude(id, value) } }
    fun mood(id: String, value: String) { viewModelScope.launch { dao.mood(id, value.trim().take(40)) } }
    fun forget(ids: List<String>) { viewModelScope.launch {
        app.database.withTransaction {
            ids.forEach { dao.putSetting(Setting("forget:$it", System.currentTimeMillis().toString())) }
            ids.chunked(500).forEach { dao.forget(it) }
        }
    } }
    fun clearHistory() { viewModelScope.launch {
        app.database.withTransaction { dao.putSetting(Setting("historyEpoch", System.currentTimeMillis().toString())); dao.clearHistory() }
    } }
    fun savePlaylist(name: String, ids: List<String>, id: String = UUID.randomUUID().toString()) {
        if (name.isBlank()) return
        viewModelScope.launch { dao.playlist(Playlist(id, name.trim().take(80), ids.filter { songId -> songs.value.any { it.id == songId } }.joinToString("\n"))) }
    }
    fun deletePlaylist(id: String) { viewModelScope.launch { dao.deletePlaylist(id) } }
    fun removeFolder(folder: Folder) { viewModelScope.launch {
        cancelScan()
        scanJob?.join()
        app.database.withTransaction {
            val ids = dao.songsInRoot(folder.uri).map { it.id }
            ids.chunked(500).forEach { dao.deleteSongs(it); dao.forget(it) }
            dao.playlists().forEach { dao.playlist(it.copy(songIds = it.songIds.split('\n').filterNot { id -> id in ids }.joinToString("\n"))) }
            dao.deleteFolder(folder.uri)
        }
        runCatching { app.contentResolver.releasePersistableUriPermission(folder.uri.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    } }
    fun profile(profile: AudioProfile) {
        if (profile.valid()) setting("profile:${audio.value.profileKey}", ProfileCodec.encode(profile))
    }
    fun exportLocal(uri: Uri, preset: Boolean) { viewModelScope.launch {
        try { withContext(Dispatchers.IO) {
            val text = if (preset) ProfileCodec.encode(audio.value.profile) else LocalBackup(app.database).export()
            app.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) } ?: error("No output")
        }; message.value = "Local export saved." } catch (_: Exception) { message.value = "Export failed. Choose a writable location." }
    } }
    fun importLocal(uri: Uri, preset: Boolean) { viewModelScope.launch {
        try {
            val text = withContext(Dispatchers.IO) { readBounded(uri, if (preset) 16384 else 16 * 1024 * 1024) }
            if (preset) profile(ProfileCodec.decode(text)) else withContext(Dispatchers.IO) { LocalBackup(app.database).import(text) }
            message.value = "Import complete. Only already-authorized songs were matched."
        } catch (_: Exception) { message.value = "Import rejected: invalid, oversized, or unsupported file. No partial import was applied." }
    } }
    fun loadLyrics(uri: Uri, id: String) { viewModelScope.launch {
        try { val text = withContext(Dispatchers.IO) { readBounded(uri, 256 * 1024) }; dao.putSetting(Setting("lyrics:$id", text)) }
        catch (_: Exception) { message.value = "Cannot read lyrics. Select a UTF-8 text or LRC file below 256 KB." }
    } }
    private fun readBounded(uri: Uri, max: Int): String {
        return app.contentResolver.openInputStream(uri)?.use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                require(output.size() + count <= max)
                output.write(buffer, 0, count)
            }
            output.toString("UTF-8")
        } ?: error("No input")
    }
    private var scanJob: Job? = null
    val message = MutableStateFlow<String?>(null)
    fun addFolder(uri: Uri) {
        viewModelScope.launch {
            try {
                val newId = android.provider.DocumentsContract.getTreeDocumentId(uri)
                val overlap = dao.folders().any { folder ->
                    val oldUri = folder.uri.toUri()
                    val oldId = android.provider.DocumentsContract.getTreeDocumentId(oldUri)
                    oldUri.authority == uri.authority && oldId != newId && (newId.startsWith("$oldId/") || oldId.startsWith("$newId/"))
                }
                if (overlap) { message.value = "This folder overlaps an existing selection. Keep one parent folder to avoid duplicates."; return@launch }
                app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                dao.putFolder(Folder(uri.toString(), uri.lastPathSegment?.substringAfterLast(':') ?: "Music"))
                rescan()
            } catch (_: Exception) { message.value = "Folder permission was not granted. Choose the folder again." }
        }
    }
    fun rescan() { if (scanJob?.isActive != true) scanJob = viewModelScope.launch { app.scanner.scan() } }
    fun cancelScan() { scanJob?.cancel() }
    fun favourite(id: String) { viewModelScope.launch { dao.toggleFavourite(id) } }
    fun setting(key: String, value: String) { viewModelScope.launch {
        dao.putSetting(Setting(key, value))
        if (key == "restoreQueue" && value == "false") dao.queue(QueueState(ids = "", index = 0, position = 0, shuffle = false, repeat = 0))
    } }
    override fun onCleared() { playback.close() }
}

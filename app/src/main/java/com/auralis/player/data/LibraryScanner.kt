package com.auralis.player.data

import android.content.ContentResolver
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import androidx.core.net.toUri
import com.auralis.player.domain.LibraryRules
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScanState(val running: Boolean = false, val found: Int = 0, val skipped: Int = 0,
    val message: String = "Choose a music folder to start")

class LibraryScanner(private val context: Context, private val db: LibraryDatabase) {
    private val mutable = MutableStateFlow(ScanState())
    val state = mutable.asStateFlow()
    private val mutex = kotlinx.coroutines.sync.Mutex()

    suspend fun scan() {
        if (!mutex.tryLock()) return
        mutable.value = ScanState(running = true, message = "Reading selected folders")
        try {
            withContext(Dispatchers.IO) {
                for (root in db.dao().folders()) scanRoot(root)
            }
            mutable.value = mutable.value.copy(running = false, message = "Scan complete")
        } catch (e: CancellationException) {
            mutable.value = mutable.value.copy(running = false, message = "Scan cancelled; incomplete folder was not changed")
            throw e
        } catch (_: Exception) {
            mutable.value = mutable.value.copy(running = false, message = "Cannot read a selected folder. Re-select it to restore access, then rescan. Incomplete folder was not changed.")
        } finally { mutex.unlock() }
    }

    private suspend fun scanRoot(root: Folder) {
        val resolver = context.contentResolver
        val tree = root.uri.toUri()
        val old = db.dao().songsInRoot(root.uri)
        val byUri = old.associateBy { it.uri }
        val found = mutableListOf<Song>()
        val seenUris = mutableSetOf<String>()
        val dirs = ArrayDeque<Pair<String, String>>()
        val visited = mutableSetOf<String>()
        dirs.add(DocumentsContract.getTreeDocumentId(tree) to root.name)
        while (dirs.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val (docId, folder) = dirs.removeFirst()
            if (!visited.add(docId)) continue
            check(visited.size <= 100_000) { "Folder tree too large" }
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
            val projection = arrayOf("document_id", "_display_name", "mime_type", "_size", "last_modified")
            val cursor = resolver.query(children, projection, null, null, null) ?: error("Provider returned no snapshot")
            cursor.use {
                while (it.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    val id = it.getString(0)
                    val name = it.getString(1) ?: "Untitled"
                    val mime = it.getString(2).orEmpty()
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        dirs.add(id to "$folder/$name")
                    } else if (mime.startsWith("audio/") || name.substringAfterLast('.').lowercase() in LibraryRules.extensions) {
                        val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                        if (!seenUris.add(uri.toString())) continue
                        val previous = byUri[uri.toString()]
                        val size = it.getLong(3)
                        val modified = it.getLong(4)
                        val song = if (previous != null && modified > 0 && previous.modified == modified && previous.size == size) previous.copy(folder = folder)
                        else readSong(resolver, uri, root.uri, folder, name, size, modified, mime, previous)
                        found.add(song)
                        mutable.value = mutable.value.copy(found = mutable.value.found + 1, message = "Scanning audio files")
                    }
                }
            }
        }
        currentCoroutineContext().ensureActive()
        // Only a unique disappeared fingerprint is a move. Multiple copies remain distinct songs.
        val missing = old.filter { it.uri !in seenUris }
        val newFingerprints = found.filter { it.uri !in byUri }.groupBy { it.fingerprint }
        val missingFingerprints = missing.groupBy { it.fingerprint }
        val reconciled = found.map { song ->
            val match = missingFingerprints[song.fingerprint]?.singleOrNull()
            if (song.uri !in byUri && song.fingerprint.isNotEmpty() && newFingerprints[song.fingerprint]?.size == 1 && match != null)
                song.copy(id = match.id, added = match.added, favourite = match.favourite, excluded = match.excluded, mood = match.mood)
            else song
        }
        val retained = reconciled.map { it.id }.toSet()
        db.withTransaction {
            val deleted = old.filter { it.id !in retained }.map { it.id }
            deleted.chunked(500).forEach { db.dao().deleteSongs(it); db.dao().forget(it) }
            db.dao().putSongs(reconciled)
            if (deleted.isNotEmpty()) {
                val gone = deleted.toSet()
                db.dao().playlists().forEach { list ->
                    db.dao().playlist(list.copy(songIds = list.songIds.split('\n').filterNot { it in gone }.joinToString("\n")))
                }
            }
        }
    }

    private fun readSong(resolver: ContentResolver, uri: Uri, root: String, folder: String, name: String,
        size: Long, modified: Long, mime: String, previous: Song?): Song {
        var result = Song(id = previous?.id ?: UUID.randomUUID().toString(), uri = uri.toString(), root = root,
            folder = folder, title = name.substringBeforeLast('.', name), size = size, modified = modified,
            mime = mime, added = previous?.added ?: System.currentTimeMillis(), favourite = previous?.favourite ?: false,
            excluded = previous?.excluded ?: false, mood = previous?.mood.orEmpty())
        // Reading a bounded prefix detects probable copies without hashing every byte of long tracks.
        resolver.openInputStream(uri)?.use { stream ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(32 * 1024)
            var remaining = 128 * 1024
            while (remaining > 0) {
                val count = stream.read(buffer, 0, minOf(buffer.size, remaining))
                if (count < 0) break
                digest.update(buffer, 0, count); remaining -= count
            }
            result = result.copy(fingerprint = "$size:" + digest.digest().joinToString("") { "%02x".format(it) })
        } ?: error("File access unavailable")
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            fun m(key: Int) = retriever.extractMetadata(key)
            result = result.copy(
                title = LibraryRules.text(m(7), result.title), artist = LibraryRules.text(m(2), "Unknown artist"),
                album = LibraryRules.text(m(1), "Unknown album"), albumArtist = LibraryRules.text(m(13), ""),
                genre = LibraryRules.text(m(6), ""), track = LibraryRules.text(m(0), ""), disc = LibraryRules.text(m(14), ""),
                year = LibraryRules.text(m(8), ""), duration = m(9)?.toLongOrNull()?.coerceAtLeast(0) ?: 0,
                bitrate = m(20)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                mime = m(12) ?: mime,
            )
        } catch (_: RuntimeException) { mutable.value = mutable.value.copy(skipped = mutable.value.skipped + 1) }
        finally { retriever.release() }
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val codec = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (!codec.startsWith("audio/")) continue
                fun int(key: String) = if (format.containsKey(key)) format.getInteger(key) else 0
                result = result.copy(codec = codec, sampleRate = int(MediaFormat.KEY_SAMPLE_RATE), channels = int(MediaFormat.KEY_CHANNEL_COUNT),
                    bitDepth = when (int("pcm-encoding")) { 2 -> 16; 3 -> 8; 4 -> 32; 21 -> 24; 22 -> 32; else -> 0 })
                break
            }
        } catch (_: java.io.IOException) { /* Corrupt/truncated content can fail extractor setup after the URI was read successfully. */ }
        catch (_: RuntimeException) { /* Playback reports unsupported codecs independently. */ }
        finally { extractor.release() }
        return result
    }
}

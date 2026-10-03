package com.auralis.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auralis.player.data.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest

data class PlaybackState(val connected: Boolean = false, val id: String? = null, val playing: Boolean = false,
    val position: Long = 0, val duration: Long = 0, val queue: List<String> = emptyList(), val index: Int = 0,
    val shuffle: Boolean = false, val repeat: Int = 0, val error: String? = null)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackConnection(context: Context, private val scope: CoroutineScope) : AutoCloseable {
    private val mutable = MutableStateFlow(PlaybackState())
    val state = mutable.asStateFlow()
    private var controller: MediaController? = null
    private val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
    private var ticker: Job? = null
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { update() }
    }
    init {
        future.addListener({
            try {
                controller = future.get().also { it.addListener(listener) }
                update()
                ticker = scope.launch {
                    mutable.subscriptionCount.collectLatest { subscribers ->
                        if (subscribers > 0) while (isActive) { delay(500); update() }
                    }
                }
            } catch (_: Exception) { mutable.value = mutable.value.copy(error = "Playback service unavailable. Reopen the app.") }
        }, ContextCompat.getMainExecutor(context))
    }
    private fun update() {
        val c = controller ?: return
        mutable.value = PlaybackState(true, c.currentMediaItem?.mediaId, c.isPlaying, c.currentPosition.coerceAtLeast(0),
            c.duration.coerceAtLeast(0), (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId }, c.currentMediaItemIndex,
            c.shuffleModeEnabled, c.repeatMode, c.playerError?.let { "Cannot play this file. Check folder access or try another supported format." })
    }
    fun play(songs: List<Song>, index: Int = 0) {
        if (songs.isEmpty()) return
        controller?.apply { setMediaItems(songs.map { it.mediaItem() }, index.coerceIn(songs.indices), 0); prepare(); play() }
    }
    fun toggle() { controller?.apply { if (isPlaying) pause() else { prepare(); play() } } }
    fun seek(position: Long) { controller?.seekTo(position.coerceAtLeast(0)) }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPrevious() }
    fun shuffle() { controller?.apply { shuffleModeEnabled = !shuffleModeEnabled } }
    fun repeat() { controller?.apply { repeatMode = (repeatMode + 1) % 3 } }
    fun playNext(song: Song) { controller?.apply { addMediaItem((currentMediaItemIndex + 1).coerceIn(0, mediaItemCount), song.mediaItem()) } }
    fun remove(index: Int) { controller?.apply { if (index in 0 until mediaItemCount) removeMediaItem(index) } }
    fun move(from: Int, to: Int) { controller?.apply { if (from in 0 until mediaItemCount && to in 0 until mediaItemCount) moveMediaItem(from, to) } }
    override fun close() { ticker?.cancel(); controller?.removeListener(listener); MediaController.releaseFuture(future) }
}

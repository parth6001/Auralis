package com.auralis.player.playback

import android.os.SystemClock
import androidx.media3.common.Player
import androidx.room.withTransaction
import com.auralis.player.data.LibraryDatabase
import com.auralis.player.data.Listen
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Counts elapsed audible playback time, never seek distance or paused wall-clock time. */
class ListeningTracker(private val player: Player, private val db: LibraryDatabase, private val scope: CoroutineScope) : Player.Listener {
    private var songId: String? = null
    private var started = System.currentTimeMillis()
    private var duration = 0L
    private var played = 0L
    private var tickingAt: Long? = null
    private var paused = false
    fun privacyChanged(historyPaused: Boolean) {
        if (paused != historyPaused) { paused = historyPaused; played = 0; started = System.currentTimeMillis(); tickingAt = null }
        if (!paused && player.isPlaying && tickingAt == null) tickingAt = SystemClock.elapsedRealtime()
    }
    private fun tick() {
        tickingAt?.let { played += (SystemClock.elapsedRealtime() - it).coerceAtLeast(0) }
        tickingAt = if (player.isPlaying && !paused) SystemClock.elapsedRealtime() else null
    }
    override fun onIsPlayingChanged(isPlaying: Boolean) { tick() }
    override fun onEvents(player: Player, events: Player.Events) { if (player.duration > 0) duration = player.duration }
    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        tick(); finish(reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
        songId = mediaItem?.mediaId; started = System.currentTimeMillis(); played = 0
        duration = 0; tickingAt = if (player.isPlaying && !paused) SystemClock.elapsedRealtime() else null
    }
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (player.duration > 0) duration = player.duration
        if (playbackState == Player.STATE_ENDED) { tick(); finish(false); played = 0; tickingAt = null }
    }
    fun close() { tick(); finish(false) }
    private fun finish(manualSkip: Boolean) {
        val id = songId ?: return
        if (paused || played < 1000) return
        val at = started
        val counted = played
        val length = duration
        val calendar = Calendar.getInstance().apply { timeInMillis = at }
        val event = Listen(songId = id, at = at, playedMs = counted, durationMs = length,
            completed = length > 0 && counted >= length * .9, skipped = manualSkip && counted < 30_000 && (length <= 0 || counted < length * .25),
            hour = calendar.get(Calendar.HOUR_OF_DAY), day = calendar.get(Calendar.DAY_OF_WEEK))
        scope.launch {
            db.withTransaction {
                val dao = db.dao()
                val epoch = maxOf(dao.setting("historyEpoch")?.toLongOrNull() ?: 0, dao.setting("forget:$id")?.toLongOrNull() ?: 0)
                if (dao.setting("historyPaused") != "true" && at > epoch && dao.song(id) != null) dao.listen(event)
            }
        }
    }
}

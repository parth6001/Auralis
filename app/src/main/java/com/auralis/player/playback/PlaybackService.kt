package com.auralis.player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import android.content.Context
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.auralis.player.AuralisApp
import com.auralis.player.MainActivity
import com.auralis.player.data.QueueState
import com.auralis.player.data.Song
import com.auralis.player.data.ProfileCodec
import com.auralis.player.audio.*
import kotlinx.coroutines.*

fun Song.mediaItem(): MediaItem = MediaItem.Builder().setMediaId(id).setUri(uri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).build()).build()

@UnstableApi
class PlaybackService : MediaSessionService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val dao by lazy { (application as AuralisApp).database.dao() }
    private var ready = false
    private val processor = IemProcessor()
    private lateinit var routing: AudioRouting
    private val audioStore by lazy { (application as AuralisApp).audioStatus }
    private var pauseOnDisconnect = true
    private var profileJob: Job? = null
    private var preferences: Map<String, String> = emptyMap()
    private lateinit var tracker: ListeningTracker

    private fun applyProfile() {
        val key = audioStore.mutable.value.profileKey
        val value = preferences["profile:$key"]
        val profile = value?.let { runCatching { ProfileCodec.decode(it) }.getOrNull() } ?: AudioProfile(name = audioStore.mutable.value.devices.firstOrNull()?.name?.take(80) ?: "Speaker")
        processor.profile = profile
        audioStore.mutable.value = audioStore.mutable.value.copy(profile = profile, dspSupported = processor.supported)
    }

    override fun onCreate() {
        super.onCreate()
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
                DefaultAudioSink.Builder(context).setAudioProcessors(arrayOf(processor)).build()
        }
        player = ExoPlayer.Builder(this, renderers).setUsePlatformDiagnostics(false).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        val activity = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, player).setSessionActivity(activity).build()
        tracker = ListeningTracker(player, (application as AuralisApp).database, (application as AuralisApp).persistenceScope)
        player.addListener(tracker)
        routing = AudioRouting(this, changed = { devices ->
            val key = devices.firstOrNull()?.key ?: "Speaker||"
            audioStore.mutable.value = audioStore.mutable.value.copy(devices = devices, profileKey = key)
            applyProfile()
        }, removed = { if (pauseOnDisconnect) player.pause() })
        profileJob = scope.launch {
            dao.observeSettings().collect { entries ->
                preferences = entries.associate { it.key to it.value }
                pauseOnDisconnect = preferences["pauseOnDisconnect"] != "false"
                tracker.privacyChanged(preferences["historyPaused"] == "true")
                player.setHandleAudioBecomingNoisy(pauseOnDisconnect)
                applyProfile()
            }
        }
        player.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                processor.trackGain = null; processor.albumGain = null
                for (group in tracks.groups) for (index in 0 until group.length) {
                    if (!group.isTrackSelected(index)) continue
                    val metadata = group.getTrackFormat(index).metadata ?: continue
                    for (entryIndex in 0 until metadata.length()) {
                        val entry = metadata[entryIndex]
                        val pair = when (entry) {
                            is VorbisComment -> entry.key to entry.value
                            is TextInformationFrame -> (entry.description ?: entry.id) to entry.values.firstOrNull().orEmpty()
                            else -> null
                        } ?: continue
                        when (pair.first.uppercase(java.util.Locale.ROOT)) {
                            "REPLAYGAIN_TRACK_GAIN" -> processor.trackGain = ReplayGain.parse(pair.second)
                            "REPLAYGAIN_ALBUM_GAIN" -> processor.albumGain = ReplayGain.parse(pair.second)
                        }
                    }
                }
                audioStore.mutable.value = audioStore.mutable.value.copy(gainInfo = "ReplayGain track=${processor.trackGain ?: "unavailable"} dB; album=${processor.albumGain ?: "unavailable"} dB")
            }
            override fun onEvents(player: Player, events: Player.Events) {
                if (ready) persist()
                audioStore.mutable.value = audioStore.mutable.value.copy(dspSupported = processor.supported)
            }
        })
        scope.launch {
            if (dao.setting("restoreQueue") != "false") {
                val saved = dao.queue()
                if (saved != null && player.mediaItemCount == 0) {
                    val songs = dao.songs().associateBy { it.id }
                    val ids = saved.ids.split('\n')
                    val queue = ids.mapNotNull { songs[it] }
                    if (queue.isNotEmpty() && player.mediaItemCount == 0) {
                        val current = ids.getOrNull(saved.index)
                        val index = queue.indexOfFirst { it.id == current }.coerceAtLeast(0)
                        player.setMediaItems(queue.map { it.mediaItem() }, index, if (queue[index].id == current) saved.position else 0)
                        player.shuffleModeEnabled = saved.shuffle
                        player.repeatMode = saved.repeat.coerceIn(0, 2)
                        player.prepare() // Never autoplay after service/process recreation.
                    }
                }
            }
            ready = true
            while (isActive) { delay(5000); persist() }
        }
    }

    private fun persist() {
        val state = snapshot()
        scope.launch { if (dao.setting("restoreQueue") != "false") dao.queue(state) }
    }
    private fun snapshot() = QueueState(ids = (0 until player.mediaItemCount).joinToString("\n") { player.getMediaItemAt(it).mediaId },
        index = player.currentMediaItemIndex.coerceAtLeast(0), position = player.currentPosition.coerceAtLeast(0),
        shuffle = player.shuffleModeEnabled, repeat = player.repeatMode)

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) stopSelf()
    }
    override fun onDestroy() {
        val finalState = snapshot()
        if (ready) (application as AuralisApp).persistenceScope.launch { if (dao.setting("restoreQueue") != "false") dao.queue(finalState) }
        tracker.close(); routing.close(); profileJob?.cancel(); session?.release(); player.release(); scope.cancel(); super.onDestroy()
    }
}

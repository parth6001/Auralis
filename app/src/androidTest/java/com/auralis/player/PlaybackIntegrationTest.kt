package com.auralis.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.util.UnstableApi
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test

@UnstableApi
class PlaybackIntegrationTest {
    @Test fun generatedWavDecodesSeeksAndEnds() = decode("tone-stereo-44100.wav")
    @Test fun generatedMp3DecodesSeeksAndEnds() = decode("tone.mp3")
    @Test fun generatedFlacDecodesSeeksAndEnds() = decode("tone.flac")
    @Test fun generatedAacDecodesSeeksAndEnds() = decode("tone.m4a")
    @Test fun generatedVorbisDecodesSeeksAndEnds() = decode("tone.ogg")
    @Test fun generatedOpusDecodesSeeksAndEnds() = decode("tone.opus")
    @Test fun generatedHighRateFlacDecodesSeeksAndEnds() = decode("tone-96000.flac")
    @Test fun alacPlaysOrReportsUnsupportedDecoder() = decode("tone-alac.m4a", allowUnsupported = true)
    private fun decode(name: String, allowUnsupported: Boolean = false) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val fixture = File(context.cacheDir, name)
        instrumentation.context.assets.open("media/$name").use { input -> fixture.outputStream().use { input.copyTo(it) } }
        val prepared = CountDownLatch(1)
        val ended = CountDownLatch(1)
        val error = AtomicReference<PlaybackException?>()
        lateinit var player: ExoPlayer
        instrumentation.runOnMainSync {
            player = ExoPlayer.Builder(context).build()
            player.volume = 0f
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) prepared.countDown()
                    if (playbackState == Player.STATE_ENDED) ended.countDown()
                }
                override fun onPlayerError(e: PlaybackException) { error.set(e); prepared.countDown(); ended.countDown() }
            })
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(fixture))); player.prepare()
        }
        try {
            assertTrue("$name decoder did not become ready", prepared.await(20, TimeUnit.SECONDS))
            if (allowUnsupported && error.get() != null) {
                assertTrue("Unexpected ALAC failure: ${error.get()?.errorCode}", error.get()!!.errorCode in setOf(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED, PlaybackException.ERROR_CODE_DECODER_INIT_FAILED))
                println("FORMAT $name: unsupported platform decoder (${error.get()!!.errorCode}); graceful error verified")
                return
            }
            assertNull(error.get())
            instrumentation.runOnMainSync { assertTrue(player.duration in 1950..2050); player.seekTo(1000); player.play() }
            assertTrue("Playback did not reach end", ended.await(15, TimeUnit.SECONDS))
            assertNull(error.get())
            println("FORMAT $name: decode, seek, end passed")
        } finally { instrumentation.runOnMainSync { player.release() }; fixture.delete() }
    }
}

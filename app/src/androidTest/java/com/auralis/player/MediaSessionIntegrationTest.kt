package com.auralis.player

import android.app.NotificationManager
import android.content.ComponentName
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.auralis.player.data.Song
import com.auralis.player.playback.PlaybackService
import com.auralis.player.playback.mediaItem
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MediaSessionIntegrationTest {
    @Test fun backgroundServiceNotificationMediaButtonAndQueue() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val fixture = File(context.cacheDir, "session-tone.wav")
        instrumentation.context.assets.open("media/tone-stereo-44100.wav").use { input -> fixture.outputStream().use { input.copyTo(it) } }
        val song = Song("session-test", Uri.fromFile(fixture).toString(), "integration", "integration", "Session test tone")
        val dao = (context.applicationContext as AuralisApp).database.dao()
        runBlocking { dao.putSongs(listOf(song)) }
        val activity = ActivityScenario.launch(MainActivity::class.java)
        val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        val controller = future.get(20, TimeUnit.SECONDS)
        fun await(description: String, condition: () -> Boolean) {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
            var done = false
            while (System.nanoTime() < deadline && !done) {
                instrumentation.runOnMainSync { done = condition() }
                if (!done) Thread.sleep(25) // Deadline-based condition polling, not a fixed test delay.
            }
            assertTrue(description, done)
        }
        try {
            instrumentation.runOnMainSync {
                controller.setMediaItems(listOf(song.mediaItem(), song.copy(id = "queue-second", title = "Second").mediaItem()))
                controller.repeatMode = Player.REPEAT_MODE_ALL
                controller.prepare(); controller.play()
            }
            await("Service did not play") { controller.isPlaying }
            await("Media notification was not posted") { context.getSystemService(NotificationManager::class.java).activeNotifications.isNotEmpty() }
            activity.onActivity { it.moveTaskToBack(true) }
            await("Background playback stopped") { controller.isPlaying }
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_MEDIA_PAUSE")).use { it.readBytes() }
            await("System media pause did not reach the session") { !controller.playWhenReady }
            instrumentation.runOnMainSync {
                controller.moveMediaItem(1, 0)
                assertEquals("queue-second", controller.getMediaItemAt(0).mediaId)
                controller.removeMediaItem(0)
                controller.seekTo(750)
            }
            context.startActivity(android.content.Intent(context, MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
            activity.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            activity.recreate()
            await("Queue did not survive activity recreation") { controller.mediaItemCount == 1 && controller.currentMediaItem?.mediaId == song.id }
        } finally {
            instrumentation.runOnMainSync { controller.stop(); controller.clearMediaItems(); MediaController.releaseFuture(future) }
            activity.close()
            runBlocking { dao.deleteSongs(listOf(song.id)); dao.forget(listOf(song.id)) }
            fixture.delete()
        }
    }
}

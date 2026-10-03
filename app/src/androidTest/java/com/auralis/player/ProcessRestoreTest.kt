package com.auralis.player

import android.content.ComponentName
import android.net.Uri
import android.os.Process
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import com.auralis.player.data.QueueState
import com.auralis.player.data.Setting
import com.auralis.player.data.Song
import com.auralis.player.playback.PlaybackService
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ProcessRestoreTest {
    // run-process-restore.ps1 also runs this across an actual force-stop boundary.
    @Test fun restoresCheckpointPaused() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val dao = (context.applicationContext as AuralisApp).database.dao()
        val phase = InstrumentationRegistry.getArguments().getString("restorePhase", "both")
        val fixture = File(context.filesDir, "restore-tone.wav")
        if (phase == "both") {
            instrumentation.runOnMainSync { context.stopService(android.content.Intent(context, PlaybackService::class.java)) }
            instrumentation.waitForIdleSync()
        }
        if (phase != "verify") {
            instrumentation.context.assets.open("media/tone-stereo-44100.wav").use { input -> fixture.outputStream().use { input.copyTo(it) } }
            runBlocking {
                dao.putSongs(listOf(Song("restore-one", Uri.fromFile(fixture).toString(), "restore-test", "restore-test", "Restored tone")))
                dao.putSetting(Setting("restoreQueue", "true"))
                dao.putSetting(Setting("restoreTestPid", Process.myPid().toString()))
                dao.queue(QueueState(ids = "restore-one", index = 0, position = 750, shuffle = true, repeat = Player.REPEAT_MODE_ALL))
                assertEquals(750L, dao.queue()!!.position)
            }
        }
        if (phase == "seed") return
        if (phase == "verify") runBlocking { assertNotEquals(dao.setting("restoreTestPid"), Process.myPid().toString()) }
        val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        val controller = future.get(20, TimeUnit.SECONDS)
        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
            var restored = false
            while (!restored && System.nanoTime() < deadline) {
                instrumentation.runOnMainSync { restored = controller.currentMediaItem?.mediaId == "restore-one" && controller.playbackState == Player.STATE_READY }
                if (!restored) Thread.sleep(25)
            }
            assertTrue("Queue checkpoint was not restored", restored)
            instrumentation.runOnMainSync {
                assertFalse(controller.playWhenReady)
                assertFalse(controller.isPlaying)
                assertEquals(750L, controller.currentPosition)
                assertTrue(controller.shuffleModeEnabled)
                assertEquals(Player.REPEAT_MODE_ALL, controller.repeatMode)
            }
        } finally {
            instrumentation.runOnMainSync { controller.stop(); controller.clearMediaItems(); MediaController.releaseFuture(future) }
            runBlocking {
                dao.deleteSongs(listOf("restore-one")); dao.forget(listOf("restore-one"))
                dao.queue(QueueState(ids = "", index = 0, position = 0, shuffle = false, repeat = Player.REPEAT_MODE_OFF))
            }
            fixture.delete()
        }
    }
}

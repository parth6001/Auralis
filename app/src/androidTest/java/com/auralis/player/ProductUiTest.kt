package com.auralis.player

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.auralis.player.data.*
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProductUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val dao get() = (context.applicationContext as AuralisApp).database.dao()
    private lateinit var fixture: File
    @Before fun seed() = runBlocking {
        fixture = File(context.filesDir, "ui-fixture.wav")
        InstrumentationRegistry.getInstrumentation().context.assets.open("media/tone-stereo-44100.wav").use { input -> fixture.outputStream().use { input.copyTo(it) } }
        dao.putSongs(listOf(Song("ui-one", Uri.fromFile(fixture).toString(), "ui-test", "ui-test", "Test First Tone", duration = 2000), Song("ui-two", Uri.fromFile(fixture).toString() + "#second", "ui-test", "ui-test", "Test Second Tone", duration = 2000)))
        dao.putSetting(Setting("recommendations", "false"))
    }
    @After fun cleanup(): Unit = runBlocking {
        dao.deleteSongs(listOf("ui-one", "ui-two")); dao.forget(listOf("ui-one", "ui-two"))
        dao.playlists().filter { it.name == "UI queue playlist" }.forEach { dao.deletePlaylist(it.id) }
        dao.putSetting(Setting("recommendations", "true")); dao.putSetting(Setting("theme", "System"))
        fixture.delete()
    }
    @Test fun queuePlaylistThemesAndPrivacyReset() {
        compose.onNodeWithText("Library").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Test First Tone").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Test First Tone").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Pause").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Pause").performClick()
        compose.onAllNodesWithText("Test First Tone").onLast().performClick()
        compose.onNodeWithContentDescription("Playback position").assertExists()
        compose.onNodeWithText("Queue").performClick()
        compose.onAllNodesWithText("Down").onFirst().performClick()
        compose.onNodeWithText("Save as playlist").performClick()
        compose.onNodeWithText("Playlist name").performTextReplacement("UI queue playlist")
        compose.onNodeWithText("Save playlist").performClick()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Mixes").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("UI queue playlist"))
        compose.onNodeWithText("UI queue playlist").assertIsDisplayed()
        compose.onNodeWithText("Privacy & settings").performClick()
        for (theme in listOf("Light", "Dark", "AMOLED")) {
            compose.onNodeWithText(theme).performScrollTo().performClick().assertIsSelected()
        }
        compose.onNodeWithText("Clear listening history").performScrollTo().performClick()
        compose.onNodeWithText("Clear all listening history?").assertIsDisplayed()
        compose.onNodeWithText("Clear history").performClick()
        compose.waitUntil(10000) { runBlocking { dao.history().isEmpty() } }
    }
}

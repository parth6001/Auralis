package com.auralis.player

import android.provider.DocumentsContract
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.auralis.player.data.*
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ScannerIntegrationTest {
    @Test fun scanMoveDuplicateDeleteAndProviderFailure() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val root = File(context.filesDir, "scanner-fixtures").apply { mkdirs() }
        root.listFiles()?.forEach { it.deleteRecursively() }
        val original = File(root, "tone.wav")
        instrumentation.context.assets.open("media/tone-stereo-44100.wav").use { input -> original.outputStream().use { input.copyTo(it) } }
        val db = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build()
        try {
            val uri = DocumentsContract.buildTreeDocumentUri("com.auralis.player.fixtures", "root")
            db.dao().putFolder(Folder(uri.toString(), "Test music"))
            val scanner = LibraryScanner(context, db)
            scanner.scan()
            assertEquals("Scan complete", scanner.state.value.message)
            assertEquals(1, db.dao().songs().size)
            val first = db.dao().songs().single()
            db.dao().toggleFavourite(first.id)
            scanner.scan(); assertEquals(first.id, db.dao().songs().single().id)
            val moved = File(root, "moved.wav")
            assertTrue(original.renameTo(moved))
            scanner.scan()
            assertEquals(first.id, db.dao().songs().single().id)
            assertTrue(db.dao().songs().single().favourite)
            moved.copyTo(File(root, "copy.wav"))
            scanner.scan(); assertEquals(2, db.dao().songs().size)
            File(root, ".fail").writeText("failure")
            moved.delete(); scanner.scan()
            assertEquals(2, db.dao().songs().size)
            assertTrue(scanner.state.value.message.contains("Cannot read"))
            File(root, ".fail").delete(); scanner.scan()
            assertEquals(1, db.dao().songs().size)
            for (name in listOf("corrupt.mp3", "truncated.wav", "tagged.mp3")) {
                instrumentation.context.assets.open("media/$name").use { input -> File(root, name).outputStream().use { input.copyTo(it) } }
            }
            scanner.scan()
            assertEquals("Scan complete", scanner.state.value.message)
            assertEquals(4, db.dao().songs().size)
            assertTrue(scanner.state.value.skipped >= 2)
            val tagged = db.dao().songs().single { it.title == "Auralis 音楽" }
            assertTrue(tagged.album.startsWith("Auralis long "))
            assertEquals(2048, tagged.album.length)
        } finally { db.close(); root.deleteRecursively() }
    }
}

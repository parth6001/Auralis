package com.auralis.player

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.auralis.player.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun upsertIsIdempotentAndQueuePersists() = runBlocking {
        val name = "integration-${System.nanoTime()}.db"
        var db = Room.databaseBuilder(context, LibraryDatabase::class.java, name).addMigrations(LibraryDatabase.MIGRATION_1_2).build()
        try {
            val song = Song("one", "content://test/one", "root", "folder", "Tone")
            db.dao().putSongs(listOf(song)); db.dao().putSongs(listOf(song.copy(title = "Updated")))
            assertEquals(1, db.dao().songs().size)
            db.dao().queue(QueueState(ids = "one", index = 0, position = 1250, shuffle = true, repeat = 2))
            db.close()
            db = Room.databaseBuilder(context, LibraryDatabase::class.java, name).addMigrations(LibraryDatabase.MIGRATION_1_2).build()
            assertEquals("Updated", db.dao().song("one")?.title)
            assertEquals(1250L, db.dao().queue()?.position)
            assertTrue(db.dao().queue()!!.shuffle)
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun historyCanBeForgottenAndCleared() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build()
        try {
            db.dao().listen(Listen(songId = "one", at = 1, playedMs = 100, durationMs = 100, completed = true, skipped = false, hour = 8, day = 1))
            db.dao().forget(listOf("one"))
            val count = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM Listen").use { it.moveToFirst(); it.getInt(0) }
            assertEquals(0, count)
            db.dao().clearHistory()
        } finally { db.close() }
    }
}

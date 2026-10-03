package com.auralis.player

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.auralis.player.audio.AudioProfile
import com.auralis.player.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ImportSafetyTest {
    @Test fun presetRoundTripAndMalformedImportAreSafe() {
        val profile = AudioProfile(name = "USB + IEM")
        assertEquals(profile, ProfileCodec.decode(ProfileCodec.encode(profile)))
        assertTrue(runCatching { ProfileCodec.decode("{broken") }.isFailure)
        assertTrue(runCatching { ProfileCodec.decode(" ".repeat(17000)) }.isFailure)
    }
    @Test fun backupMergeIsIdempotentAndDoesNotImportStorage() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build()
        try {
            val song = Song("one", "content://local/one", "root", "folder", "Tone", favourite = true, fingerprint = "testhash")
            db.dao().putSongs(listOf(song)); db.dao().putSetting(Setting("theme", "AMOLED"))
            val backup = LocalBackup(db)
            val text = backup.export()
            db.dao().toggleFavourite(song.id)
            backup.import(text); backup.import(text)
            assertTrue(db.dao().song("one")!!.favourite)
            assertEquals(1, db.dao().songs().size)
            assertTrue(db.dao().folders().isEmpty())
            assertTrue(runCatching { backup.import(text.replace("AMOLED", "invalid-theme")) }.isFailure)
            assertEquals("AMOLED", db.dao().setting("theme"))
        } finally { db.close() }
    }
}

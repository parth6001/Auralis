package com.auralis.player

import android.app.Application
import androidx.room.Room
import com.auralis.player.data.LibraryDatabase
import com.auralis.player.data.LibraryScanner
import com.auralis.player.audio.AudioStatusStore

class AuralisApp : Application() {
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.DEBUG) androidx.media3.common.util.Log.setLogLevel(androidx.media3.common.util.Log.LOG_LEVEL_OFF)
    }
    val persistenceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    val audioStatus = AudioStatusStore()
    val database by lazy {
        Room.databaseBuilder(this, LibraryDatabase::class.java, "auralis.db")
            .addMigrations(LibraryDatabase.MIGRATION_1_2).build()
    }
    val scanner by lazy { LibraryScanner(this, database) }
}

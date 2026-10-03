package com.auralis.player.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(indices = [Index(value = ["uri"], unique = true), Index("root")])
data class Song(
    @PrimaryKey val id: String,
    val uri: String,
    val root: String,
    val folder: String,
    val title: String,
    val artist: String = "Unknown artist",
    val album: String = "Unknown album",
    val albumArtist: String = "",
    val genre: String = "",
    val track: String = "",
    val disc: String = "",
    val year: String = "",
    val duration: Long = 0,
    val size: Long = 0,
    val modified: Long = 0,
    val added: Long = System.currentTimeMillis(),
    val mime: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val channels: Int = 0,
    val bitDepth: Int = 0,
    val fingerprint: String = "",
    val favourite: Boolean = false,
    val excluded: Boolean = false,
    @ColumnInfo(defaultValue = "''") val mood: String = "",
)

@Entity data class Folder(@PrimaryKey val uri: String, val name: String)
@Entity(indices = [Index("songId")])
data class Listen(@PrimaryKey(autoGenerate = true) val id: Long = 0, val songId: String,
    val at: Long, val playedMs: Long, val durationMs: Long, val completed: Boolean, val skipped: Boolean,
    val hour: Int, val day: Int)
@Entity data class Playlist(@PrimaryKey val id: String, val name: String, val songIds: String)
@Entity data class Setting(@PrimaryKey val key: String, val value: String)
@Entity data class QueueState(@PrimaryKey val id: Int = 0, val ids: String, val index: Int, val position: Long,
    val shuffle: Boolean, val repeat: Int)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM Song ORDER BY title COLLATE NOCASE") fun observeSongs(): Flow<List<Song>>
    @Query("SELECT * FROM Song") suspend fun songs(): List<Song>
    @Query("SELECT * FROM Song WHERE root = :root") suspend fun songsInRoot(root: String): List<Song>
    @Query("SELECT * FROM Song WHERE id = :id") suspend fun song(id: String): Song?
    @Upsert suspend fun putSongs(songs: List<Song>)
    @Query("DELETE FROM Song WHERE id IN (:ids)") suspend fun deleteSongs(ids: List<String>)
    @Query("UPDATE Song SET favourite = NOT favourite WHERE id = :id") suspend fun toggleFavourite(id: String)
    @Query("UPDATE Song SET excluded = :excluded WHERE id = :id") suspend fun exclude(id: String, excluded: Boolean)
    @Query("UPDATE Song SET mood = :mood WHERE id = :id") suspend fun mood(id: String, mood: String)
    @Query("SELECT * FROM Folder") fun observeFolders(): Flow<List<Folder>>
    @Query("SELECT * FROM Folder") suspend fun folders(): List<Folder>
    @Upsert suspend fun putFolder(folder: Folder)
    @Query("DELETE FROM Folder WHERE uri = :uri") suspend fun deleteFolder(uri: String)
    @Query("SELECT * FROM Listen ORDER BY at DESC") fun observeHistory(): Flow<List<Listen>>
    @Query("SELECT * FROM Listen ORDER BY at DESC LIMIT 100000") suspend fun history(): List<Listen>
    @Insert suspend fun listen(event: Listen)
    @Query("DELETE FROM Listen") suspend fun clearHistory()
    @Query("DELETE FROM Listen WHERE songId IN (:ids)") suspend fun forget(ids: List<String>)
    @Query("SELECT * FROM Playlist ORDER BY name") fun observePlaylists(): Flow<List<Playlist>>
    @Query("SELECT * FROM Playlist") suspend fun playlists(): List<Playlist>
    @Upsert suspend fun playlist(playlist: Playlist)
    @Query("DELETE FROM Playlist WHERE id = :id") suspend fun deletePlaylist(id: String)
    @Query("SELECT * FROM Setting") fun observeSettings(): Flow<List<Setting>>
    @Query("SELECT * FROM Setting") suspend fun settings(): List<Setting>
    @Query("SELECT value FROM Setting WHERE `key` = :key") suspend fun setting(key: String): String?
    @Upsert suspend fun putSetting(setting: Setting)
    @Query("SELECT * FROM QueueState WHERE id = 0") suspend fun queue(): QueueState?
    @Upsert suspend fun queue(state: QueueState)
}

@Database(entities = [Song::class, Folder::class, Listen::class, Playlist::class, Setting::class, QueueState::class], version = 2, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun dao(): LibraryDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Song ADD COLUMN mood TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}

package com.auralis.player.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** Import only app annotations for already-authorized songs. Never import a storage grant. */
class LocalBackup(private val db: LibraryDatabase) {
    suspend fun export(): String = db.withTransaction {
        val dao = db.dao()
        JSONObject().put("schema", 1)
            .put("songs", JSONArray(dao.songs().map { JSONObject().put("id", it.id).put("fingerprint", it.fingerprint).put("favourite", it.favourite).put("excluded", it.excluded).put("mood", it.mood) }))
            .put("settings", JSONArray(dao.settings().filterNot { it.key == "historyEpoch" || it.key.startsWith("forget:") || it.key.startsWith("lyrics:") }.map { JSONObject().put("key", it.key).put("value", it.value) }))
            .put("playlists", JSONArray(dao.playlists().map { JSONObject().put("name", it.name).put("ids", JSONArray(it.songIds.split('\n'))) }))
            .put("history", JSONArray(dao.history().map { JSONObject().put("song", it.songId).put("at", it.at).put("played", it.playedMs).put("duration", it.durationMs).put("completed", it.completed).put("skipped", it.skipped).put("hour", it.hour).put("day", it.day) }))
            .toString(2)
    }

    suspend fun import(text: String) {
        require(text.length <= 16 * 1024 * 1024)
        val json = JSONObject(text)
        require(json.getInt("schema") == 1)
        fun array(name: String, limit: Int): List<JSONObject> {
            val a = json.getJSONArray(name); require(a.length() <= limit)
            return (0 until a.length()).map { a.getJSONObject(it) }
        }
        val incomingSongs = array("songs", 100000)
        val incomingSettings = array("settings", 10000)
        val incomingPlaylists = array("playlists", 1000)
        val incomingHistory = array("history", 100000)
        // Parse and validate every record before opening the write transaction.
        val settings = incomingSettings.map {
            val key = it.getString("key"); val value = it.getString("value")
            require(key.length <= 4096 && value.length <= 16384)
            when {
                key == "theme" -> require(value in listOf("System", "Light", "Dark", "AMOLED"))
                key in setOf("historyPaused", "recommendations", "reduceRepetition", "restoreQueue", "pauseOnDisconnect", "reducedMotion") || key.startsWith("excludeArtist:") || key.startsWith("pin:") -> require(value in setOf("true", "false"))
                key.startsWith("mix:") -> require(value in setOf("show", "hidden", "disabled"))
                key.startsWith("profile:") -> ProfileCodec.decode(value)
                else -> error("Unrecognized setting")
            }
            Setting(key, value)
        }
        incomingSongs.forEach { require(it.getString("id").length <= 100 && it.getString("fingerprint").length <= 256 && it.getString("mood").length <= 40); it.getBoolean("favourite"); it.getBoolean("excluded") }
        incomingPlaylists.forEach { require(it.getString("name").length in 1..80 && it.getJSONArray("ids").length() <= 100000) }
        val history = incomingHistory.map {
            Listen(songId = it.getString("song"), at = it.getLong("at"), playedMs = it.getLong("played"), durationMs = it.getLong("duration"), completed = it.getBoolean("completed"), skipped = it.getBoolean("skipped"), hour = it.getInt("hour"), day = it.getInt("day"))
                .also { event -> require(event.songId.length <= 100 && event.at in 0..System.currentTimeMillis() && event.playedMs in 0..604800000 && event.durationMs in 0..604800000 && event.hour in 0..23 && event.day in 1..7) }
        }
        db.withTransaction {
            val dao = db.dao()
            val existing = dao.songs()
            val byId = existing.associateBy { it.id }
            val byHash = existing.groupBy { it.fingerprint }
            val mapping = mutableMapOf<String, String>()
            val updated = incomingSongs.mapNotNull { input ->
                val id = input.getString("id")
                val hash = input.getString("fingerprint")
                val song = byId[id] ?: if (hash.isNotEmpty()) byHash[hash]?.singleOrNull() else null
                song?.also { mapping[id] = it.id }?.copy(favourite = input.getBoolean("favourite"), excluded = input.getBoolean("excluded"), mood = input.getString("mood"))
            }
            dao.putSongs(updated)
            settings.forEach { dao.putSetting(it) }
            incomingPlaylists.forEach { input ->
                val ids = input.getJSONArray("ids")
                val mapped = (0 until ids.length()).mapNotNull { mapping[ids.getString(it)] }
                val name = input.getString("name")
                val id = java.util.UUID.nameUUIDFromBytes((name + mapped.joinToString()).toByteArray()).toString()
                dao.playlist(Playlist(id, name, mapped.joinToString("\n")))
            }
            val existingEvents = dao.history().map { Triple(it.songId, it.at, it.playedMs) }.toMutableSet()
            history.forEach { event -> mapping[event.songId]?.let { id ->
                if (existingEvents.add(Triple(id, event.at, event.playedMs))) dao.listen(event.copy(songId = id))
            } }
        }
    }
}

package com.auralis.player.domain

import com.auralis.player.data.Listen
import com.auralis.player.data.Song
import kotlin.math.ln

data class Recommendation(val song: Song, val score: Double, val explanation: String)
data class PlayStats(val count: Int, val completed: Int, val skips: Int, val last: Long)

object Recommendations {
    const val DAY = 86_400_000L
    fun stats(events: List<Listen>): Map<String, PlayStats> = events.groupBy { it.songId }.mapValues { (_, list) ->
        PlayStats(list.size, list.count { it.completed }, list.count { it.skipped }, list.maxOf { it.at })
    }

    fun rank(songs: List<Song>, events: List<Listen>, now: Long, hour: Int, reduceRepetition: Boolean = true,
        excludedArtists: Set<String> = emptySet(), limit: Int = 30): List<Recommendation> {
        val stats = stats(events)
        val artists = songs.associate { it.id to it.artist }
        val artistFinishes = events.filter { it.completed }.groupingBy { artists[it.songId] }.eachCount()
        val recentPlays = events.filter { now - it.at in 0..DAY }.groupingBy { it.songId }.eachCount()
        val timeFinishes = events.filter { it.completed && it.hour / 6 == hour / 6 }.groupingBy { artists[it.songId] }.eachCount()
        val candidates = songs.filter { !it.excluded && it.artist !in excludedArtists }.map { song ->
            val s = stats[song.id]
            val days = s?.let { ((now - it.last).coerceAtLeast(0)).toDouble() / DAY } ?: 365.0
            val completion = if (s == null) 0.0 else s.completed.toDouble() / s.count
            val skips = if (s == null) 0.0 else s.skips.toDouble() / s.count
            val affinity = ln(1.0 + (artistFinishes[song.artist] ?: 0))
            val time = (timeFinishes[song.artist] ?: 0).coerceAtMost(5) * .4
            val fatigue = (recentPlays[song.id] ?: 0) * if (reduceRepetition) 4.0 else 1.5
            val rediscovery = minOf(days / 30.0, 3.0)
            val score = (if (song.favourite) 5.0 else 0.0) + completion * 4 + affinity + time + rediscovery - skips * 7 - fatigue + if (s == null) 2.0 else 0.0
            val reason = when {
                s == null -> "Explore a song you have not played yet."
                song.favourite && days > 30 -> "A favourite you have not heard in a while."
                time > 0 -> "You often finish this artist in this part of the day."
                completion >= .75 -> "You usually finish this song."
                affinity > 0 -> "Because you finish music by this artist."
                else -> "Rediscover something outside your recent rotation."
            }
            Recommendation(song, score, reason)
        }.sortedWith(compareByDescending<Recommendation> { it.score }.thenBy { it.song.id }).toMutableList()
        val selected = mutableListOf<Recommendation>()
        val artistCounts = mutableMapOf<String, Int>()
        while (selected.size < limit && candidates.isNotEmpty()) {
            // Diversity penalty is applied to each selection, without randomly changing results.
            val next = candidates.maxWithOrNull(compareBy<Recommendation> { it.score - (artistCounts[it.song.artist] ?: 0) * 4.0 }.thenByDescending { it.song.id })!!
            candidates.remove(next); selected.add(next)
            artistCounts[next.song.artist] = (artistCounts[next.song.artist] ?: 0) + 1
        }
        return selected
    }
}

object SmartMixes {
    val names = listOf("Most Played", "Recently Added", "Recently Played", "No-Skip Favourites", "Forgotten Favourites",
        "Morning Mix", "Evening Mix", "Weekend Mix", "Rediscover Old Songs", "Current Rotation", "Favourite Artists")
    fun songs(name: String, library: List<Song>, events: List<Listen>, now: Long): List<Song> {
        val allowed = library.filterNot { it.excluded }
        val stats = Recommendations.stats(events)
        val favouriteArtists = allowed.filter { it.favourite }.map { it.artist }.toSet()
        val day = Recommendations.DAY
        val timeIds = events.filter { it.completed && when (name) { "Morning Mix" -> it.hour in 5..11; "Evening Mix" -> it.hour in 17..23; "Weekend Mix" -> it.day in setOf(1, 7); else -> false } }.map { it.songId }.toSet()
        val members = when (name) {
            "Most Played" -> allowed.filter { it.id in stats }.sortedWith(compareByDescending<Song> { stats[it.id]?.count }.thenBy { it.id })
            "Recently Added" -> allowed.sortedWith(compareByDescending<Song> { it.added }.thenBy { it.id })
            "Recently Played" -> allowed.filter { it.id in stats }.sortedWith(compareByDescending<Song> { stats[it.id]?.last }.thenBy { it.id })
            "No-Skip Favourites" -> allowed.filter { it.favourite && (stats[it.id]?.completed ?: 0) >= 2 && stats[it.id]?.skips == 0 }
            "Forgotten Favourites" -> allowed.filter { it.favourite && now - (stats[it.id]?.last ?: it.added) > 30 * day }
            "Morning Mix", "Evening Mix", "Weekend Mix" -> allowed.filter { it.id in timeIds }
            "Rediscover Old Songs" -> allowed.filter { now - it.added > 30 * day && now - (stats[it.id]?.last ?: 0) > 30 * day }
            "Current Rotation" -> allowed.filter { now - (stats[it.id]?.last ?: 0) in 0..(7 * day) }
            "Favourite Artists" -> allowed.filter { it.artist in favouriteArtists }
            else -> if (name.startsWith("Mood: ")) allowed.filter { it.mood.equals(name.removePrefix("Mood: "), true) } else emptyList()
        }
        return (if (name in setOf("Most Played", "Recently Added", "Recently Played")) members else members.sortedBy { it.id }).take(100)
    }
}

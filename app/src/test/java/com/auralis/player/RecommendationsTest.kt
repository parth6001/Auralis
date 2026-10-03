package com.auralis.player

import com.auralis.player.data.*
import com.auralis.player.domain.*
import org.junit.Assert.*
import org.junit.Test

class RecommendationsTest {
    private val now = 200 * Recommendations.DAY
    private fun song(id: String, artist: String = id, favourite: Boolean = false) = Song(id, "content://test/$id", "root", "folder", id, artist = artist, added = 1, favourite = favourite)
    private fun event(id: String, completed: Boolean = true, skip: Boolean = false, age: Long = 2 * Recommendations.DAY, hour: Int = 20, day: Int = 7) =
        Listen(songId = id, at = now - age, playedMs = if (completed) 100000 else 1000, durationMs = 100000, completed = completed, skipped = skip, hour = hour, day = day)
    @Test fun rankingIsDeterministicAndExplained() {
        val songs = listOf(song("a"), song("b"), song("c"))
        val events = listOf(event("a"))
        val first = Recommendations.rank(songs, events, now, 20)
        assertEquals(first, Recommendations.rank(songs.reversed(), events.reversed(), now, 20))
        assertTrue(first.all { it.explanation.isNotBlank() })
    }
    @Test fun skipPenaltyAndDailyFatigueReduceScore() {
        val songs = listOf(song("a"))
        fun score(events: List<Listen>) = Recommendations.rank(songs, events, now, 20).single().score
        assertTrue(score(listOf(event("a"))) > score(listOf(event("a", completed = false, skip = true))))
        assertTrue(score(listOf(event("a"))) > score(List(4) { event("a", age = 1000) }))
    }
    @Test fun exclusionsAndArtistDiversityAreHonoured() {
        val songs = (1..6).map { song("a$it", "same") } + song("other", "different") + song("blocked").copy(excluded = true)
        val ranked = Recommendations.rank(songs, emptyList(), now, 20)
        assertFalse(ranked.any { it.song.id == "blocked" })
        assertTrue(ranked.take(3).map { it.song.artist }.distinct().size > 1)
        assertTrue(Recommendations.rank(songs, emptyList(), now, 20, excludedArtists = setOf("same")).none { it.song.artist == "same" })
    }
    @Test fun historyRemovalRemovesBehaviouralAffinity() {
        val songs = listOf(song("a"))
        val ranked = Recommendations.rank(songs, emptyList(), now, 20)
        assertEquals("Explore a song you have not played yet.", ranked.single().explanation)
        assertTrue(SmartMixes.songs("Most Played", songs, emptyList(), now).isEmpty())
        assertTrue(SmartMixes.songs("Morning Mix", songs, emptyList(), now).isEmpty())
    }
    @Test fun deterministicSmartMembership() {
        val songs = listOf(song("a", favourite = true), song("b", favourite = true), song("c").copy(mood = "Instrumental"))
        val events = listOf(event("a", hour = 8), event("a", hour = 8), event("b", completed = false, skip = true))
        assertEquals(listOf("a"), SmartMixes.songs("No-Skip Favourites", songs, events, now).map { it.id })
        assertEquals(listOf("a"), SmartMixes.songs("Morning Mix", songs, events, now).map { it.id })
        assertEquals(listOf("c"), SmartMixes.songs("Mood: Instrumental", songs, events, now).map { it.id })
        assertEquals("a", SmartMixes.songs("Most Played", songs, events, now).first().id)
    }
    @Test fun oldFavouritesQualifyAndRecentOnesDoNot() {
        val songs = listOf(song("old", favourite = true), song("recent", favourite = true))
        val events = listOf(event("old", age = 50 * Recommendations.DAY), event("recent", age = Recommendations.DAY))
        assertEquals(listOf("old"), SmartMixes.songs("Forgotten Favourites", songs, events, now).map { it.id })
    }
    @Test fun libraryScaleSmoke() {
        for (size in listOf(100, 1000, 10000)) {
            val songs = (1..size).map { song("$it", "artist${it % 100}") }
            val start = System.nanoTime()
            val ranked = Recommendations.rank(songs, emptyList(), now, 8)
            val elapsed = (System.nanoTime() - start) / 1_000_000
            println("rank library=$size elapsedMs=$elapsed")
            assertEquals(30, ranked.size)
            assertEquals(30, ranked.map { it.song.id }.distinct().size)
        }
    }
}

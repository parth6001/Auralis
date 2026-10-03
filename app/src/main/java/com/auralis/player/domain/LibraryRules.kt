package com.auralis.player.domain

import java.util.Locale
import kotlin.random.Random

object LibraryRules {
    val extensions = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "alac")
    fun text(value: String?, fallback: String): String = value?.replace('\u0000', ' ')
        ?.trim()?.take(2048)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: fallback

    fun probableDuplicate(title: String, artist: String, duration: Long, size: Long): String =
        "${title.lowercase(Locale.ROOT)}|${artist.lowercase(Locale.ROOT)}|${duration / 1000}|$size"

    fun shuffled(ids: List<String>, seed: Int): List<String> = ids.shuffled(Random(seed))
    fun next(index: Int, count: Int, repeat: Int): Int? = when {
        count == 0 -> null
        repeat == 1 -> index.coerceIn(0, count - 1)
        index + 1 < count -> index + 1
        repeat == 2 -> 0
        else -> null
    }
}

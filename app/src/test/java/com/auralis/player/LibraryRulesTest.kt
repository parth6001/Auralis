package com.auralis.player

import com.auralis.player.domain.LibraryRules
import org.junit.Assert.*
import org.junit.Test

class LibraryRulesTest {
    @Test fun missingAndMalformedMetadata() {
        assertEquals("Fallback", LibraryRules.text("<unknown>", "Fallback"))
        assertEquals("é 音", LibraryRules.text("  é 音\u0000 ", "Fallback"))
        assertEquals(2048, LibraryRules.text("a".repeat(5000), "").length)
    }
    @Test fun duplicateKeyNormalizesCaseButKeepsSize() {
        assertEquals(LibraryRules.probableDuplicate("Tone", "Artist", 1050, 80), LibraryRules.probableDuplicate("TONE", "ARTIST", 1099, 80))
        assertNotEquals(LibraryRules.probableDuplicate("Tone", "Artist", 1050, 80), LibraryRules.probableDuplicate("Tone", "Artist", 1050, 81))
    }
    @Test fun repeatBoundaries() {
        assertNull(LibraryRules.next(0, 0, 2))
        assertNull(LibraryRules.next(2, 3, 0))
        assertEquals(0, LibraryRules.next(2, 3, 2))
        assertEquals(2, LibraryRules.next(2, 3, 1))
    }
    @Test fun shuffleIsReproduciblePermutation() {
        val ids = (1..100).map { "$it" }
        assertEquals(LibraryRules.shuffled(ids, 7), LibraryRules.shuffled(ids, 7))
        assertEquals(ids.toSet(), LibraryRules.shuffled(ids, 7).toSet())
        assertNotEquals(ids, LibraryRules.shuffled(ids, 7))
    }
}

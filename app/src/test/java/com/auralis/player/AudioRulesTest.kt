package com.auralis.player

import com.auralis.player.audio.*
import org.junit.Assert.*
import org.junit.Test

class AudioRulesTest {
    @Test fun replayGainParsingFallbackAndClippingSafety() {
        assertEquals(-7.2f, ReplayGain.parse("-7.20 dB"))
        assertNull(ReplayGain.parse("NaN"))
        assertNull(ReplayGain.parse("999 dB"))
        assertEquals(-6f, ReplayGain.select(ReplayGainMode.ALBUM, -6f, null))
        assertEquals(0f, ReplayGain.select(ReplayGainMode.OFF, -6f, -8f))
        assertTrue(AudioProfile(mode = OutputMode.ENHANCED_IEM).gain(12f) <= 1f)
        assertEquals(ReplayGainMode.OFF, AudioProfile(replayGain = ReplayGainMode.ALBUM).effective().replayGain)
    }
    @Test fun directDisablesEveryProcessingControl() {
        val effective = AudioProfile(bands = listOf(8f, 4f, 1f, -1f, 2f), preamp = -4f, balance = 1f, crossfeed = .2f).effective()
        assertEquals(List(5) { 0f }, effective.bands)
        assertEquals(1f, effective.gain())
        assertEquals(0f, effective.balance)
        assertEquals(0f, effective.crossfeed)
        assertFalse(effective.limiter)
    }
    @Test fun overlappingPositiveBandsReceiveConservativeHeadroom() {
        val profile = AudioProfile(mode = OutputMode.ENHANCED_IEM, bands = listOf(6f, 3f, -4f, 0f, 0f))
        assertEquals(-9f, profile.safePreamp())
        assertTrue(profile.gain() < .36f)
    }
    @Test fun invalidPresetsRejected() {
        assertFalse(AudioProfile(balance = Float.NaN).valid())
        assertFalse(AudioProfile(bands = listOf(0f)).valid())
        assertFalse(AudioProfile(preamp = 10f).valid())
        assertTrue(AudioProfile().valid())
    }
    @Test fun disconnectPolicyAndRouteIdentity() {
        assertTrue(RouteRules.shouldPause(true, true))
        assertFalse(RouteRules.shouldPause(false, true))
        assertFalse(RouteRules.shouldPause(true, false))
        assertNotEquals(RouteRules.profileKey("USB", "DAC", "a"), RouteRules.profileKey("USB", "DAC", "b"))
        assertEquals(RouteRules.profileKey("USB", "DAC", "a"), RouteRules.profileKey("USB", "DAC", "a"))
    }
}

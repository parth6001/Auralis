package com.auralis.player.audio

import kotlin.math.pow

enum class OutputMode { PURE_DIRECT, ENHANCED_IEM }
enum class ReplayGainMode { OFF, TRACK, ALBUM }

data class AudioProfile(val name: String = "My output", val mode: OutputMode = OutputMode.PURE_DIRECT,
    val bands: List<Float> = List(5) { 0f }, val preamp: Float = 0f, val balance: Float = 0f,
    val crossfeed: Float = 0f, val limiter: Boolean = true, val replayGain: ReplayGainMode = ReplayGainMode.OFF) {
    fun valid() = name.length in 1..80 && bands.size == 5 && bands.all { it.isFinite() && it in -12f..12f } &&
        preamp.isFinite() && preamp in -24f..0f && balance.isFinite() && balance in -1f..1f && crossfeed.isFinite() && crossfeed in 0f..0.3f
    // Sum of positive bands is conservative headroom for overlapping filters.
    fun safePreamp(): Float = minOf(preamp, -bands.sumOf { it.coerceAtLeast(0f).toDouble() }.toFloat())
    fun gain(replayGainDb: Float = 0f): Float = 10.0.pow(minOf(preamp + replayGainDb, safePreamp()) / 20.0).toFloat()
    fun effective(): AudioProfile = if (mode == OutputMode.PURE_DIRECT) copy(bands = List(5) { 0f }, preamp = 0f, balance = 0f, crossfeed = 0f, limiter = false, replayGain = ReplayGainMode.OFF) else this
}

object ReplayGain {
    fun parse(value: String): Float? = value.trim().removeSuffix("dB").removeSuffix("db").trim().toFloatOrNull()?.takeIf { it.isFinite() && it in -60f..24f }
    fun select(mode: ReplayGainMode, track: Float?, album: Float?): Float = when (mode) { ReplayGainMode.OFF -> 0f; ReplayGainMode.TRACK -> track ?: 0f; ReplayGainMode.ALBUM -> album ?: track ?: 0f }
}

object RouteRules {
    fun profileKey(type: String, product: String, address: String): String = "$type|${product.take(128)}|${address.take(128)}"
    fun shouldPause(removedPersonalOutput: Boolean, pauseOnDisconnect: Boolean) = removedPersonalOutput && pauseOnDisconnect
}

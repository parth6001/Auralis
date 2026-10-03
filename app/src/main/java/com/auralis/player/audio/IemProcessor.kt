package com.auralis.player.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.*

/** Streaming PCM16 DSP. No resampling or sample-rate claims; unsupported encodings bypass DSP. */
@UnstableApi
class IemProcessor : BaseAudioProcessor() {
    @Volatile var profile = AudioProfile()
    @Volatile var supported = false
    @Volatile var trackGain: Float? = null
    @Volatile var albumGain: Float? = null
    private var applied: AudioProfile? = null
    private var filters = emptyArray<Array<Biquad>>()
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        supported = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount in 1..2
        applied = null
        return if (supported) inputAudioFormat else AudioProcessor.AudioFormat.NOT_SET
    }
    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) { applied = null }
    override fun queueInput(inputBuffer: ByteBuffer) {
        // Media3 sends its shared empty buffer while draining the pipeline. Do not copy it into itself.
        if (!inputBuffer.hasRemaining()) return
        val output = replaceOutputBuffer(inputBuffer.remaining())
        val active = profile.effective()
        if (active.mode == OutputMode.PURE_DIRECT) { output.put(inputBuffer); output.flip(); return }
        if (applied != active) {
            filters = Array(inputAudioFormat.channelCount) {
                Array(5) { band -> Biquad(floatArrayOf(60f, 230f, 910f, 3600f, 14000f)[band].coerceAtMost(inputAudioFormat.sampleRate * 0.45f), active.bands[band], inputAudioFormat.sampleRate) }
            }
            applied = active
        }
        val gain = active.gain(ReplayGain.select(active.replayGain, trackGain, albumGain))
        while (inputBuffer.remaining() >= 2 * inputAudioFormat.channelCount) {
            var left = inputBuffer.short / 32768f
            var right = if (inputAudioFormat.channelCount == 2) inputBuffer.short / 32768f else left
            for (filter in filters[0]) left = filter.process(left)
            if (inputAudioFormat.channelCount == 2) for (filter in filters[1]) right = filter.process(right)
            val mixedLeft = (left + right * active.crossfeed) / (1f + active.crossfeed)
            val mixedRight = (right + left * active.crossfeed) / (1f + active.crossfeed)
            left = mixedLeft * gain * (1f - active.balance.coerceAtLeast(0f))
            right = mixedRight * gain * (1f + active.balance.coerceAtMost(0f))
            fun pcm(value: Float): Short {
                val bounded = if (active.limiter) value.coerceIn(-0.98f, 0.98f) else value.coerceIn(-1f, 0.999969f)
                return (bounded * 32768f).roundToInt().toShort()
            }
            output.putShort(pcm(left))
            if (inputAudioFormat.channelCount == 2) output.putShort(pcm(right))
        }
        output.flip()
    }
}

/** RBJ peaking filter; coefficients recalculated only on a profile/format change. */
internal class Biquad(frequency: Float, db: Float, sampleRate: Int) {
    private val a = 10.0.pow(db / 40.0)
    private val omega = 2 * PI * frequency / sampleRate
    private val alpha = sin(omega) / 2
    private val a0 = 1 + alpha / a
    private val b0 = (1 + alpha * a) / a0
    private val b1 = -2 * cos(omega) / a0
    private val b2 = (1 - alpha * a) / a0
    private val a1 = b1
    private val a2 = (1 - alpha / a) / a0
    private var z1 = 0.0
    private var z2 = 0.0
    fun process(x: Float): Float {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y.toFloat()
    }
}

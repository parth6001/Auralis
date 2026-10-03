package com.auralis.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.auralis.player.audio.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AudioProcessorTest {
    @Test fun directCopiesPcmAndEmptyDrainDoesNotCrash() {
        val processor = IemProcessor()
        processor.queueInput(AudioProcessor.EMPTY_BUFFER)
        processor.configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        val input = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).putShort(32767).putShort(-32768).putShort(1234).putShort(-1234)
        input.flip(); processor.queueInput(input)
        val output = processor.output
        assertEquals(32767, output.short.toInt()); assertEquals(-32768, output.short.toInt())
        assertEquals(1234, output.short.toInt()); assertEquals(-1234, output.short.toInt())
        processor.queueInput(AudioProcessor.EMPTY_BUFFER)
    }
    @Test fun enhancedBalanceAndLimiterBoundOutput() {
        val processor = IemProcessor()
        processor.profile = AudioProfile(mode = OutputMode.ENHANCED_IEM, balance = 1f)
        processor.configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).putShort(32767).putShort(32767)
        input.flip(); processor.queueInput(input)
        val output = processor.output
        assertEquals(0, output.short.toInt())
        assertTrue(output.short.toInt() in 32000..32113)
    }
}

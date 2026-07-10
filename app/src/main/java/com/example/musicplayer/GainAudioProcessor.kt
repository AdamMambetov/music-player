package com.example.musicplayer

import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.pow

class GainAudioProcessor : AudioProcessor {

    private var inputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER

    @Volatile
    private var gainFactor = 1f
    private var active = false

    fun setGainDb(gainDb: Float) {
        gainFactor = 10f.pow(gainDb / 20f)
        active = abs(gainFactor - 1f) > 0.001f
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        this.inputAudioFormat = inputAudioFormat
        return inputAudioFormat
    }

    override fun isActive(): Boolean = active

    override fun queueInput(inputBuffer: ByteBuffer) {
        val input = inputBuffer.duplicate().order(ByteOrder.nativeOrder()).asShortBuffer()
        val samples = ShortArray(input.remaining())
        input.get(samples)

        val out = ShortArray(samples.size)
        for (i in samples.indices) {
            val v = (samples[i] * gainFactor).toInt()
            out[i] = v.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val bytes = ByteBuffer.allocateDirect(out.size * 2).order(ByteOrder.nativeOrder())
        bytes.asShortBuffer().put(out)
        bytes.position(0)
        bytes.limit(out.size * 2)
        outputBuffer = bytes

        inputBuffer.position(inputBuffer.limit())
    }

    override fun getOutput(): ByteBuffer = outputBuffer

    override fun queueEndOfStream() {}

    override fun isEnded(): Boolean = false

    override fun flush() {
        outputBuffer = EMPTY_BUFFER
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        gainFactor = 1f
        active = false
    }

    companion object {
        private val EMPTY_BUFFER: ByteBuffer =
            ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }
}

package com.example.musicplayer

import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.tanh

class GainAudioProcessor : AudioProcessor {

    private var inputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER

    @Volatile
    private var gainFactor = 1f
    private var active = false

    companion object {
        private val EMPTY_BUFFER: ByteBuffer =
            ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
        private const val GAIN_MIN_DB = -12f
        private const val GAIN_MAX_DB = 6f
        private const val PEAK_HARD_LIMIT_DB = -1f
        private const val PEAK_SOFT_LIMIT_DB = -3f
    }

    fun setGainDb(rawGainDb: Float, peakLevelDb: Float = -100f) {
        var gainDb = rawGainDb.coerceIn(GAIN_MIN_DB, GAIN_MAX_DB)

        if (peakLevelDb > PEAK_SOFT_LIMIT_DB) {
            val headroom = PEAK_HARD_LIMIT_DB - peakLevelDb
            if (headroom < 0f) {
                gainDb = 0f
            } else {
                gainDb = gainDb.coerceIn(GAIN_MIN_DB, headroom)
            }
        }

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
            val scaled = samples[i] * gainFactor / Short.MAX_VALUE
            val limited = tanh(scaled) * Short.MAX_VALUE
            out[i] = limited.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
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
}

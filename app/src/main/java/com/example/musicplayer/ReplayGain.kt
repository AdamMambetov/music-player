package com.example.musicplayer

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ReplayGain {
    suspend fun analyzeTrack(context: Context, uri: Uri): AnalysisResult =
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)

            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }
            require(audioTrackIndex >= 0 && format != null)

            extractor.selectTrack(audioTrackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val maxSamples = 2_000_000
            var sampleCount = 0L
            var sumSquares = 0.0
            var peakSample = 0.0

            val preFilter = BiquadFilter(highpass(15000.0, sampleRate, 0.7071))
            val shelfFilter = BiquadFilter(lowShelf(200.0, sampleRate, -3.0, 0.7071))

            val inputBuffers = codec.inputBuffers
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = inputBuffers[inputIndex]
                        inputBuffer.clear()
                        val size = extractor.readSampleData(inputBuffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(
                                inputIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            val timeUs = extractor.sampleTime
                            codec.queueInputBuffer(inputIndex, 0, size, timeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outputIndex >= 0 -> {
                        val outBuffer = codec.getOutputBuffer(outputIndex)!!
                        if (bufferInfo.size > 0) {
                            val bytes = ByteArray(bufferInfo.size)
                            outBuffer.position(bufferInfo.offset)
                            outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            outBuffer.get(bytes)

                            var i = 0
                            while (i + 1 < bytes.size && sampleCount < maxSamples) {
                                val lo = bytes[i].toInt() and 0xff
                                val hi = bytes[i + 1].toInt()
                                val sample = (hi shl 8 or lo).toShort().toInt() / 32768.0
                                val filtered = shelfFilter.process(preFilter.process(sample))
                                sumSquares += filtered * filtered
                                sampleCount++
                                i += 2
                            }
                        }

                        codec.releaseOutputBuffer(outputIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                        if (sampleCount >= maxSamples) {
                            outputDone = true
                        }
                    }

                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            val rms = sqrt(sumSquares / maxOf(sampleCount, 1L))
            val loudnessDb = (20.0 * kotlin.math.log10(maxOf(rms, 1e-9))).toFloat()

            // -14 dBFS ≈ 89 dB SPL (ReplayGain calibration)
            AnalysisResult(
                trackGainDb = -loudnessDb - 14f,
                referenceLoudnessDb = 89f,
                analyzedAt = System.currentTimeMillis(),
            )
        }

    private fun highpass(freq: Double, sampleRate: Int, q: Double): DoubleArray {
        val w0 = 2.0 * PI * freq / sampleRate
        val cosW = cos(w0)
        val sinW = sin(w0)
        val alpha = sinW / (2.0 * q)
        val b0 = (1.0 + cosW) / 2.0
        val a0 = 1.0 + alpha
        return doubleArrayOf(
            b0 / a0, -(1.0 + cosW) / a0, b0 / a0,
            -2.0 * cosW / a0, (1.0 - alpha) / a0
        )
    }

    private fun lowShelf(freq: Double, sampleRate: Int, gainDb: Double, q: Double): DoubleArray {
        val A = Math.pow(10.0, gainDb / 40.0)
        val w0 = 2.0 * PI * freq / sampleRate
        val cosW = cos(w0)
        val sinW = sin(w0)
        val alpha = sinW / (2.0 * q)
        val sqrtA2alpha = 2.0 * sqrt(A) * alpha

        val b0 = A * ((A + 1.0) - (A - 1.0) * cosW + sqrtA2alpha)
        val b1 = 2.0 * A * ((A - 1.0) - (A + 1.0) * cosW)
        val b2 = A * ((A + 1.0) - (A - 1.0) * cosW - sqrtA2alpha)
        val a0 = (A + 1.0) + (A - 1.0) * cosW + sqrtA2alpha
        val a1 = -2.0 * ((A - 1.0) + (A + 1.0) * cosW)
        val a2 = (A + 1.0) + (A - 1.0) * cosW - sqrtA2alpha

        return doubleArrayOf(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    private class BiquadFilter(coeffs: DoubleArray) {
        private val b0 = coeffs[0]; private val b1 = coeffs[1]; private val b2 = coeffs[2]
        private val a1 = coeffs[3]; private val a2 = coeffs[4]
        private var x1 = 0.0; private var x2 = 0.0
        private var y1 = 0.0; private var y2 = 0.0

        fun process(input: Double): Double {
            val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = input
            y2 = y1; y1 = output
            return output
        }
    }
}

data class AnalysisResult(
    val trackGainDb: Float?,
    val referenceLoudnessDb: Float = 89.0f,
    val analyzedAt: Long? = null
)

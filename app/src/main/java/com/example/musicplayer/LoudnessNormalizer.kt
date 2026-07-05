package com.example.musicplayer

import android.media.audiofx.DynamicsProcessing
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class LoudnessNormalizer(private val sessionId: Int) {
    private var dp: DynamicsProcessing? = null

    companion object {
        private const val TAG = "LoudnessNormalizer"
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun enable(): Boolean {
        return try {
            disable()

            val channelCount = 2
            val mbcBandCount = 1

            val config = DynamicsProcessing.Config.Builder(
                0,
                channelCount,
                true, mbcBandCount,   // MBC: enabled, 1 band
                false, 0,             // PreEq: disabled
                false, 0,             // PostEq: disabled
                true                  // Limiter: enabled
            ).build()

            dp = DynamicsProcessing(0, sessionId, config)

            // Компрессор: низкий порог, высокое ratio — все треки на одном уровне
            val mbcBand = DynamicsProcessing.MbcBand(
                true,     // enabled
                20f,      // cutoff (Hz) — покрываем весь спектр
                -20f,     // threshold (dB) — низкий порог, ловит тихие сигналы
                6f,       // knee width — плавный переход
                12f,      // ratio — сильная компрессия (12:1)
                5f,       // attack (ms) — быстрая атака
                150f,     // release (ms) — медленное восстановление
                0f,       // expander ratio — без экспандера
                -100f,    // noise gate — не подавляем тихие звуки
                6f,       // pre gain — поднимаем тихие треки (+6 dB)
                -3f       // post gain — компенсируем общий подъём
            )

            for (ch in 0 until channelCount) {
                dp?.setMbcBandByChannelIndex(ch, 0, mbcBand)
            }

            // Лимитер: не даём перегрузить (> 0 dB)
            val limiter = DynamicsProcessing.Limiter(
                true,     // enabled
                true,     // linkGroups enabled
                0,        // linkGroup
                0f,       // threshold — 0 dB
                20f,      // ratio — жёсткий лимитер
                1f,       // attack (ms)
                50f,      // release (ms)
                0f        // post gain
            )
            for (ch in 0 until channelCount) {
                dp?.setLimiterByChannelIndex(ch, limiter)
            }

            dp?.enabled = true
            Log.d(TAG, "Loudness normalizer enabled on session $sessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable: ${e.message}")
            false
        }
    }

    fun disable() {
        try {
            dp?.apply {
                enabled = false
                release()
            }
            dp = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable: ${e.message}")
        }
    }

    fun release() = disable()
}

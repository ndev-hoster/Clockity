package com.clockity.app.utils

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationUtils {

    fun getPatternTimings(patternName: String): LongArray {
        return when (patternName.lowercase()) {
            "heartbeat" -> longArrayOf(0, 150, 150, 150, 600)
            "tick-tock" -> longArrayOf(0, 100, 300, 100, 500)
            "rapid" -> longArrayOf(0, 80, 80, 80, 80, 80, 400)
            "smooth" -> longArrayOf(0, 500, 300, 500, 300)
            "none" -> longArrayOf(0)
            else -> longArrayOf(0, 400, 300, 400, 800) // Basic One UI alarm pattern
        }
    }

    fun startVibration(context: Context, patternName: String) {
        if (patternName.equals("none", ignoreCase = true)) return

        val timings = getPatternTimings(patternName)
        val amplitudes = IntArray(timings.size) { index ->
            if (index % 2 == 0) 0 else 255
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val vibrator = vibratorManager?.defaultVibrator
            val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, 0)
            }
        }
    }

    fun stopVibration(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.cancel()
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.cancel()
        }
    }
}

package com.clockity.app.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SoundUtils {
    private var mediaPlayer: MediaPlayer? = null
    private var volumeRampJob: Job? = null

    fun playAlarm(
        context: Context,
        isGentleWakeUp: Boolean,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ) {
        stopAlarm()

        val alertUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(context, alertUri)
            isLooping = true
            prepare()
        }

        if (isGentleWakeUp) {
            // Start quiet and ramp up over 60 seconds
            mediaPlayer?.setVolume(0.1f, 0.1f)
            mediaPlayer?.start()

            volumeRampJob = coroutineScope.launch {
                for (step in 1..20) {
                    delay(3000) // 20 steps * 3 seconds = 60s
                    val volume = 0.1f + (0.9f * (step / 20f))
                    mediaPlayer?.setVolume(volume, volume)
                }
            }
        } else {
            mediaPlayer?.setVolume(1.0f, 1.0f)
            mediaPlayer?.start()
        }
    }

    fun stopAlarm() {
        volumeRampJob?.cancel()
        volumeRampJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}

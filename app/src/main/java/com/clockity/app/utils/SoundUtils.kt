package com.clockity.app.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SoundUtils {
    private const val TAG = "SoundUtils"
    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var toneGenerator: ToneGenerator? = null
    private var volumeRampJob: Job? = null
    private var fallbackToneJob: Job? = null

    fun playAlarm(
        context: Context,
        isGentleWakeUp: Boolean,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ) {
        stopAlarm()

        // 1. Ensure Alarm Stream Volume is audible
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                if (currentVol <= 0 && maxVol > 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, (maxVol * 0.75f).toInt().coerceAtLeast(1), 0)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to adjust alarm volume", e)
        }

        // 2. Candidate Alert URIs to try (actual file URIs first, then default URIs)
        val candidateUris = mutableListOf<Uri>()

        try {
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)?.let { candidateUris.add(it) }
        } catch (_: Exception) {}
        try {
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)?.let { candidateUris.add(it) }
        } catch (_: Exception) {}
        try {
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)?.let { candidateUris.add(it) }
        } catch (_: Exception) {}

        candidateUris.add(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        candidateUris.add(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
        candidateUris.add(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        candidateUris.add(Settings.System.DEFAULT_ALARM_ALERT_URI)
        candidateUris.add(Settings.System.DEFAULT_RINGTONE_URI)
        candidateUris.add(Settings.System.DEFAULT_NOTIFICATION_URI)

        val uniqueUris = candidateUris.distinct()

        var playbackSuccess = false

        // 3. Tier 1: Try MediaPlayer
        for (uri in uniqueUris) {
            try {
                val mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setDataSource(context, uri)
                mp.isLooping = true
                mp.prepare()

                if (isGentleWakeUp) {
                    mp.setVolume(0.4f, 0.4f)
                    mp.start()
                    volumeRampJob = coroutineScope.launch {
                        for (step in 1..20) {
                            delay(3000)
                            val vol = 0.4f + (0.6f * (step / 20f))
                            try {
                                mp.setVolume(vol, vol)
                            } catch (_: Exception) {}
                        }
                    }
                } else {
                    mp.setVolume(1.0f, 1.0f)
                    mp.start()
                }

                mediaPlayer = mp
                playbackSuccess = true
                Log.d(TAG, "MediaPlayer successfully started with URI: $uri")
                break
            } catch (e: Exception) {
                Log.w(TAG, "MediaPlayer failed for URI: $uri, error: ${e.message}")
            }
        }

        // 4. Tier 2: Fallback to android.media.Ringtone if MediaPlayer failed
        if (!playbackSuccess) {
            for (uri in uniqueUris) {
                try {
                    val rt = RingtoneManager.getRingtone(context, uri)
                    if (rt != null) {
                        rt.audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            rt.isLooping = true
                            rt.volume = 1.0f
                        }
                        rt.play()
                        ringtone = rt
                        playbackSuccess = true
                        Log.d(TAG, "Ringtone successfully started with URI: $uri")
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Ringtone failed for URI: $uri, error: ${e.message}")
                }
            }
        }

        // 5. Tier 3: ToneGenerator repeating synth alarm tone fallback
        if (!playbackSuccess) {
            try {
                Log.w(TAG, "Falling back to ToneGenerator alarm synth tone")
                val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                toneGenerator = tg
                fallbackToneJob = coroutineScope.launch {
                    while (true) {
                        tg.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1200)
                        delay(1600)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ToneGenerator failed as well", e)
            }
        }
    }

    fun stopAlarm() {
        volumeRampJob?.cancel()
        volumeRampJob = null
        fallbackToneJob?.cancel()
        fallbackToneJob = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        try {
            ringtone?.stop()
        } catch (_: Exception) {}
        ringtone = null

        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (_: Exception) {}
        toneGenerator = null
    }
}

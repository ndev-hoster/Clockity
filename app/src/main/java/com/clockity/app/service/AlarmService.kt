package com.clockity.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.clockity.app.utils.SoundUtils
import com.clockity.app.utils.VibrationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class AlarmService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        val label = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Alarm"
        val timeStr = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_TIME) ?: "00:00"
        val isGentleWake = intent?.getBooleanExtra(AlarmReceiver.EXTRA_GENTLE_WAKE, true) ?: true
        val vibrationPattern = intent?.getStringExtra(AlarmReceiver.EXTRA_VIBRATION_PATTERN) ?: "Basic"

        // 1. Acquire Partial WakeLock (max 10 minutes)
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Clockity:AlarmWakeLock")?.apply {
            acquire(10 * 60 * 1000L)
        }

        // 2. Build and display foreground notification
        val notification = NotificationHelper.buildAlarmRingingNotification(this, alarmId, label, timeStr)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID_ALARM,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID_ALARM, notification)
        }

        // 3. Start audio & vibration
        SoundUtils.playAlarm(this, isGentleWake)
        VibrationUtils.startVibration(this, vibrationPattern)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundUtils.stopAlarm()
        VibrationUtils.stopVibration(this)
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        serviceScope.cancel()
    }
}

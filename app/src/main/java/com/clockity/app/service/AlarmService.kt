package com.clockity.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.clockity.app.data.local.ClockityDatabase
import com.clockity.app.utils.AlarmScheduler
import com.clockity.app.utils.AppLogger
import com.clockity.app.utils.PreferencesManager
import com.clockity.app.utils.SoundUtils
import com.clockity.app.utils.VibrationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var autoTimeoutJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        val label = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Alarm"
        val timeStr = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_TIME) ?: "00:00"
        val isGentleWake = intent?.getBooleanExtra(AlarmReceiver.EXTRA_GENTLE_WAKE, true) ?: true
        val vibrationPattern = intent?.getStringExtra(AlarmReceiver.EXTRA_VIBRATION_PATTERN) ?: "Basic"

        val silenceMinutes = PreferencesManager.getAlarmSilenceMins(this)
        AppLogger.i("AlarmService", "Starting alarm #$alarmId ($label at $timeStr). Silence timeout: $silenceMinutes min")

        // 1. Acquire Partial WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Clockity:AlarmWakeLock")?.apply {
            val lockTime = if (silenceMinutes > 0) (silenceMinutes + 1) * 60 * 1000L else 10 * 60 * 1000L
            acquire(lockTime)
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

        // 4. Auto-silence timeout (triggers Missed Alarm when unanswered)
        autoTimeoutJob?.cancel()
        if (silenceMinutes > 0) {
            autoTimeoutJob = serviceScope.launch {
                val timeoutMillis = silenceMinutes * 60 * 1000L
                AppLogger.d("AlarmService", "Alarm #$alarmId scheduled to auto-silence in $silenceMinutes min ($timeoutMillis ms)")
                delay(timeoutMillis)

                AppLogger.w("AlarmService", "Alarm #$alarmId timed out after $silenceMinutes min. Triggering Missed Alarm notification.")

                if (alarmId != -1L) {
                    NotificationHelper.showMissedAlarmNotification(this@AlarmService, alarmId, label, timeStr)

                    CoroutineScope(Dispatchers.IO).launch {
                        val db = ClockityDatabase.getDatabase(this@AlarmService)
                        val alarm = db.alarmDao().getAlarmById(alarmId)
                        if (alarm != null) {
                            if (!alarm.isRepeating()) {
                                db.alarmDao().setAlarmEnabled(alarmId, false)
                            } else {
                                AlarmScheduler.scheduleAlarm(this@AlarmService, alarm)
                            }
                        }
                    }
                }

                // Notify AlarmRingingActivity to finish without user clicking manual dismiss
                val timeoutIntent = Intent(AlarmReceiver.ACTION_ALARM_TIMED_OUT).apply {
                    putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                    setPackage(packageName)
                }
                sendBroadcast(timeoutIntent)

                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        autoTimeoutJob?.cancel()
        SoundUtils.stopAlarm()
        VibrationUtils.stopVibration(this)
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        serviceScope.cancel()
        AppLogger.d("AlarmService", "AlarmService destroyed")
    }
}

package com.clockity.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.clockity.app.data.local.ClockityDatabase
import com.clockity.app.ui.alarm.AlarmRingingActivity
import com.clockity.app.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.clockity.app.ACTION_TRIGGER_ALARM"
        const val ACTION_DISMISS_ALARM = "com.clockity.app.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.clockity.app.ACTION_SNOOZE_ALARM"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_ALARM_TIME = "extra_alarm_time"
        const val EXTRA_GENTLE_WAKE = "extra_gentle_wake"
        const val EXTRA_VIBRATION_PATTERN = "extra_vibration_pattern"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_SNOOZE_REPEAT_COUNT = "extra_snooze_repeat_count"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"
        val timeStr = intent.getStringExtra(EXTRA_ALARM_TIME) ?: ""
        val isGentleWake = intent.getBooleanExtra(EXTRA_GENTLE_WAKE, true)
        val vibrationPattern = intent.getStringExtra(EXTRA_VIBRATION_PATTERN) ?: "Basic"
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)

        when (intent.action) {
            ACTION_TRIGGER_ALARM -> {
                // 1. Dismiss any upcoming notification if still hanging
                NotificationHelper.cancelUpcomingAlarmNotification(context, alarmId)

                // 2. Start Foreground Alarm Service (audio + vibration)
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    putExtra(EXTRA_ALARM_LABEL, label)
                    putExtra(EXTRA_ALARM_TIME, timeStr)
                    putExtra(EXTRA_GENTLE_WAKE, isGentleWake)
                    putExtra(EXTRA_VIBRATION_PATTERN, vibrationPattern)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // 3. Launch Fullscreen Ringing Activity
                val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    putExtra(EXTRA_ALARM_LABEL, label)
                    putExtra(EXTRA_ALARM_TIME, timeStr)
                    putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(ringingIntent)
            }

            ACTION_DISMISS_ALARM -> {
                // Stop service & sound
                context.stopService(Intent(context, AlarmService::class.java))

                // Reschedule or disable once-off alarm in database
                if (alarmId != -1L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = ClockityDatabase.getDatabase(context)
                        val alarm = db.alarmDao().getAlarmById(alarmId)
                        if (alarm != null) {
                            if (!alarm.isRepeating()) {
                                db.alarmDao().setAlarmEnabled(alarmId, false)
                            } else {
                                AlarmScheduler.scheduleAlarm(context, alarm)
                            }
                        }
                    }
                }
            }

            ACTION_SNOOZE_ALARM -> {
                // Stop current ringing
                context.stopService(Intent(context, AlarmService::class.java))
                // Schedule snooze
                if (alarmId != -1L) {
                    AlarmScheduler.scheduleSnooze(context, alarmId, snoozeMinutes, label)
                }
            }
        }
    }
}

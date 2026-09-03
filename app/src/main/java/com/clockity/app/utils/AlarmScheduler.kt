package com.clockity.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.clockity.app.data.models.Alarm
import com.clockity.app.service.AlarmReceiver
import com.clockity.app.service.UpcomingAlarmReceiver

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"
    private const val UPCOMING_OFFSET_MILLIS = 30 * 60 * 1000L // 30 mins before

    fun scheduleAlarm(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Calculate next trigger time
        val triggerMillis = TimeUtils.getNextTriggerMillis(alarm)
        val (timeStr, amPm) = alarm.formatTime12H()

        // 2. Main Alarm Intent
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmReceiver.EXTRA_ALARM_TIME, "$timeStr $amPm")
            putExtra(AlarmReceiver.EXTRA_GENTLE_WAKE, alarm.isGentleWakeUp)
            putExtra(AlarmReceiver.EXTRA_VIBRATION_PATTERN, alarm.vibrationPattern)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, alarm.snoozeDurationMinutes)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_REPEAT_COUNT, alarm.snoozeRepeatCount)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Set exact alarm using setAlarmClock (high priority, exempt from Doze and background restrictions)
        try {
            val showIntent = Intent(context, com.clockity.app.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                alarm.id.toInt(),
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Scheduled alarm #${alarm.id} with setAlarmClock for $triggerMillis")
        } catch (e: Exception) {
            Log.w(TAG, "setAlarmClock failed, falling back to setExactAndAllowWhileIdle", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to schedule alarm", e2)
            }
        }

        // 4. Schedule Upcoming Notification (30 mins before if in the future)
        val upcomingMillis = triggerMillis - UPCOMING_OFFSET_MILLIS
        if (upcomingMillis > System.currentTimeMillis()) {
            val upcomingIntent = Intent(context, UpcomingAlarmReceiver::class.java).apply {
                action = UpcomingAlarmReceiver.ACTION_SHOW_UPCOMING
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            }
            val upcomingPendingIntent = PendingIntent.getBroadcast(
                context,
                (alarm.id + 5000).toInt(),
                upcomingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    upcomingMillis,
                    upcomingPendingIntent
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule upcoming alarm notice", e)
            }
        }
    }

    fun scheduleSnooze(context: Context, alarmId: Long, snoozeMinutes: Int, label: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snoozeMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, "$label (Snoozed)")
            putExtra(AlarmReceiver.EXTRA_GENTLE_WAKE, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val showIntent = Intent(context, com.clockity.app.MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                alarmId.toInt(),
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(snoozeMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                snoozeMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancel main alarm
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Cancel upcoming notice
        val upcomingIntent = Intent(context, UpcomingAlarmReceiver::class.java)
        val upcomingPendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + 5000).toInt(),
            upcomingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(upcomingPendingIntent)
    }
}

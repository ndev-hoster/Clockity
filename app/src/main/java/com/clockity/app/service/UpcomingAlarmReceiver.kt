package com.clockity.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clockity.app.data.local.ClockityDatabase
import com.clockity.app.utils.AlarmScheduler
import com.clockity.app.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpcomingAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW_UPCOMING = "com.clockity.app.ACTION_SHOW_UPCOMING"
        const val ACTION_DISMISS_UPCOMING = "com.clockity.app.ACTION_DISMISS_UPCOMING"
        const val ACTION_SNOOZE_UPCOMING = "com.clockity.app.ACTION_SNOOZE_UPCOMING"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return
        AppLogger.i("UpcomingAlarmReceiver", "Received action=${intent.action} for alarmId=$alarmId")

        when (intent.action) {
            ACTION_SHOW_UPCOMING -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = ClockityDatabase.getDatabase(context)
                    val alarm = db.alarmDao().getAlarmById(alarmId)
                    if (alarm != null && alarm.isEnabled) {
                        AppLogger.d("UpcomingAlarmReceiver", "Showing upcoming notification for alarm $alarmId")
                        NotificationHelper.showUpcomingAlarmNotification(context, alarm)
                    } else {
                        AppLogger.w("UpcomingAlarmReceiver", "Alarm $alarmId is null or disabled; skipping upcoming notification")
                    }
                }
            }

            ACTION_SNOOZE_UPCOMING -> {
                // User pressed "Snooze (5m)" on upcoming notice
                AppLogger.i("UpcomingAlarmReceiver", "Upcoming alarm $alarmId snoozed by 5 minutes")
                NotificationHelper.cancelUpcomingAlarmNotification(context, alarmId)
                AlarmScheduler.cancelAlarm(context, alarmId)

                val label = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Alarm"
                AlarmScheduler.scheduleSnooze(context, alarmId, 5, label)
            }

            ACTION_DISMISS_UPCOMING -> {
                // User pressed "Dismiss Now" from the 30-min prior notification
                AppLogger.i("UpcomingAlarmReceiver", "Upcoming alarm $alarmId dismissed early by user")
                NotificationHelper.cancelUpcomingAlarmNotification(context, alarmId)
                AlarmScheduler.cancelAlarm(context, alarmId)

                CoroutineScope(Dispatchers.IO).launch {
                    val db = ClockityDatabase.getDatabase(context)
                    val alarm = db.alarmDao().getAlarmById(alarmId)
                    if (alarm != null) {
                        if (!alarm.isRepeating()) {
                            db.alarmDao().setAlarmEnabled(alarmId, false)
                        } else {
                            // Reschedule for next week / next recurring day
                            AlarmScheduler.scheduleAlarm(context, alarm)
                        }
                    }
                }
            }
        }
    }
}

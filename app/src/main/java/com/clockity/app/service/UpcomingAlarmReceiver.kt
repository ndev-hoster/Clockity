package com.clockity.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clockity.app.data.local.ClockityDatabase
import com.clockity.app.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpcomingAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW_UPCOMING = "com.clockity.app.ACTION_SHOW_UPCOMING"
        const val ACTION_DISMISS_UPCOMING = "com.clockity.app.ACTION_DISMISS_UPCOMING"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        when (intent.action) {
            ACTION_SHOW_UPCOMING -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = ClockityDatabase.getDatabase(context)
                    val alarm = db.alarmDao().getAlarmById(alarmId)
                    if (alarm != null && alarm.isEnabled) {
                        NotificationHelper.showUpcomingAlarmNotification(context, alarm)
                    }
                }
            }

            ACTION_DISMISS_UPCOMING -> {
                // User pressed "Dismiss Now" from the 30-min prior notification
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

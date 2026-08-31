package com.clockity.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.clockity.app.MainActivity
import com.clockity.app.R
import com.clockity.app.data.models.Alarm
import com.clockity.app.ui.alarm.AlarmRingingActivity

object NotificationHelper {

    const val ALARM_CHANNEL_ID = "clockity_alarm_channel"
    const val UPCOMING_CHANNEL_ID = "clockity_upcoming_channel"
    const val TIMER_CHANNEL_ID = "clockity_timer_channel"

    const val NOTIFICATION_ID_ALARM = 1001
    const val NOTIFICATION_ID_UPCOMING_BASE = 2000
    const val NOTIFICATION_ID_TIMER = 3001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Alarm Ringing Channel
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                context.getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.alarm_channel_desc)
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            // Upcoming Alarm Channel (Silent notice 30m prior)
            val upcomingChannel = NotificationChannel(
                UPCOMING_CHANNEL_ID,
                context.getString(R.string.upcoming_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.upcoming_channel_desc)
                enableVibration(false)
                setShowBadge(false)
            }

            // Timer Channel
            val timerChannel = NotificationChannel(
                TIMER_CHANNEL_ID,
                context.getString(R.string.timer_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.timer_channel_desc)
            }

            manager.createNotificationChannels(listOf(alarmChannel, upcomingChannel, timerChannel))
        }
    }

    fun buildAlarmRingingNotification(context: Context, alarmId: Long, label: String, timeStr: String): Notification {
        val fullScreenIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmReceiver.EXTRA_ALARM_TIME, timeStr)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt() + 100,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt() + 200,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label.ifBlank { "Alarm" })
            .setContentText(timeStr)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze", snoozePendingIntent)
            .build()
    }

    fun showUpcomingAlarmNotification(context: Context, alarm: Alarm) {
        val (timeStr, amPm) = alarm.formatTime12H()
        val fullTime = "$timeStr $amPm"

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick Dismiss PendingIntent
        val dismissIntent = Intent(context, UpcomingAlarmReceiver::class.java).apply {
            action = UpcomingAlarmReceiver.ACTION_DISMISS_UPCOMING
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (NOTIFICATION_ID_UPCOMING_BASE + alarm.id).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, UPCOMING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Upcoming alarm: $fullTime")
            .setContentText(alarm.label.ifBlank { "Scheduled Alarm" })
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss Now", dismissPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((NOTIFICATION_ID_UPCOMING_BASE + alarm.id).toInt(), notification)
    }

    fun cancelUpcomingAlarmNotification(context: Context, alarmId: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel((NOTIFICATION_ID_UPCOMING_BASE + alarmId).toInt())
    }
}

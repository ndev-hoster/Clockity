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
import com.clockity.app.data.models.ActiveTimer
import com.clockity.app.data.models.Alarm
import com.clockity.app.data.models.PomodoroState
import com.clockity.app.ui.alarm.AlarmRingingActivity

object NotificationHelper {

    const val ALARM_CHANNEL_ID = "clockity_alarm_channel"
    const val UPCOMING_CHANNEL_ID = "clockity_upcoming_channel"
    const val TIMER_CHANNEL_ID = "clockity_timer_channel"
    const val TIMER_RING_CHANNEL_ID = "clockity_timer_ring_channel"

    const val NOTIFICATION_ID_ALARM = 1001
    const val NOTIFICATION_ID_UPCOMING_BASE = 2000
    const val NOTIFICATION_ID_TIMER = 3001
    const val NOTIFICATION_ID_TIMER_FINISHED = 3002

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

            // Running Timer Channel
            val timerChannel = NotificationChannel(
                TIMER_CHANNEL_ID,
                context.getString(R.string.timer_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.timer_channel_desc)
                setShowBadge(false)
            }

            // Timer Finished Ring Channel
            val timerRingChannel = NotificationChannel(
                TIMER_RING_CHANNEL_ID,
                "Timer Finished Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarm alert when a timer reaches zero"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            manager.createNotificationChannels(listOf(alarmChannel, upcomingChannel, timerChannel, timerRingChannel))
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

    fun buildRunningTimerNotification(context: Context, timer: ActiveTimer): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_tab", 2) // Timer tab
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            301,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause or Resume action
        val toggleAction = if (timer.isRunning) TimerService.ACTION_PAUSE else TimerService.ACTION_RESUME
        val toggleTitle = if (timer.isRunning) "Pause" else "Resume"
        val toggleIntent = Intent(context, TimerService::class.java).apply {
            action = toggleAction
            putExtra("timer_id", timer.id)
        }
        val togglePendingIntent = PendingIntent.getService(
            context,
            302,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // +1m Action
        val plusOneIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_ADD_MINUTE
            putExtra("timer_id", timer.id)
        }
        val plusOnePendingIntent = PendingIntent.getService(
            context,
            303,
            plusOneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel Action
        val cancelIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_CANCEL
            putExtra("timer_id", timer.id)
        }
        val cancelPendingIntent = PendingIntent.getService(
            context,
            304,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val totalSecs = (timer.totalMillis / 1000).toInt()
        val remSecs = (timer.remainingMillis / 1000).toInt()
        val formattedTime = timer.formatRemainingTime()

        val builder = NotificationCompat.Builder(context, TIMER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(timer.title)
            .setContentText(if (timer.isRunning) formattedTime else "$formattedTime (Paused)")
            .setProgress(totalSecs.coerceAtLeast(1), remSecs, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, toggleTitle, togglePendingIntent)
            .addAction(android.R.drawable.ic_input_add, "+1m", plusOnePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)

        return builder.build()
    }

    fun buildPomodoroNotification(context: Context, pomo: PomodoroState): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_tab", 2)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            401,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleAction = if (pomo.isRunning) TimerService.ACTION_POMO_PAUSE else TimerService.ACTION_POMO_START
        val toggleTitle = if (pomo.isRunning) "Pause" else "Resume"
        val toggleIntent = Intent(context, TimerService::class.java).apply {
            action = toggleAction
        }
        val togglePendingIntent = PendingIntent.getService(
            context,
            402,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_POMO_SKIP
        }
        val skipPendingIntent = PendingIntent.getService(
            context,
            403,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedTime = pomo.formatRemainingTime()

        return NotificationCompat.Builder(context, TIMER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("${pomo.phase.title} (${pomo.currentCycle}/${pomo.totalCycles})")
            .setContentText(if (pomo.isRunning) formattedTime else "$formattedTime (Paused)")
            .setProgress(pomo.totalSeconds.toInt(), pomo.remainingSeconds.toInt(), false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, toggleTitle, togglePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Skip Phase", skipPendingIntent)
            .build()
    }

    fun showTimerFinishedNotification(context: Context, timer: ActiveTimer) {
        val stopIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP_RINGING
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            305,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_tab", 2)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            306,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, TIMER_RING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer Finished!")
            .setContentText("${timer.title} has completed.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_TIMER_FINISHED, notification)
    }

    fun cancelTimerFinishedNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID_TIMER_FINISHED)
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

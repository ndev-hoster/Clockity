package com.clockity.app.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.clockity.app.utils.TimerManager

class TimerService : Service() {

    companion object {
        const val ACTION_UPDATE_NOTIFICATION = "com.clockity.app.action.UPDATE_TIMER_NOTIF"
        const val ACTION_PAUSE = "com.clockity.app.action.PAUSE_TIMER"
        const val ACTION_RESUME = "com.clockity.app.action.RESUME_TIMER"
        const val ACTION_ADD_MINUTE = "com.clockity.app.action.ADD_MINUTE_TIMER"
        const val ACTION_CANCEL = "com.clockity.app.action.CANCEL_TIMER"

        const val ACTION_POMO_START = "com.clockity.app.action.POMO_START"
        const val ACTION_POMO_PAUSE = "com.clockity.app.action.POMO_PAUSE"
        const val ACTION_POMO_SKIP = "com.clockity.app.action.POMO_SKIP"

        const val ACTION_STOP_RINGING = "com.clockity.app.action.STOP_RINGING"
        const val ACTION_STOP_SERVICE = "com.clockity.app.action.STOP_TIMER_SERVICE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timerId = intent?.getStringExtra("timer_id")

        when (intent?.action) {
            ACTION_PAUSE -> {
                timerId?.let { TimerManager.pauseTimer(it) }
            }
            ACTION_RESUME -> {
                timerId?.let { TimerManager.resumeTimer(it) }
            }
            ACTION_ADD_MINUTE -> {
                timerId?.let { TimerManager.addOneMinute(it) }
            }
            ACTION_CANCEL -> {
                timerId?.let { TimerManager.cancelTimer(it) }
            }
            ACTION_POMO_START -> {
                TimerManager.startPomodoro()
            }
            ACTION_POMO_PAUSE -> {
                TimerManager.pausePomodoro()
            }
            ACTION_POMO_SKIP -> {
                TimerManager.skipPomodoroPhase()
            }
            ACTION_STOP_RINGING -> {
                TimerManager.stopRinging()
            }
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        updateNotification()
        return START_STICKY
    }

    private fun updateNotification() {
        val runningTimers = TimerManager.activeTimers.value.filter { it.isRunning || it.isPaused }
        val pomoState = TimerManager.pomodoroState.value

        if (runningTimers.isNotEmpty()) {
            val primaryTimer = runningTimers.first()
            val notif = NotificationHelper.buildRunningTimerNotification(this, primaryTimer)
            startForeground(NotificationHelper.NOTIFICATION_ID_TIMER, notif)
        } else if (pomoState.isRunning || pomoState.isPaused) {
            val notif = NotificationHelper.buildPomodoroNotification(this, pomoState)
            startForeground(NotificationHelper.NOTIFICATION_ID_TIMER, notif)
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
}

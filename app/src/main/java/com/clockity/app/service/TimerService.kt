package com.clockity.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.clockity.app.R

class TimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("timer_title") ?: "Timer"
        val remaining = intent?.getStringExtra("timer_remaining") ?: "Running"

        val notification = NotificationCompat.Builder(this, NotificationHelper.TIMER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(remaining)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NotificationHelper.NOTIFICATION_ID_TIMER, notification)
        return START_NOT_STICKY
    }
}

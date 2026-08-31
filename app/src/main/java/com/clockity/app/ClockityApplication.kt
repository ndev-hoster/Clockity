package com.clockity.app

import android.app.Application
import com.clockity.app.service.NotificationHelper
import com.clockity.app.utils.TimerManager

class ClockityApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        TimerManager.init(this)
    }
}

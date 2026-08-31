package com.clockity.app

import android.app.Application
import com.clockity.app.service.NotificationHelper

class ClockityApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }
}

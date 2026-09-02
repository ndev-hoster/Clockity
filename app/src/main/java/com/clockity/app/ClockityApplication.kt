package com.clockity.app

import android.app.Application
import com.clockity.app.service.NotificationHelper
import com.clockity.app.utils.PreferencesManager
import com.clockity.app.utils.TimerManager

class ClockityApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        PreferencesManager.init(this)
        TimerManager.init(this)
    }
}

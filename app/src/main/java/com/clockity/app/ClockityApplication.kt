package com.clockity.app

import android.app.Application
import com.clockity.app.service.NotificationHelper
import com.clockity.app.utils.AppLogger
import com.clockity.app.utils.PreferencesManager
import com.clockity.app.utils.TimerManager

class ClockityApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PreferencesManager.init(this)
        AppLogger.init(this)
        NotificationHelper.createNotificationChannels(this)
        TimerManager.init(this)
    }
}

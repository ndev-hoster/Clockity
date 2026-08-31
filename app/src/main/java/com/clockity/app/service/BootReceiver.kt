package com.clockity.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clockity.app.data.local.ClockityDatabase
import com.clockity.app.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = ClockityDatabase.getDatabase(context)
                val enabledAlarms = db.alarmDao().getEnabledAlarms()
                for (alarm in enabledAlarms) {
                    AlarmScheduler.scheduleAlarm(context, alarm)
                }
            }
        }
    }
}

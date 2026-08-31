package com.clockity.app.data.models

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val app: String = "Clockity",
    val alarms: List<Alarm> = emptyList(),
    val groups: List<AlarmGroup> = emptyList(),
    val cities: List<WorldCity> = emptyList(),
    val timerPresets: List<TimerPreset> = emptyList()
)

package com.clockity.app.ui.alarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clockity.app.data.local.ClockityDatabase
import com.clockity.app.data.models.Alarm
import com.clockity.app.data.models.AlarmGroup
import com.clockity.app.utils.AlarmScheduler
import com.clockity.app.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AlarmUiState(
    val groups: List<AlarmGroup> = emptyList(),
    val alarms: List<Alarm> = emptyList(),
    val nextAlarmSummary: String = "No alarms set"
)

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ClockityDatabase.getDatabase(application)
    private val alarmDao = db.alarmDao()
    private val groupDao = db.alarmGroupDao()

    val uiState: StateFlow<AlarmUiState> = combine(
        groupDao.getAllGroups(),
        alarmDao.getAllAlarms()
    ) { groups, alarms ->
        val enabledAlarms = alarms.filter { it.isEnabled }
        val nextSummary = if (enabledAlarms.isEmpty()) {
            "All alarms turned off"
        } else {
            val nearestMillis = enabledAlarms.minOf { TimeUtils.getNextTriggerMillis(it) }
            TimeUtils.formatTimeUntil(nearestMillis)
        }
        AlarmUiState(
            groups = groups,
            alarms = alarms,
            nextAlarmSummary = nextSummary
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlarmUiState()
    )

    fun toggleAlarm(alarm: Alarm) {
        val newState = !alarm.isEnabled
        viewModelScope.launch(Dispatchers.IO) {
            val updated = alarm.copy(isEnabled = newState)
            alarmDao.updateAlarm(updated)
            if (newState) {
                AlarmScheduler.scheduleAlarm(getApplication(), updated)
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
            }
        }
    }

    /**
     * Bulk toggles all alarms belonging to a specific group ON or OFF
     */
    fun toggleGroup(group: AlarmGroup) {
        val newGroupState = !group.isEnabled
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Update group status
            groupDao.setGroupEnabled(group.id, newGroupState)

            // 2. Update all alarms in this group
            alarmDao.setGroupAlarmsEnabled(group.id, newGroupState)

            // 3. Reschedule or cancel alarms in system AlarmManager
            val groupAlarms = alarmDao.getAlarmsByGroupId(group.id)
            for (alarm in groupAlarms) {
                if (newGroupState) {
                    AlarmScheduler.scheduleAlarm(getApplication(), alarm.copy(isEnabled = true))
                } else {
                    AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
                }
            }
        }
    }

    fun toggleGroupExpansion(group: AlarmGroup) {
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.setGroupExpanded(group.id, !group.isExpanded)
        }
    }

    fun saveAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingList = uiState.value.alarms
            // Check for identical alarm: same hour, minute, daysOfWeek, specificDateMillis, groupId
            val matching = existingList.firstOrNull {
                it.id != alarm.id &&
                it.hour == alarm.hour &&
                it.minute == alarm.minute &&
                it.daysOfWeek == alarm.daysOfWeek &&
                it.specificDateMillis == alarm.specificDateMillis &&
                it.groupId == alarm.groupId
            }

            if (alarm.id == 0L) {
                if (matching != null) {
                    // Update and re-enable existing identical alarm instead of duplicating
                    val updated = matching.copy(
                        label = if (alarm.label.isNotBlank()) alarm.label else matching.label,
                        isGentleWakeUp = alarm.isGentleWakeUp,
                        snoozeDurationMinutes = alarm.snoozeDurationMinutes,
                        vibrationPattern = alarm.vibrationPattern,
                        isEnabled = true
                    )
                    alarmDao.updateAlarm(updated)
                    AlarmScheduler.scheduleAlarm(getApplication(), updated)
                } else {
                    val newId = alarmDao.insertAlarm(alarm)
                    val inserted = alarm.copy(id = newId)
                    if (inserted.isEnabled) {
                        AlarmScheduler.scheduleAlarm(getApplication(), inserted)
                    }
                }
            } else {
                if (matching != null) {
                    // Duplicate found: remove obsolete duplicate
                    alarmDao.deleteAlarm(matching)
                    AlarmScheduler.cancelAlarm(getApplication(), matching.id)
                }
                alarmDao.updateAlarm(alarm)
                if (alarm.isEnabled) {
                    AlarmScheduler.scheduleAlarm(getApplication(), alarm)
                } else {
                    AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
                }
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
            alarmDao.deleteAlarm(alarm)
        }
    }

    fun moveAlarmToGroup(alarm: Alarm, targetGroupId: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = alarm.copy(groupId = targetGroupId)
            alarmDao.updateAlarm(updated)
        }
    }

    fun createGroup(name: String, colorHex: String = "#3E82F7") {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val exists = uiState.value.groups.any { it.name.equals(cleanName, ignoreCase = true) }
            if (!exists) {
                groupDao.insertGroup(
                    AlarmGroup(
                        name = cleanName,
                        isEnabled = true,
                        isExpanded = false,
                        colorHex = colorHex
                    )
                )
            }
        }
    }

    fun updateGroup(group: AlarmGroup) {
        val cleanName = group.name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val exists = uiState.value.groups.any { it.id != group.id && it.name.equals(cleanName, ignoreCase = true) }
            if (!exists) {
                groupDao.updateGroup(group.copy(name = cleanName))
            }
        }
    }

    fun deleteGroup(group: AlarmGroup) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmDao.unassignAlarmsFromGroup(group.id)
            groupDao.deleteGroup(group)
        }
    }
}

package com.clockity.app.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alarms",
    foreignKeys = [
        ForeignKey(
            entity = AlarmGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("groupId")]
)
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val isEnabled: Boolean = true,
    /**
     * Bitmask of active days (bit 1 = Monday, bit 2 = Tuesday, ..., bit 7 = Sunday).
     * 0 indicates a one-time (once-off) alarm.
     */
    val daysOfWeek: Int = 0,
    val groupId: Long? = null,
    val isGentleWakeUp: Boolean = true,
    val snoozeDurationMinutes: Int = 5,
    val snoozeRepeatCount: Int = 3,
    val vibrationPattern: String = "Basic",
    val soundTitle: String = "One UI Bell",
    val soundUri: String = "default",
    val specificDateMillis: Long? = null
) {
    fun isRepeating(): Boolean = daysOfWeek != 0 && (specificDateMillis == null || specificDateMillis <= 0)

    fun isDaySelected(dayOfWeek1To7: Int): Boolean {
        return (daysOfWeek and (1 shl dayOfWeek1To7)) != 0
    }

    fun toggleDay(dayOfWeek1To7: Int): Alarm {
        val newMask = daysOfWeek xor (1 shl dayOfWeek1To7)
        return copy(daysOfWeek = newMask, specificDateMillis = null)
    }

    fun formatDaysSummary(): String {
        if (specificDateMillis != null && specificDateMillis > 0) {
            val sdf = java.text.SimpleDateFormat("EEE, MMM d", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(specificDateMillis))
        }
        if (daysOfWeek == 0) return "Once"
        if (daysOfWeek == 0b11111110 || daysOfWeek == 254 || daysOfWeek == 127) return "Every day"
        if (daysOfWeek == 0b01111100 || daysOfWeek == 62 || daysOfWeek == 31) return "Weekdays"
        if (daysOfWeek == 0b11000000 || daysOfWeek == 192 || daysOfWeek == 96) return "Weekends"

        val dayNames = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val active = mutableListOf<String>()
        for (i in 1..7) {
            if (isDaySelected(i)) {
                active.add(dayNames[i])
            }
        }
        return active.joinToString(", ")
    }

    fun formatTime12H(): Pair<String, String> {
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val amPm = if (hour < 12) "AM" else "PM"
        val timeStr = String.format("%d:%02d", h12, minute)
        return Pair(timeStr, amPm)
    }
}

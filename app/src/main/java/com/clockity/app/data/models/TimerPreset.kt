package com.clockity.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timer_presets")
data class TimerPreset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val totalSeconds: Long,
    val emoji: String = ""
) {
    fun formatDurationSummary(): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val parts = mutableListOf<String>()
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0) parts.add("${minutes}m")
        if (seconds > 0 || parts.isEmpty()) parts.add("${seconds}s")
        return parts.joinToString(" ")
    }
}

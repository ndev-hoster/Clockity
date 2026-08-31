package com.clockity.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Entity(tableName = "world_cities")
data class WorldCity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cityName: String,
    val countryName: String,
    val timeZoneId: String,
    val displayOrder: Int = 0
) {
    fun getZonedDateTime(offsetHours: Float = 0f): ZonedDateTime {
        val zone = ZoneId.of(timeZoneId)
        val now = Instant.now().plusMillis((offsetHours * 3600 * 1000).toLong())
        return ZonedDateTime.ofInstant(now, zone)
    }

    fun formatTime(offsetHours: Float = 0f): Pair<String, String> {
        val zdt = getZonedDateTime(offsetHours)
        val timeStr = zdt.format(DateTimeFormatter.ofPattern("h:mm", Locale.getDefault()))
        val amPm = zdt.format(DateTimeFormatter.ofPattern("a", Locale.getDefault())).uppercase()
        return Pair(timeStr, amPm)
    }

    fun formatTimeDifference(): String {
        val localZone = ZoneId.systemDefault()
        val targetZone = ZoneId.of(timeZoneId)
        val now = Instant.now()
        val localOffset = localZone.rules.getOffset(now).totalSeconds
        val targetOffset = targetZone.rules.getOffset(now).totalSeconds
        val diffSeconds = targetOffset - localOffset
        val diffHours = diffSeconds / 3600.0

        return when {
            diffHours == 0.0 -> "Same time"
            diffHours > 0 -> {
                val formatted = if (diffHours % 1 == 0.0) "${diffHours.toInt()}" else String.format("%.1f", diffHours)
                "+$formatted hrs"
            }
            else -> {
                val absH = -diffHours
                val formatted = if (absH % 1 == 0.0) "${absH.toInt()}" else String.format("%.1f", absH)
                "-$formatted hrs"
            }
        }
    }

    fun isDaytime(offsetHours: Float = 0f): Boolean {
        val hour = getZonedDateTime(offsetHours).hour
        return hour in 6..18
    }
}

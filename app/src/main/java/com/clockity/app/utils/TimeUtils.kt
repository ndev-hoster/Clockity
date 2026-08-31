package com.clockity.app.utils

import com.clockity.app.data.models.Alarm
import java.util.Calendar

object TimeUtils {

    /**
     * Calculates the exact next epoch millis timestamp when this alarm should trigger.
     */
    fun getNextTriggerMillis(alarm: Alarm): Long {
        val now = Calendar.getInstance()

        // 1. Specific Calendar Date Alarm
        if (alarm.specificDateMillis != null) {
            val dateCal = Calendar.getInstance().apply {
                timeInMillis = alarm.specificDateMillis
            }
            val target = Calendar.getInstance().apply {
                set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
                set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return target.timeInMillis
        }

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!alarm.isRepeating()) {
            // Once-off alarm: if time has passed today, trigger tomorrow
            if (target.before(now) || target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Repeating alarm: Find next matching day
        // Java Calendar.DAY_OF_WEEK: Sunday=1, Monday=2, ..., Saturday=7
        // Our bitmask: bit 1=Monday (Calendar 2) .. bit 7=Sunday (Calendar 1)
        for (dayOffset in 0..7) {
            val candidate = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            if (candidate.timeInMillis > now.timeInMillis) {
                val calDay = candidate.get(Calendar.DAY_OF_WEEK)
                val bitmaskIndex = if (calDay == Calendar.SUNDAY) 7 else calDay - 1
                if (alarm.isDaySelected(bitmaskIndex)) {
                    return candidate.timeInMillis
                }
            }
        }

        // Fallback: tomorrow same time
        target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis
    }

    /**
     * Returns a human-friendly duration string, e.g. "Alarm in 7 hours 23 minutes"
     */
    fun formatTimeUntil(nextMillis: Long): String {
        val diff = nextMillis - System.currentTimeMillis()
        if (diff <= 0) return "Alarm ringing soon"

        val totalMinutes = diff / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val days = hours / 24
        val remHours = hours % 24

        return when {
            days > 0 -> "Alarm in $days d $remHours h"
            hours > 0 && minutes > 0 -> "Alarm in $hours h $minutes min"
            hours > 0 -> "Alarm in $hours hours"
            minutes > 0 -> "Alarm in $minutes minutes"
            else -> "Alarm in less than a minute"
        }
    }

    /**
     * Formats milliseconds into mm:ss.SS for stopwatch
     */
    fun formatStopwatchTime(timeMillis: Long): Triple<String, String, String> {
        val hundredths = (timeMillis % 1000) / 10
        val totalSeconds = timeMillis / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600

        val mainPart = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
        val hundredthsPart = String.format(".%02d", hundredths)
        return Triple(mainPart, hundredthsPart, String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths))
    }
}

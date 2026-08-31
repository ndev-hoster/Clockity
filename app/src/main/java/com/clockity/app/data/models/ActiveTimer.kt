package com.clockity.app.data.models

import java.util.UUID

data class ActiveTimer(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Timer",
    val totalMillis: Long,
    val remainingMillis: Long,
    val isRunning: Boolean = true,
    val isPaused: Boolean = false
) {
    val progress: Float
        get() = if (totalMillis > 0) remainingMillis.toFloat() / totalMillis.toFloat() else 0f

    fun formatRemaining(): String {
        val totalSecs = (remainingMillis + 999) / 1000
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}

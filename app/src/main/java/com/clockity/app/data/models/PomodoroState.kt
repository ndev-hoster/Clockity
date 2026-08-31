package com.clockity.app.data.models

enum class PomodoroPhase(val title: String, val defaultMinutes: Int) {
    FOCUS("Focus Session", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15)
}

data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val currentCycle: Int = 1,          // 1 to 4
    val totalCycles: Int = 4,
    val totalSeconds: Long = 25 * 60L,
    val remainingSeconds: Long = 25 * 60L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val completedSessionsToday: Int = 0,
    val totalFocusMinutesToday: Int = 0
) {
    val progress: Float
        get() = if (totalSeconds > 0) 1f - (remainingSeconds.toFloat() / totalSeconds.toFloat()) else 0f

    fun formatRemainingTime(): String {
        val mins = remainingSeconds / 60
        val secs = remainingSeconds % 60
        return String.format("%02d:%02d", mins, secs)
    }
}

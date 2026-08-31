package com.clockity.app.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clockity.app.data.local.ClockityDatabase
import com.clockity.app.data.models.ActiveTimer
import com.clockity.app.data.models.TimerPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TimerUiState(
    val presets: List<TimerPreset> = emptyList(),
    val activeTimers: List<ActiveTimer> = emptyList()
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ClockityDatabase.getDatabase(application)
    private val presetDao = db.timerPresetDao()

    private val _activeTimers = MutableStateFlow<List<ActiveTimer>>(emptyList())
    private var tickerJob: Job? = null

    val uiState: StateFlow<TimerUiState> = combine(
        presetDao.getAllPresets(),
        _activeTimers
    ) { presets, active ->
        TimerUiState(
            presets = presets,
            activeTimers = active
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerUiState()
    )

    init {
        startTickerLoop()
        viewModelScope.launch(Dispatchers.IO) {
            if (presetDao.getCount() == 0) {
                val defaultPresets = listOf(
                    TimerPreset(title = "Tea / Coffee", totalSeconds = 180, emoji = ""),
                    TimerPreset(title = "Boiled Eggs", totalSeconds = 480, emoji = ""),
                    TimerPreset(title = "Power Nap", totalSeconds = 1200, emoji = ""),
                    TimerPreset(title = "Pomodoro", totalSeconds = 1500, emoji = ""),
                    TimerPreset(title = "Quick Workout", totalSeconds = 2700, emoji = ""),
                    TimerPreset(title = "Face Mask", totalSeconds = 900, emoji = "")
                )
                presetDao.insertAll(defaultPresets)
            }
        }
    }

    private fun startTickerLoop() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(200)
                val currentList = _activeTimers.value
                if (currentList.any { it.isRunning }) {
                    val updated = currentList.map { timer ->
                        if (timer.isRunning && timer.remainingMillis > 0) {
                            val newRemaining = (timer.remainingMillis - 200).coerceAtLeast(0L)
                            timer.copy(
                                remainingMillis = newRemaining,
                                isRunning = newRemaining > 0
                            )
                        } else {
                            timer
                        }
                    }
                    _activeTimers.value = updated
                }
            }
        }
    }

    fun startPreset(preset: TimerPreset) {
        val totalMs = preset.totalSeconds * 1000L
        val timer = ActiveTimer(
            title = preset.title,
            totalMillis = totalMs,
            remainingMillis = totalMs,
            isRunning = true
        )
        _activeTimers.value = listOf(timer) + _activeTimers.value
    }

    fun startCustomTimer(hours: Int, minutes: Int, seconds: Int, label: String) {
        val totalSeconds = (hours * 3600L) + (minutes * 60L) + seconds
        if (totalSeconds <= 0) return
        val totalMs = totalSeconds * 1000L
        val timer = ActiveTimer(
            title = label.ifBlank { "Timer" },
            totalMillis = totalMs,
            remainingMillis = totalMs,
            isRunning = true
        )
        _activeTimers.value = listOf(timer) + _activeTimers.value
    }

    fun pauseTimer(id: String) {
        _activeTimers.value = _activeTimers.value.map {
            if (it.id == id) it.copy(isRunning = false, isPaused = true) else it
        }
    }

    fun resumeTimer(id: String) {
        _activeTimers.value = _activeTimers.value.map {
            if (it.id == id) it.copy(isRunning = true, isPaused = false) else it
        }
    }

    fun cancelTimer(id: String) {
        _activeTimers.value = _activeTimers.value.filter { it.id != id }
    }

    fun addOneMinute(id: String) {
        _activeTimers.value = _activeTimers.value.map {
            if (it.id == id) {
                val newRem = it.remainingMillis + 60_000L
                val newTot = it.totalMillis + 60_000L
                it.copy(remainingMillis = newRem, totalMillis = newTot)
            } else it
        }
    }

    fun addPreset(title: String, durationSeconds: Long, emoji: String = "") {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank() || durationSeconds <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            val exists = uiState.value.presets.any {
                it.title.equals(cleanTitle, ignoreCase = true) && it.totalSeconds == durationSeconds
            }
            if (!exists) {
                presetDao.insertPreset(
                    TimerPreset(title = cleanTitle, totalSeconds = durationSeconds, emoji = emoji)
                )
            }
        }
    }

    fun updatePreset(preset: TimerPreset) {
        viewModelScope.launch(Dispatchers.IO) {
            presetDao.updatePreset(preset)
        }
    }

    fun deletePreset(preset: TimerPreset) {
        viewModelScope.launch(Dispatchers.IO) {
            presetDao.deletePreset(preset)
        }
    }
}

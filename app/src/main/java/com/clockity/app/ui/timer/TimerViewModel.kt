package com.clockity.app.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clockity.app.data.local.ClockityDatabase
import com.clockity.app.data.models.ActiveTimer
import com.clockity.app.data.models.PomodoroState
import com.clockity.app.data.models.TimerPreset
import com.clockity.app.utils.TimerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TimerUiState(
    val presets: List<TimerPreset> = emptyList(),
    val activeTimers: List<ActiveTimer> = emptyList(),
    val pomodoroState: PomodoroState = PomodoroState(),
    val ringingTimer: ActiveTimer? = null
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ClockityDatabase.getDatabase(application)
    private val presetDao = db.timerPresetDao()

    val uiState: StateFlow<TimerUiState> = combine(
        presetDao.getAllPresets(),
        TimerManager.activeTimers,
        TimerManager.pomodoroState,
        TimerManager.ringingTimer
    ) { presets, active, pomo, ringing ->
        TimerUiState(
            presets = presets,
            activeTimers = active,
            pomodoroState = pomo,
            ringingTimer = ringing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerUiState()
    )

    init {
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

    fun startPreset(preset: TimerPreset) {
        TimerManager.startPreset(preset)
    }

    fun startCustomTimer(hours: Int, minutes: Int, seconds: Int, label: String) {
        TimerManager.startCustomTimer(hours, minutes, seconds, label)
    }

    fun pauseTimer(id: String) {
        TimerManager.pauseTimer(id)
    }

    fun resumeTimer(id: String) {
        TimerManager.resumeTimer(id)
    }

    fun cancelTimer(id: String) {
        TimerManager.cancelTimer(id)
    }

    fun addOneMinute(id: String) {
        TimerManager.addOneMinute(id)
    }

    fun stopRinging() {
        TimerManager.stopRinging()
    }

    // Pomodoro Controls
    fun startPomodoro() {
        TimerManager.startPomodoro()
    }

    fun pausePomodoro() {
        TimerManager.pausePomodoro()
    }

    fun resetPomodoro() {
        TimerManager.resetPomodoro()
    }

    fun skipPomodoroPhase() {
        TimerManager.skipPomodoroPhase()
    }

    // Presets Management
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

package com.clockity.app.ui.stopwatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LapData(
    val lapNumber: Int,
    val lapTimeMillis: Long,
    val overallTimeMillis: Long
)

data class StopwatchUiState(
    val isRunning: Boolean = false,
    val elapsedMillis: Long = 0L,
    val laps: List<LapData> = emptyList()
) {
    val fastestLap: LapData?
        get() = if (laps.size >= 2) laps.minByOrNull { it.lapTimeMillis } else null

    val slowestLap: LapData?
        get() = if (laps.size >= 2) laps.maxByOrNull { it.lapTimeMillis } else null
}

class StopwatchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var baseTime: Long = 0L
    private var accumulatedTime: Long = 0L

    fun start() {
        if (_uiState.value.isRunning) return
        baseTime = System.currentTimeMillis() - accumulatedTime
        _uiState.value = _uiState.value.copy(isRunning = true)

        timerJob = viewModelScope.launch {
            while (true) {
                accumulatedTime = System.currentTimeMillis() - baseTime
                _uiState.value = _uiState.value.copy(elapsedMillis = accumulatedTime)
                delay(10) // 100 Hz refresh
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        timerJob = null
        _uiState.value = _uiState.value.copy(isRunning = false)
    }

    fun reset() {
        pause()
        accumulatedTime = 0L
        baseTime = 0L
        _uiState.value = StopwatchUiState()
    }

    fun recordLap() {
        val currentElapsed = _uiState.value.elapsedMillis
        val previousTotal = _uiState.value.laps.firstOrNull()?.overallTimeMillis ?: 0L
        val lapTime = currentElapsed - previousTotal
        val nextLapNum = _uiState.value.laps.size + 1

        val newLap = LapData(
            lapNumber = nextLapNum,
            lapTimeMillis = lapTime,
            overallTimeMillis = currentElapsed
        )

        // Newest lap on top (One UI behavior)
        val updatedLaps = listOf(newLap) + _uiState.value.laps
        _uiState.value = _uiState.value.copy(laps = updatedLaps)
    }
}

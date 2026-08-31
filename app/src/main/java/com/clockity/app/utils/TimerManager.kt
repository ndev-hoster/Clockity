package com.clockity.app.utils

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.clockity.app.data.models.ActiveTimer
import com.clockity.app.data.models.PomodoroPhase
import com.clockity.app.data.models.PomodoroState
import com.clockity.app.data.models.TimerPreset
import com.clockity.app.service.NotificationHelper
import com.clockity.app.service.TimerService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object TimerManager {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickerJob: Job? = null

    private val _activeTimers = MutableStateFlow<List<ActiveTimer>>(emptyList())
    val activeTimers = _activeTimers.asStateFlow()

    private val _pomodoroState = MutableStateFlow(PomodoroState())
    val pomodoroState = _pomodoroState.asStateFlow()

    private val _ringingTimer = MutableStateFlow<ActiveTimer?>(null)
    val ringingTimer = _ringingTimer.asStateFlow()

    private var appContext: Context? = null
    private var ringtone: Ringtone? = null
    private var isVibrating = false

    fun init(context: Context) {
        appContext = context.applicationContext
        startTickerLoop()
    }

    private fun startTickerLoop() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                delay(200)
                tickActiveTimers()
                tickPomodoro()
            }
        }
    }

    private fun tickActiveTimers() {
        val current = _activeTimers.value
        if (current.any { it.isRunning }) {
            var finishedTimer: ActiveTimer? = null
            val updated = current.map { timer ->
                if (timer.isRunning && timer.remainingMillis > 0) {
                    val newRemaining = (timer.remainingMillis - 200).coerceAtLeast(0L)
                    if (newRemaining == 0L && timer.remainingMillis > 0) {
                        finishedTimer = timer.copy(remainingMillis = 0L, isRunning = false)
                    }
                    timer.copy(
                        remainingMillis = newRemaining,
                        isRunning = newRemaining > 0
                    )
                } else {
                    timer
                }
            }
            _activeTimers.value = updated

            // Trigger ring if a timer just finished
            finishedTimer?.let { onTimerFinished(it) }

            // Sync with Foreground Service
            syncForegroundService()
        }
    }

    private fun tickPomodoro() {
        val currentPomo = _pomodoroState.value
        if (currentPomo.isRunning && currentPomo.remainingSeconds > 0) {
            // Check once per second
            val newSecs = (currentPomo.remainingSeconds - 1).coerceAtLeast(0L)
            if (newSecs == 0L) {
                onPomodoroPhaseFinished(currentPomo)
            } else {
                _pomodoroState.value = currentPomo.copy(remainingSeconds = newSecs)
            }
            syncForegroundService()
        }
    }

    private fun onTimerFinished(timer: ActiveTimer) {
        _ringingTimer.value = timer
        appContext?.let { ctx ->
            playAlertSoundAndVibrate(ctx)
            NotificationHelper.showTimerFinishedNotification(ctx, timer)
        }
    }

    private fun onPomodoroPhaseFinished(state: PomodoroState) {
        appContext?.let { ctx ->
            playAlertSoundAndVibrate(ctx)
        }

        // Automatic phase transition
        val nextPhase: PomodoroPhase
        val nextCycle: Int
        var completedSessions = state.completedSessionsToday
        var totalMins = state.totalFocusMinutesToday

        when (state.phase) {
            PomodoroPhase.FOCUS -> {
                completedSessions++
                totalMins += 25
                if (state.currentCycle >= state.totalCycles) {
                    nextPhase = PomodoroPhase.LONG_BREAK
                    nextCycle = 1
                } else {
                    nextPhase = PomodoroPhase.SHORT_BREAK
                    nextCycle = state.currentCycle
                }
            }
            PomodoroPhase.SHORT_BREAK -> {
                nextPhase = PomodoroPhase.FOCUS
                nextCycle = state.currentCycle + 1
            }
            PomodoroPhase.LONG_BREAK -> {
                nextPhase = PomodoroPhase.FOCUS
                nextCycle = 1
            }
        }

        _pomodoroState.value = state.copy(
            phase = nextPhase,
            currentCycle = nextCycle,
            totalSeconds = nextPhase.defaultMinutes * 60L,
            remainingSeconds = nextPhase.defaultMinutes * 60L,
            isRunning = false,
            isPaused = false,
            completedSessionsToday = completedSessions,
            totalFocusMinutesToday = totalMins
        )
        syncForegroundService()
    }

    fun startCustomTimer(hours: Int, minutes: Int, seconds: Int, label: String) {
        if (_activeTimers.value.size >= 5) {
            appContext?.let { android.widget.Toast.makeText(it, "Maximum of 5 active timers allowed", android.widget.Toast.LENGTH_SHORT).show() }
            return
        }
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
        syncForegroundService()
    }

    fun startPreset(preset: TimerPreset) {
        if (_activeTimers.value.size >= 5) {
            appContext?.let { android.widget.Toast.makeText(it, "Maximum of 5 active timers allowed", android.widget.Toast.LENGTH_SHORT).show() }
            return
        }
        val totalMs = preset.totalSeconds * 1000L
        val timer = ActiveTimer(
            title = preset.title,
            totalMillis = totalMs,
            remainingMillis = totalMs,
            isRunning = true
        )
        _activeTimers.value = listOf(timer) + _activeTimers.value
        syncForegroundService()
    }

    fun pauseTimer(id: String) {
        _activeTimers.value = _activeTimers.value.map {
            if (it.id == id) it.copy(isRunning = false, isPaused = true) else it
        }
        syncForegroundService()
    }

    fun resumeTimer(id: String) {
        _activeTimers.value = _activeTimers.value.map {
            if (it.id == id) it.copy(isRunning = true, isPaused = false) else it
        }
        syncForegroundService()
    }

    fun cancelTimer(id: String) {
        _activeTimers.value = _activeTimers.value.filter { it.id != id }
        syncForegroundService()
    }

    fun addOneMinute(id: String) {
        _activeTimers.value = _activeTimers.value.map {
            if (it.id == id) {
                val newRem = it.remainingMillis + 60_000L
                val newTot = it.totalMillis + 60_000L
                it.copy(remainingMillis = newRem, totalMillis = newTot)
            } else it
        }
        syncForegroundService()
    }

    // Pomodoro Controls
    fun startPomodoro() {
        _pomodoroState.value = _pomodoroState.value.copy(isRunning = true, isPaused = false)
        syncForegroundService()
    }

    fun pausePomodoro() {
        _pomodoroState.value = _pomodoroState.value.copy(isRunning = false, isPaused = true)
        syncForegroundService()
    }

    fun resetPomodoro() {
        val cur = _pomodoroState.value
        _pomodoroState.value = cur.copy(
            remainingSeconds = cur.totalSeconds,
            isRunning = false,
            isPaused = false
        )
        syncForegroundService()
    }

    fun skipPomodoroPhase() {
        onPomodoroPhaseFinished(_pomodoroState.value)
    }

    // Sound & Stop Ringing
    fun stopRinging() {
        _ringingTimer.value = null
        stopAlertSound()
        appContext?.let { ctx ->
            NotificationHelper.cancelTimerFinishedNotification(ctx)
        }
    }

    private fun playAlertSoundAndVibrate(context: Context) {
        try {
            stopAlertSound()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(context, uri).apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                play()
            }

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let { v ->
                isVibrating = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 500, 300, 500, 300, 700)
                    v.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 500, 300, 500, 300, 700), -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun stopAlertSound() {
        try {
            ringtone?.stop()
            ringtone = null
        } catch (_: Exception) {}
    }

    private fun syncForegroundService() {
        val ctx = appContext ?: return
        val runningTimers = _activeTimers.value.filter { it.isRunning || it.isPaused }
        val isPomoActive = _pomodoroState.value.isRunning || _pomodoroState.value.isPaused

        if (runningTimers.isNotEmpty() || isPomoActive) {
            val intent = Intent(ctx, TimerService::class.java).apply {
                action = TimerService.ACTION_UPDATE_NOTIFICATION
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        } else if (_ringingTimer.value == null) {
            val intent = Intent(ctx, TimerService::class.java).apply {
                action = TimerService.ACTION_STOP_SERVICE
            }
            ctx.stopService(intent)
        }
    }
}

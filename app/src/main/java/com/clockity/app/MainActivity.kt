package com.clockity.app

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.clockity.app.service.FloatingTimerService
import com.clockity.app.ui.alarm.AlarmScreen
import com.clockity.app.ui.alarm.AlarmViewModel
import com.clockity.app.ui.components.BackupDialog
import com.clockity.app.ui.components.ClockTab
import com.clockity.app.ui.components.OneUIBottomBar
import com.clockity.app.ui.stopwatch.StopwatchScreen
import com.clockity.app.ui.stopwatch.StopwatchViewModel
import com.clockity.app.ui.theme.*
import com.clockity.app.ui.timer.TimerScreen
import com.clockity.app.ui.timer.TimerViewModel
import com.clockity.app.ui.worldclock.WorldClockScreen
import com.clockity.app.ui.worldclock.WorldClockViewModel
import com.clockity.app.utils.TimerManager

class MainActivity : ComponentActivity() {

    private val alarmViewModel: AlarmViewModel by viewModels()
    private val worldClockViewModel: WorldClockViewModel by viewModels()
    private val stopwatchViewModel: StopwatchViewModel by viewModels()
    private val timerViewModel: TimerViewModel by viewModels()

    private var isInPipMode by mutableStateOf(false)
    private var initialTab by mutableStateOf(ClockTab.ALARM)

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_PIP_PLAY_PAUSE) {
                toggleActiveTimerPlayPause()
            }
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Register PiP action broadcast receiver
        val filter = IntentFilter(ACTION_PIP_PLAY_PAUSE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pipReceiver, filter)
        }

        setContent {
            ClockityTheme {
                if (isInPipMode) {
                    PipTimerView(
                        timerViewModel = timerViewModel,
                        stopwatchViewModel = stopwatchViewModel
                    )
                } else {
                    MainAppScreen(
                        alarmViewModel = alarmViewModel,
                        worldClockViewModel = worldClockViewModel,
                        stopwatchViewModel = stopwatchViewModel,
                        timerViewModel = timerViewModel,
                        initialTab = initialTab
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            stopService(Intent(this, FloatingTimerService::class.java))
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        try {
            stopService(Intent(this, FloatingTimerService::class.java))
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val tabIndex = intent?.getIntExtra("open_tab", -1) ?: -1
        if (tabIndex in 0..3) {
            initialTab = ClockTab.values()[tabIndex]
        } else {
            val hasActiveTimers = TimerManager.activeTimers.value.any { it.isRunning || it.isPaused } ||
                    TimerManager.pomodoroState.value.isRunning ||
                    TimerManager.pomodoroState.value.isPaused
            if (hasActiveTimers) {
                initialTab = ClockTab.TIMER
            }
        }
    }

    private fun toggleActiveTimerPlayPause() {
        val runningTimer = TimerManager.activeTimers.value.firstOrNull { it.isRunning }
        val pausedTimer = TimerManager.activeTimers.value.firstOrNull { it.isPaused }
        val pomo = TimerManager.pomodoroState.value
        val isSwRunning = stopwatchViewModel.uiState.value.isRunning

        if (runningTimer != null) {
            TimerManager.pauseTimer(runningTimer.id)
        } else if (pausedTimer != null) {
            TimerManager.resumeTimer(pausedTimer.id)
        } else if (pomo.isRunning) {
            TimerManager.pausePomodoro()
        } else if (pomo.isPaused) {
            TimerManager.startPomodoro()
        } else if (isSwRunning) {
            stopwatchViewModel.pause()
        } else if (stopwatchViewModel.uiState.value.elapsedMillis > 0) {
            stopwatchViewModel.start()
        }
        updatePipParams()
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isTimerRunning = TimerManager.activeTimers.value.any { it.isRunning }
            val isPomoRunning = TimerManager.pomodoroState.value.isRunning
            val isSwRunning = stopwatchViewModel.uiState.value.isRunning
            val isRunning = isTimerRunning || isPomoRunning || isSwRunning

            val intent = Intent(ACTION_PIP_PLAY_PAUSE).apply {
                setPackage(packageName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val icon = Icon.createWithResource(
                this,
                if (isRunning) R.drawable.ic_pip_pause else R.drawable.ic_pip_play
            )
            val title = if (isRunning) "Pause" else "Resume"
            val action = RemoteAction(icon, title, title, pendingIntent)

            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(239, 100))
                .setActions(listOf(action))
                .build()
            setPictureInPictureParams(params)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val runningTimers = TimerManager.activeTimers.value.any { it.isRunning || it.isPaused }
        val isPomoRunning = TimerManager.pomodoroState.value.isRunning || TimerManager.pomodoroState.value.isPaused
        val isStopwatchRunning = stopwatchViewModel.uiState.value.isRunning

        if (runningTimers || isPomoRunning || isStopwatchRunning) {
            if (Settings.canDrawOverlays(this)) {
                // Launch the true 1/3rd sized mini floating pill overlay!
                val serviceIntent = Intent(this, FloatingTimerService::class.java)
                startService(serviceIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                updatePipParams()
                val isRunning = runningTimers || isPomoRunning || isStopwatchRunning
                val intent = Intent(ACTION_PIP_PLAY_PAUSE).apply { setPackage(packageName) }
                val pendingIntent = PendingIntent.getBroadcast(
                    this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val icon = Icon.createWithResource(
                    this,
                    if (isRunning) R.drawable.ic_pip_pause else R.drawable.ic_pip_play
                )
                val title = if (isRunning) "Pause" else "Resume"
                val action = RemoteAction(icon, title, title, pendingIntent)

                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(239, 100))
                    .setActions(listOf(action))
                    .build()
                enterPictureInPictureMode(params)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            updatePipParams()
        } else {
            // User expanded PiP window -> Ensure directly navigating to Timer tab
            val hasActiveTimers = TimerManager.activeTimers.value.any { it.isRunning || it.isPaused } ||
                    TimerManager.pomodoroState.value.isRunning ||
                    TimerManager.pomodoroState.value.isPaused
            if (hasActiveTimers) {
                initialTab = ClockTab.TIMER
            }
        }
    }

    companion object {
        const val ACTION_PIP_PLAY_PAUSE = "com.clockity.app.ACTION_PIP_PLAY_PAUSE"
    }
}

/**
 * Ultra-compact Picture-in-Picture window containing only the tight digital numbers
 * with zero wasted margin or padding.
 */
@Composable
fun PipTimerView(
    timerViewModel: TimerViewModel,
    stopwatchViewModel: StopwatchViewModel
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val swState by stopwatchViewModel.uiState.collectAsState()

    val runningTimer = timerState.activeTimers.firstOrNull { it.isRunning || it.isPaused }
    val pomo = timerState.pomodoroState

    val timeText: String = if (runningTimer != null) {
        runningTimer.formatRemaining()
    } else if (pomo.isRunning || pomo.isPaused) {
        pomo.formatRemainingTime()
    } else if (swState.isRunning || swState.elapsedMillis > 0) {
        val mins = swState.elapsedMillis / 60000
        val secs = (swState.elapsedMillis % 60000) / 1000
        String.format("%02d:%02d", mins, secs)
    } else {
        "00:00"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = timeText,
            color = Color.White,
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = (-1).sp,
            modifier = Modifier.wrapContentSize()
        )
    }
}

@Composable
fun MainAppScreen(
    alarmViewModel: AlarmViewModel,
    worldClockViewModel: WorldClockViewModel,
    stopwatchViewModel: StopwatchViewModel,
    timerViewModel: TimerViewModel,
    initialTab: ClockTab = ClockTab.ALARM
) {
    var currentTab by remember { mutableStateOf(initialTab) }
    var showBackupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialTab) {
        currentTab = initialTab
    }

    Scaffold(
        bottomBar = {
            OneUIBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        },
        containerColor = OneUIBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ClockTab.ALARM -> AlarmScreen(
                    viewModel = alarmViewModel,
                    onOpenBackup = { showBackupDialog = true }
                )
                ClockTab.WORLD_CLOCK -> WorldClockScreen(
                    viewModel = worldClockViewModel,
                    onOpenBackup = { showBackupDialog = true }
                )
                ClockTab.STOPWATCH -> StopwatchScreen(viewModel = stopwatchViewModel)
                ClockTab.TIMER -> TimerScreen(viewModel = timerViewModel)
            }
        }
    }

    if (showBackupDialog) {
        BackupDialog(onDismiss = { showBackupDialog = false })
    }
}

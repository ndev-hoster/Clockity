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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.launch

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
                        stopwatchViewModel = stopwatchViewModel,
                        onTogglePlayPause = { toggleActiveTimerPlayPause() }
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
        val runningTimers = TimerManager.activeTimers.value.any { it.isRunning }
        val isPomoRunning = TimerManager.pomodoroState.value.isRunning
        val isStopwatchRunning = stopwatchViewModel.uiState.value.isRunning

        if (runningTimers || isPomoRunning || isStopwatchRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        }
    }

    companion object {
        const val ACTION_PIP_PLAY_PAUSE = "com.clockity.app.ACTION_PIP_PLAY_PAUSE"
    }
}

@Composable
fun PipTimerView(
    timerViewModel: TimerViewModel,
    stopwatchViewModel: StopwatchViewModel,
    onTogglePlayPause: () -> Unit
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val swState by stopwatchViewModel.uiState.collectAsState()

    val runningTimer = timerState.activeTimers.firstOrNull { it.isRunning || it.isPaused }
    val pomo = timerState.pomodoroState

    val timeText: String
    val isRunning: Boolean
    val progress: Float

    if (runningTimer != null) {
        timeText = runningTimer.formatRemaining()
        isRunning = runningTimer.isRunning
        progress = runningTimer.progress
    } else if (pomo.isRunning || pomo.isPaused) {
        timeText = pomo.formatRemainingTime()
        isRunning = pomo.isRunning
        progress = pomo.progress
    } else if (swState.isRunning || swState.elapsedMillis > 0) {
        val mins = swState.elapsedMillis / 60000
        val secs = (swState.elapsedMillis % 60000) / 1000
        timeText = String.format("%02d:%02d", mins, secs)
        isRunning = swState.isRunning
        progress = ((swState.elapsedMillis % 60000) / 60000f)
    } else {
        timeText = "00:00"
        isRunning = false
        progress = 0f
    }

    // Full bleed pill layout matching Samsung One UI dynamic island pill
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = OneUICardElevated,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, OneUIDivider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Large Digital Time Display
            Text(
                text = timeText,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            // Right: Circular Progress Ring & Interactive Play/Pause Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onTogglePlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = OneUIBlue,
                    strokeWidth = 3.5.dp,
                    trackColor = OneUIDivider
                )
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Resume",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
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

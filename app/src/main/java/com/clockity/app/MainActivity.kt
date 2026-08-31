package com.clockity.app

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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

class MainActivity : ComponentActivity() {

    private val alarmViewModel: AlarmViewModel by viewModels()
    private val worldClockViewModel: WorldClockViewModel by viewModels()
    private val stopwatchViewModel: StopwatchViewModel by viewModels()
    private val timerViewModel: TimerViewModel by viewModels()

    private var isInPipMode by mutableStateOf(false)
    private var initialTab by mutableStateOf(ClockTab.ALARM)

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

        setContent {
            ClockityTheme {
                if (isInPipMode) {
                    PipTimerView(timerViewModel, stopwatchViewModel)
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val runningTimers = TimerManager.activeTimers.value.any { it.isRunning }
        val isPomoRunning = TimerManager.pomodoroState.value.isRunning
        val isStopwatchRunning = stopwatchViewModel.uiState.value.isRunning

        if (runningTimers || isPomoRunning || isStopwatchRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(239, 100))
                    .build()
                enterPictureInPictureMode(params)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }
}

@Composable
fun PipTimerView(
    timerViewModel: TimerViewModel,
    stopwatchViewModel: StopwatchViewModel
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val swState by stopwatchViewModel.uiState.collectAsState()

    val runningTimer = timerState.activeTimers.firstOrNull { it.isRunning || it.isPaused }
    val pomo = timerState.pomodoroState

    val timeText: String
    val isRunning: Boolean
    val progress: Float
    val onTogglePlayPause: () -> Unit

    if (runningTimer != null) {
        timeText = runningTimer.formatRemaining()
        isRunning = runningTimer.isRunning
        progress = runningTimer.progress
        onTogglePlayPause = {
            if (runningTimer.isRunning) timerViewModel.pauseTimer(runningTimer.id)
            else timerViewModel.resumeTimer(runningTimer.id)
        }
    } else if (pomo.isRunning || pomo.isPaused) {
        timeText = pomo.formatRemainingTime()
        isRunning = pomo.isRunning
        progress = pomo.progress
        onTogglePlayPause = {
            if (pomo.isRunning) timerViewModel.pausePomodoro()
            else timerViewModel.startPomodoro()
        }
    } else if (swState.isRunning || swState.elapsedMillis > 0) {
        val mins = swState.elapsedMillis / 60000
        val secs = (swState.elapsedMillis % 60000) / 1000
        timeText = String.format("%02d:%02d", mins, secs)
        isRunning = swState.isRunning
        progress = ((swState.elapsedMillis % 60000) / 60000f)
        onTogglePlayPause = {
            if (swState.isRunning) stopwatchViewModel.pause()
            else stopwatchViewModel.start()
        }
    } else {
        timeText = "00:00"
        isRunning = false
        progress = 0f
        onTogglePlayPause = {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUIBlack),
        contentAlignment = Alignment.Center
    ) {
        // Floating Pill Container
        Row(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .clip(CircleShape)
                .background(OneUICardElevated)
                .border(BorderStroke(1.dp, OneUIDivider), CircleShape)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Large Digital Time Display
            Text(
                text = timeText,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            // Right: Circular Progress Ring & Interactive Play/Pause Button
            Box(
                modifier = Modifier
                    .size(38.dp)
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
                    modifier = Modifier.size(18.dp)
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
                .background(OneUIBlack)
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

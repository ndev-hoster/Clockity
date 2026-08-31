package com.clockity.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

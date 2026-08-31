package com.clockity.app.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.data.models.PomodoroPhase
import com.clockity.app.data.models.TimerPreset
import com.clockity.app.ui.components.OneUIHeader
import com.clockity.app.ui.components.TimerWheelDurationPicker
import com.clockity.app.ui.theme.*

@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Standard Timers, 1 = Pomodoro Focus

    var inputHours by remember { mutableIntStateOf(0) }
    var inputMinutes by remember { mutableIntStateOf(10) }
    var inputSeconds by remember { mutableIntStateOf(0) }

    var showAddPresetDialog by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<TimerPreset?>(null) }

    val activeCount = uiState.activeTimers.count { it.isRunning }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OneUIBlack)
    ) {
        // One UI Header
        OneUIHeader(
            title = if (selectedTab == 0) "Timer" else "Focus",
            subtitle = if (selectedTab == 0) {
                if (activeCount > 0) "$activeCount timer${if (activeCount > 1) "s" else ""} running" else "Set duration"
            } else {
                "${uiState.pomodoroState.completedSessionsToday} sessions completed today"
            }
        )

        // Ringing Alert Banner if any timer reached zero
        AnimatedVisibility(visible = uiState.ringingTimer != null) {
            uiState.ringingTimer?.let { finishedTimer ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = OneUIRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, OneUIRed)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Timer Finished!",
                                color = OneUIRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${finishedTimer.title} reached 00:00",
                                color = OneUITextPrimary,
                                fontSize = 13.sp
                            )
                        }
                        Button(
                            onClick = { viewModel.stopRinging() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OneUIRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Stop", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Segmented Switcher (Timers | Focus)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(OneUICardDark)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == 0) OneUIBlue else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Timers",
                    color = if (selectedTab == 0) OneUIBlack else OneUITextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == 1) OneUIBlue else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Focus Mode",
                    color = if (selectedTab == 1) OneUIBlack else OneUITextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        if (selectedTab == 0) {
            // STANDARD TIMERS VIEW
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 96.dp, top = 4.dp)
            ) {
                // 1. Quick Presets Row (FIRST)
                item {
                    PresetChips(
                        presets = uiState.presets,
                        onSelectPreset = { viewModel.startPreset(it) },
                        onEditPreset = { editingPreset = it },
                        onAddPreset = { showAddPresetDialog = true }
                    )
                }

                // 2. Active Running Timers Section (BETWEEN Presets & New Timer)
                if (uiState.activeTimers.isNotEmpty()) {
                    item {
                        Text(
                            text = "Running Timers (${uiState.activeTimers.size}/5)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OneUITextSecondary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                        )
                    }

                    item {
                        val timerCount = uiState.activeTimers.size
                        if (timerCount == 1) {
                            val timer = uiState.activeTimers.first()
                            MultiTimerCard(
                                timer = timer,
                                onPause = { viewModel.pauseTimer(timer.id) },
                                onResume = { viewModel.resumeTimer(timer.id) },
                                onCancel = { viewModel.cancelTimer(timer.id) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (timerCount == 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                uiState.activeTimers.forEach { timer ->
                                    MultiTimerCard(
                                        timer = timer,
                                        onPause = { viewModel.pauseTimer(timer.id) },
                                        onResume = { viewModel.resumeTimer(timer.id) },
                                        onCancel = { viewModel.cancelTimer(timer.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.activeTimers, key = { it.id }) { timer ->
                                    MultiTimerCard(
                                        timer = timer,
                                        onPause = { viewModel.pauseTimer(timer.id) },
                                        onResume = { viewModel.resumeTimer(timer.id) },
                                        onCancel = { viewModel.cancelTimer(timer.id) },
                                        modifier = Modifier.width(185.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Custom Timer Picker Card (NEW TIMER)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(OneUICardDark)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimerWheelDurationPicker(
                            hours = inputHours,
                            minutes = inputMinutes,
                            seconds = inputSeconds,
                            onDurationChange = { h, m, s ->
                                inputHours = h
                                inputMinutes = m
                                inputSeconds = s
                            }
                        )

                        // Quick duration bumps (+1m, +5m, +15m, +30m, Clear)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1 to "+1m", 5 to "+5m", 15 to "+15m", 30 to "+30m").forEach { (m, label) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(OneUICardElevated)
                                        .clickable {
                                            val totalMins = inputHours * 60 + inputMinutes + m
                                            inputHours = (totalMins / 60).coerceIn(0, 23)
                                            inputMinutes = (totalMins % 60).coerceIn(0, 59)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = OneUIBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(OneUICardElevated)
                                    .clickable {
                                        inputHours = 0
                                        inputMinutes = 0
                                        inputSeconds = 0
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Clear",
                                    color = OneUIRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.startCustomTimer(inputHours, inputMinutes, inputSeconds, "Timer")
                            },
                            enabled = (inputHours > 0 || inputMinutes > 0 || inputSeconds > 0) && uiState.activeTimers.size < 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OneUIBlue,
                                contentColor = OneUIBlack
                            )
                        ) {
                            Text(
                                text = if (uiState.activeTimers.size >= 5) "Max Timers Reached (5/5)" else "Start Timer",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // POMODORO FOCUS VIEW
            val pomo = uiState.pomodoroState
            val phaseColor = when (pomo.phase) {
                PomodoroPhase.FOCUS -> OneUIBlue
                PomodoroPhase.SHORT_BREAK -> OneUIYellow
                PomodoroPhase.LONG_BREAK -> Color(0xFF64B5F6)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp)
            ) {
                item {
                    // Central Circular Dial Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp)),
                        color = OneUICardDark,
                        border = BorderStroke(1.dp, OneUIDivider)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Cycle Indicators (● ● ○ ○)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 20.dp)
                            ) {
                                for (i in 1..pomo.totalCycles) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (i < pomo.currentCycle) phaseColor
                                                else if (i == pomo.currentCycle) phaseColor
                                                else OneUIDivider
                                            )
                                    )
                                }
                            }

                            // Circular Progress Ring
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(240.dp)
                                    .padding(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { pomo.progress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = phaseColor,
                                    strokeWidth = 10.dp,
                                    trackColor = OneUICardElevated,
                                    strokeCap = StrokeCap.Round
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = pomo.phase.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = phaseColor
                                    )
                                    Text(
                                        text = pomo.formatRemainingTime(),
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = OneUITextPrimary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Cycle ${pomo.currentCycle} of ${pomo.totalCycles}",
                                        fontSize = 13.sp,
                                        color = OneUITextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Control Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resetPomodoro() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, OneUIDivider),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OneUITextSecondary)
                                ) {
                                    Text("Reset", fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        if (pomo.isRunning) viewModel.pausePomodoro() else viewModel.startPomodoro()
                                    },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = if (pomo.isRunning) BorderStroke(1.dp, OneUIYellowPauseBorder) else BorderStroke(1.dp, OneUIBlueResumeBorder),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (pomo.isRunning) OneUIYellowPauseBg else OneUIBlueResumeBg,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = if (pomo.isRunning) "Pause" else if (pomo.isPaused) "Resume" else "Start",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.skipPomodoroPhase() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, OneUIDivider),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OneUIBlue)
                                ) {
                                    Text("Skip", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Daily Focus Statistics Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp)),
                        color = OneUICardDark,
                        border = BorderStroke(1.dp, OneUIDivider)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Today's Focus Summary",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUITextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${pomo.completedSessionsToday}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OneUIBlue
                                    )
                                    Text(
                                        text = "Sessions Done",
                                        fontSize = 12.sp,
                                        color = OneUITextSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${pomo.totalFocusMinutesToday}m",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OneUIYellow
                                    )
                                    Text(
                                        text = "Total Focus Time",
                                        fontSize = 12.sp,
                                        color = OneUITextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPresetDialog) {
        AddPresetDialog(
            onDismiss = { showAddPresetDialog = false },
            onSave = { title, secs ->
                viewModel.addPreset(title, secs)
            }
        )
    }

    editingPreset?.let { preset ->
        EditPresetDialog(
            preset = preset,
            onDismiss = { editingPreset = null },
            onSave = { updated ->
                viewModel.updatePreset(updated)
                editingPreset = null
            },
            onDelete = { toDelete ->
                viewModel.deletePreset(toDelete)
                editingPreset = null
            }
        )
    }
}

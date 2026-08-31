package com.clockity.app.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var inputHours by remember { mutableIntStateOf(0) }
    var inputMinutes by remember { mutableIntStateOf(10) }
    var inputSeconds by remember { mutableIntStateOf(0) }
    var timerLabel by remember { mutableStateOf("") }

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
            title = "Timer",
            subtitle = if (activeCount > 0) "$activeCount timer${if (activeCount > 1) "s" else ""} running" else "Set duration"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 96.dp, top = 4.dp)
        ) {
            // Quick Presets Row with Edit/Delete support
            item {
                PresetChips(
                    presets = uiState.presets,
                    onSelectPreset = { viewModel.startPreset(it) },
                    onEditPreset = { editingPreset = it },
                    onAddPreset = { showAddPresetDialog = true }
                )
            }

            // Custom Timer Picker Card with Scrollable Wheels & Direct Typing
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(OneUICardDark)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Scrollable Duration Wheels with Haptics and Tap-to-Type Keyboard
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

                    // Quick duration bumps (+1m, +5m, +15m, Clear)
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

                    OutlinedTextField(
                        value = timerLabel,
                        onValueChange = { timerLabel = it },
                        placeholder = { Text("Timer Name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OneUITextPrimary,
                            unfocusedTextColor = OneUITextPrimary,
                            focusedBorderColor = OneUIBlue,
                            unfocusedBorderColor = OneUIDivider
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.startCustomTimer(inputHours, inputMinutes, inputSeconds, timerLabel)
                            timerLabel = ""
                        },
                        enabled = (inputHours > 0 || inputMinutes > 0 || inputSeconds > 0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OneUIBlue,
                            contentColor = OneUIBlack
                        )
                    ) {
                        Text("Start Timer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Active Running Timers Section
            if (uiState.activeTimers.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Timers",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OneUITextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                items(uiState.activeTimers, key = { it.id }) { timer ->
                    MultiTimerCard(
                        timer = timer,
                        onPause = { viewModel.pauseTimer(timer.id) },
                        onResume = { viewModel.resumeTimer(timer.id) },
                        onCancel = { viewModel.cancelTimer(timer.id) },
                        onAddMinute = { viewModel.addOneMinute(timer.id) }
                    )
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


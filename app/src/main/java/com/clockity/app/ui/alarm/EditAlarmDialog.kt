package com.clockity.app.ui.alarm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clockity.app.data.models.Alarm
import com.clockity.app.data.models.AlarmGroup
import com.clockity.app.ui.components.AlarmWheelTimePicker
import com.clockity.app.ui.components.OneUISwitch
import com.clockity.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmDialog(
    alarm: Alarm?,
    initialGroupId: Long? = null,
    groups: List<AlarmGroup>,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: ((Alarm) -> Unit)? = null
) {
    val isNew = alarm == null
    val currentCal = remember { Calendar.getInstance() }
    var selectedHour by remember { mutableIntStateOf(alarm?.hour ?: currentCal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(alarm?.minute ?: currentCal.get(Calendar.MINUTE)) }

    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var daysOfWeek by remember { mutableStateOf(alarm?.daysOfWeek ?: 0) }
    var specificDateMillis by remember { mutableStateOf(alarm?.specificDateMillis) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    var selectedGroupId by remember { mutableStateOf(alarm?.groupId ?: initialGroupId) }
    var isGentleWake by remember { mutableStateOf(alarm?.isGentleWakeUp ?: true) }
    var snoozeMinutes by remember { mutableStateOf(alarm?.snoozeDurationMinutes ?: 5) }
    var snoozeRepeatCount by remember { mutableIntStateOf(alarm?.snoozeRepeatCount ?: 3) }
    var showCustomRepeatDialog by remember { mutableStateOf(false) }
    var customRepeatInput by remember { mutableStateOf(snoozeRepeatCount.toString()) }
    var vibrationPattern by remember { mutableStateOf(alarm?.vibrationPattern ?: "Basic") }

    val daysLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = OneUIBlack,
            border = BorderStroke(1.dp, OneUIDivider)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNew) "Add Alarm" else "Edit Alarm",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextPrimary
                    )
                    if (!isNew && onDelete != null && alarm != null) {
                        IconButton(onClick = {
                            onDelete(alarm)
                            onDismiss()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Alarm",
                                tint = OneUIRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Scrollable Number Wheel Picker with Haptics and Tap-to-Type Keyboard
                    AlarmWheelTimePicker(
                        hour = selectedHour,
                        minute = selectedMinute,
                        onTimeChange = { h, m ->
                            selectedHour = h
                            selectedMinute = m
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Day Selection Pills + Specific Date Picker Icon
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Repeat or Specific Date",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OneUITextSecondary
                            )
                            if (specificDateMillis != null) {
                                Text(
                                    text = "Date Mode",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUIYellow
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Weekday pills (M..S)
                            for (i in 1..7) {
                                val isSelected = (daysOfWeek and (1 shl i)) != 0 && specificDateMillis == null
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) OneUIBlue else OneUICardElevated)
                                        .clickable {
                                            specificDateMillis = null
                                            daysOfWeek = daysOfWeek xor (1 shl i)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = daysLabels[i - 1],
                                        color = if (isSelected) OneUIBlack else OneUITextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Date Picker Calendar Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (specificDateMillis != null) OneUIYellow else OneUICardElevated)
                                    .clickable { showDatePickerDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Pick specific date",
                                    tint = if (specificDateMillis != null) OneUIBlack else OneUITextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Display selected date chip if active
                        if (specificDateMillis != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val formattedDate = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(specificDateMillis!!))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(OneUICardElevated)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = OneUIYellow,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Alarm set for $formattedDate",
                                    fontSize = 12.sp,
                                    color = OneUIYellow,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear date",
                                    tint = OneUITextSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { specificDateMillis = null }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Alarm Label (Underlined without box)
                    TextField(
                        value = label,
                        onValueChange = { label = it },
                        placeholder = { Text("Alarm Name", color = OneUITextTertiary, fontSize = 16.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = OneUITextPrimary,
                            unfocusedTextColor = OneUITextPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = OneUIBlue,
                            unfocusedIndicatorColor = OneUIDivider
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Group Assignment Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Alarm Group",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OneUITextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedGroupId == null,
                                onClick = { selectedGroupId = null },
                                label = { Text("No Group", maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OneUIBlue,
                                    selectedLabelColor = OneUIBlack,
                                    containerColor = OneUICardElevated,
                                    labelColor = OneUITextPrimary
                                )
                            )
                            groups.forEach { group ->
                                FilterChip(
                                    selected = selectedGroupId == group.id,
                                    onClick = { selectedGroupId = group.id },
                                    label = { Text(group.name, maxLines = 1, softWrap = false) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OneUIBlue,
                                        selectedLabelColor = OneUIBlack,
                                        containerColor = OneUICardElevated,
                                        labelColor = OneUITextPrimary
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gentle Wake-up Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(OneUICardElevated)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Gentle wake-up",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OneUITextPrimary
                            )
                            Text(
                                text = "Gradually increases volume over 1 minute",
                                fontSize = 12.sp,
                                color = OneUITextSecondary
                            )
                        }
                        OneUISwitch(
                            checked = isGentleWake,
                            onCheckedChange = { isGentleWake = it },
                            checkedTrackColor = OneUIBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Snooze Duration (with Disable / "Off" option)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(OneUICardElevated)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Snooze",
                            fontSize = 15.sp,
                            color = OneUITextPrimary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(0 to "Off", 5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m").forEach { (mins, text) ->
                                val isSelected = snoozeMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) OneUIBlue else OneUIDivider)
                                        .clickable { snoozeMinutes = mins }
                                        .padding(horizontal = 9.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = text,
                                        color = if (isSelected) OneUIBlack else OneUITextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Snooze Repeat Count [1, 5, 10, never, custom]
                    if (snoozeMinutes > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(OneUICardElevated)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Repeat Count",
                                fontSize = 15.sp,
                                color = OneUITextPrimary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val standardOptions = listOf(1 to "1x", 5 to "5x", 10 to "10x", 0 to "Never")
                                val isStandard = standardOptions.any { it.first == snoozeRepeatCount }

                                standardOptions.forEach { (count, text) ->
                                    val isSelected = snoozeRepeatCount == count
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) OneUIBlue else OneUIDivider)
                                            .clickable { snoozeRepeatCount = count }
                                            .padding(horizontal = 9.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = text,
                                            color = if (isSelected) OneUIBlack else OneUITextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                // Custom option chip
                                val isCustom = !isStandard && snoozeRepeatCount > 0
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCustom) OneUIBlue else OneUIDivider)
                                        .clickable {
                                            customRepeatInput = if (snoozeRepeatCount > 0) snoozeRepeatCount.toString() else "3"
                                            showCustomRepeatDialog = true
                                        }
                                        .padding(horizontal = 9.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isCustom) "${snoozeRepeatCount}x" else "Custom",
                                        color = if (isCustom) OneUIBlack else OneUITextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Vibration Pattern
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(OneUICardElevated)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vibration",
                            fontSize = 15.sp,
                            color = OneUITextPrimary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Basic", "Heartbeat", "Tick-tock", "Rapid").forEach { pattern ->
                                val isSelected = vibrationPattern == pattern
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) OneUIBlue else OneUIDivider)
                                        .clickable { vibrationPattern = pattern }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pattern,
                                        color = if (isSelected) OneUIBlack else OneUITextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OneUITextSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val savedAlarm = (alarm ?: Alarm(hour = selectedHour, minute = selectedMinute)).copy(
                                hour = selectedHour,
                                minute = selectedMinute,
                                label = label,
                                daysOfWeek = if (specificDateMillis != null) 0 else daysOfWeek,
                                specificDateMillis = specificDateMillis,
                                groupId = selectedGroupId,
                                isGentleWakeUp = isGentleWake,
                                snoozeDurationMinutes = snoozeMinutes,
                                snoozeRepeatCount = snoozeRepeatCount,
                                vibrationPattern = vibrationPattern,
                                isEnabled = true
                            )
                            onSave(savedAlarm)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OneUIBlue,
                            contentColor = OneUIBlack
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Material 3 DatePickerDialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = specificDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        specificDateMillis = datePickerState.selectedDateMillis
                        daysOfWeek = 0 // Specific date takes priority over repeating days
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Select", color = OneUIBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel", color = OneUITextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = OneUIBlack
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = OneUIBlack,
                    titleContentColor = OneUITextPrimary,
                    headlineContentColor = OneUITextPrimary,
                    weekdayContentColor = OneUITextSecondary,
                    subheadContentColor = OneUITextSecondary,
                    yearContentColor = OneUITextPrimary,
                    currentYearContentColor = OneUIBlue,
                    selectedYearContentColor = OneUIBlack,
                    selectedYearContainerColor = OneUIBlue,
                    dayContentColor = OneUITextPrimary,
                    selectedDayContainerColor = OneUIBlue,
                    selectedDayContentColor = OneUIBlack,
                    todayContentColor = OneUIYellow,
                    todayDateBorderColor = OneUIYellow
                )
            )
        }
    }

    // Custom Snooze Repeat Count Dialog
    if (showCustomRepeatDialog) {
        AlertDialog(
            onDismissRequest = { showCustomRepeatDialog = false },
            title = {
                Text(
                    text = "Custom Snooze Repeat",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter how many times the alarm can be snoozed before stopping:",
                        fontSize = 13.sp,
                        color = OneUITextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = customRepeatInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 3) {
                                customRepeatInput = input
                            }
                        },
                        singleLine = true,
                        placeholder = { Text("e.g. 5", color = OneUITextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OneUIBlue,
                            unfocusedBorderColor = OneUIDivider,
                            focusedTextColor = OneUITextPrimary,
                            unfocusedTextColor = OneUITextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = customRepeatInput.toIntOrNull() ?: 3
                        snoozeRepeatCount = if (parsed < 1) 1 else parsed
                        showCustomRepeatDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OneUIBlue,
                        contentColor = OneUIBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRepeatDialog = false }) {
                    Text("Cancel", color = OneUITextSecondary)
                }
            },
            containerColor = OneUICardElevated,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

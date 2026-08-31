package com.clockity.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.ui.theme.*

/**
 * Reusable vertical scrollable drum number wheel with haptic tick feedback.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollableNumberWheel(
    items: List<String>,
    selectedIndex: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 44.dp,
    visibleItemsCount: Int = 3,
    onClick: (() -> Unit)? = null
) {
    val initialIdx = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIdx)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val context = LocalContext.current
    val view = LocalView.current

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // Detect center item
    val currentIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) selectedIndex
            else {
                val centerOffset = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closest = visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - centerOffset)
                }
                closest?.index ?: selectedIndex
            }
        }
    }

    // Track programmatic scrolling to prevent re-entrant callbacks during animation
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // Handle user scrolling change & haptics
    var lastHapticIndex by remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(currentIndex) {
        if (currentIndex in items.indices) {
            if (listState.isScrollInProgress && !isProgrammaticScroll) {
                onValueChange(currentIndex)
            }
            if (currentIndex != lastHapticIndex) {
                lastHapticIndex = currentIndex
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                } catch (_: Exception) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
        }
    }

    // Smoothly animate to target when external selectedIndex changes (e.g. Clear button scrolling backwards to 0)
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices && selectedIndex != currentIndex && !listState.isScrollInProgress) {
            isProgrammaticScroll = true
            try {
                listState.animateScrollToItem(selectedIndex)
            } finally {
                isProgrammaticScroll = false
            }
            lastHapticIndex = selectedIndex
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // Center Selected Highlight Bar
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(OneUIBlue.copy(alpha = 0.12f))
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemsCount / 2)),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val isSelected = index == currentIndex
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        fontSize = if (isSelected) 30.sp else 20.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) OneUITextPrimary else OneUITextTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Alarm Wheel & Direct Typing Time Picker
 */
@Composable
fun AlarmWheelTimePicker(
    hour: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    is24Hour: Boolean = false
) {
    var isKeyboardMode by remember { mutableStateOf(false) }

    // 12-hour or 24-hour components
    val isPm = hour >= 12
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }

    val hoursList = if (is24Hour) (0..23).map { String.format("%02d", it) } else (1..12).map { String.format("%02d", it) }
    val minutesList = (0..59).map { String.format("%02d", it) }
    val amPmList = listOf("AM", "PM")

    val selectedHourIndex = if (is24Hour) hour.coerceIn(0, 23) else (hour12 - 1).coerceIn(0, 11)
    val selectedMinuteIndex = minute.coerceIn(0, 59)
    val selectedAmPmIndex = if (isPm) 1 else 0

    fun updateTimeFromWheel(hIdx: Int, mIdx: Int, amPmIdx: Int) {
        val finalH = if (is24Hour) {
            hIdx.coerceIn(0, 23)
        } else {
            val hVal = hIdx + 1
            if (amPmIdx == 1) { // PM
                if (hVal == 12) 12 else hVal + 12
            } else { // AM
                if (hVal == 12) 0 else hVal
            }
        }
        val finalM = mIdx.coerceIn(0, 59)
        onTimeChange(finalH, finalM)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(OneUICardElevated)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with Mode Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isKeyboardMode) "Type Time" else "Scroll Time (Tap to Type)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OneUITextSecondary
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(OneUICardDark)
                    .clickable { isKeyboardMode = !isKeyboardMode }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isKeyboardMode) Icons.Default.UnfoldMore else Icons.Default.Keyboard,
                    contentDescription = "Toggle Mode",
                    tint = OneUIBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isKeyboardMode) "Wheel" else "Keypad",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUIBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AnimatedContent(
            targetState = isKeyboardMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "picker_mode"
        ) { keyboardMode ->
            if (keyboardMode) {
                // Direct Numeric Typing Mode
                DirectTimeInputRow(
                    hour = hour,
                    minute = minute,
                    is24Hour = is24Hour,
                    onTimeChange = onTimeChange
                )
            } else {
                // Scrollable Drum Wheels with Haptic Feedback
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours Wheel
                    ScrollableNumberWheel(
                        items = hoursList,
                        selectedIndex = selectedHourIndex,
                        onValueChange = { newH ->
                            updateTimeFromWheel(newH, selectedMinuteIndex, selectedAmPmIndex)
                        },
                        onClick = { isKeyboardMode = true },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = ":",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Minutes Wheel
                    ScrollableNumberWheel(
                        items = minutesList,
                        selectedIndex = selectedMinuteIndex,
                        onValueChange = { newM ->
                            updateTimeFromWheel(selectedHourIndex, newM, selectedAmPmIndex)
                        },
                        onClick = { isKeyboardMode = true },
                        modifier = Modifier.weight(1f)
                    )

                    if (!is24Hour) {
                        Spacer(modifier = Modifier.width(8.dp))
                        // AM/PM Wheel
                        ScrollableNumberWheel(
                            items = amPmList,
                            selectedIndex = selectedAmPmIndex,
                            onValueChange = { newAmPm ->
                                updateTimeFromWheel(selectedHourIndex, selectedMinuteIndex, newAmPm)
                            },
                            onClick = { isKeyboardMode = true },
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Direct Text Input Row for Alarm
 */
@Composable
fun DirectTimeInputRow(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    onTimeChange: (hour: Int, minute: Int) -> Unit
) {
    val isPm = hour >= 12
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }

    var hourText by remember(hour) {
        mutableStateOf(if (is24Hour) String.format("%02d", hour) else String.format("%02d", hour12))
    }
    var minuteText by remember(minute) {
        mutableStateOf(String.format("%02d", minute))
    }
    var amPmState by remember(hour) {
        mutableStateOf(if (isPm) "PM" else "AM")
    }

    val focusManager = LocalFocusManager.current

    fun applyDirectTime() {
        val hParsed = hourText.filter { it.isDigit() }.toIntOrNull() ?: 0
        val mParsed = minuteText.filter { it.isDigit() }.toIntOrNull() ?: 0

        val finalH = if (is24Hour) {
            hParsed.coerceIn(0, 23)
        } else {
            val hCapped = hParsed.coerceIn(1, 12)
            if (amPmState == "PM") {
                if (hCapped == 12) 12 else hCapped + 12
            } else {
                if (hCapped == 12) 0 else hCapped
            }
        }
        val finalM = mParsed.coerceIn(0, 59)
        onTimeChange(finalH, finalM)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hours Input
        OutlinedTextField(
            value = hourText,
            onValueChange = {
                val clean = it.filter { ch -> ch.isDigit() }.take(2)
                hourText = clean
                applyDirectTime()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = OneUITextPrimary,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.width(76.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OneUIBlue,
                unfocusedBorderColor = OneUIDivider,
                focusedContainerColor = OneUICardDark,
                unfocusedContainerColor = OneUICardDark
            )
        )

        Text(
            text = ":",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = OneUITextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Minutes Input
        OutlinedTextField(
            value = minuteText,
            onValueChange = {
                val clean = it.filter { ch -> ch.isDigit() }.take(2)
                minuteText = clean
                applyDirectTime()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = OneUITextPrimary,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.width(76.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OneUIBlue,
                unfocusedBorderColor = OneUIDivider,
                focusedContainerColor = OneUICardDark,
                unfocusedContainerColor = OneUICardDark
            )
        )

        if (!is24Hour) {
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("AM", "PM").forEach { period ->
                    val isSelected = amPmState == period
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) OneUIBlue else OneUICardDark)
                            .clickable {
                                amPmState = period
                                applyDirectTime()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period,
                            color = if (isSelected) OneUIBlack else OneUITextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Timer Wheel & Direct Typing Duration Picker
 */
@Composable
fun TimerWheelDurationPicker(
    hours: Int,
    minutes: Int,
    seconds: Int,
    onDurationChange: (h: Int, m: Int, s: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isKeyboardMode by remember { mutableStateOf(false) }

    val hoursList = (0..23).map { String.format("%02d", it) }
    val minutesList = (0..59).map { String.format("%02d", it) }
    val secondsList = (0..59).map { String.format("%02d", it) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(OneUICardElevated)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isKeyboardMode) "Type Duration" else "Scroll Duration (Tap to Type)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OneUITextSecondary
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(OneUICardDark)
                    .clickable { isKeyboardMode = !isKeyboardMode }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isKeyboardMode) Icons.Default.UnfoldMore else Icons.Default.Keyboard,
                    contentDescription = "Toggle Mode",
                    tint = OneUIBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isKeyboardMode) "Wheel" else "Keypad",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUIBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AnimatedContent(
            targetState = isKeyboardMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "timer_picker_mode"
        ) { keyboardMode ->
            if (keyboardMode) {
                // Direct Text Typing for Timer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = if (hours == 0) "00" else String.format("%02d", hours),
                            onValueChange = { str ->
                                val v = str.filter { it.isDigit() }.toIntOrNull() ?: 0
                                onDurationChange(v.coerceIn(0, 23), minutes, seconds)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUITextPrimary,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.width(72.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OneUIBlue,
                                unfocusedBorderColor = OneUIDivider,
                                focusedContainerColor = OneUICardDark,
                                unfocusedContainerColor = OneUICardDark
                            )
                        )
                        Text("hours", fontSize = 11.sp, color = OneUITextSecondary, modifier = Modifier.padding(top = 2.dp))
                    }

                    Text(
                        text = ":",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextSecondary,
                        modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 12.dp)
                    )

                    // Minutes
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = if (minutes == 0) "00" else String.format("%02d", minutes),
                            onValueChange = { str ->
                                val v = str.filter { it.isDigit() }.toIntOrNull() ?: 0
                                onDurationChange(hours, v.coerceIn(0, 59), seconds)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUITextPrimary,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.width(72.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OneUIBlue,
                                unfocusedBorderColor = OneUIDivider,
                                focusedContainerColor = OneUICardDark,
                                unfocusedContainerColor = OneUICardDark
                            )
                        )
                        Text("mins", fontSize = 11.sp, color = OneUITextSecondary, modifier = Modifier.padding(top = 2.dp))
                    }

                    Text(
                        text = ":",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextSecondary,
                        modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 12.dp)
                    )

                    // Seconds
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = if (seconds == 0) "00" else String.format("%02d", seconds),
                            onValueChange = { str ->
                                val v = str.filter { it.isDigit() }.toIntOrNull() ?: 0
                                onDurationChange(hours, minutes, v.coerceIn(0, 59))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUITextPrimary,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.width(72.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OneUIBlue,
                                unfocusedBorderColor = OneUIDivider,
                                focusedContainerColor = OneUICardDark,
                                unfocusedContainerColor = OneUICardDark
                            )
                        )
                        Text("secs", fontSize = 11.sp, color = OneUITextSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            } else {
                // Scrollable Drum Wheels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        ScrollableNumberWheel(
                            items = hoursList,
                            selectedIndex = hours.coerceIn(0, 23),
                            onValueChange = { newH ->
                                onDurationChange(newH, minutes, seconds)
                            },
                            onClick = { isKeyboardMode = true }
                        )
                        Text("hours", fontSize = 11.sp, color = OneUITextSecondary, modifier = Modifier.padding(top = 2.dp))
                    }

                    Text(
                        text = ":",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextSecondary,
                        modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 12.dp)
                    )

                    // Minutes
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        ScrollableNumberWheel(
                            items = minutesList,
                            selectedIndex = minutes.coerceIn(0, 59),
                            onValueChange = { newM ->
                                onDurationChange(hours, newM, seconds)
                            },
                            onClick = { isKeyboardMode = true }
                        )
                        Text("mins", fontSize = 11.sp, color = OneUITextSecondary, modifier = Modifier.padding(top = 2.dp))
                    }

                    Text(
                        text = ":",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextSecondary,
                        modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 12.dp)
                    )

                    // Seconds
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        ScrollableNumberWheel(
                            items = secondsList,
                            selectedIndex = seconds.coerceIn(0, 59),
                            onValueChange = { newS ->
                                onDurationChange(hours, minutes, newS)
                            },
                            onClick = { isKeyboardMode = true }
                        )
                        Text("secs", fontSize = 11.sp, color = OneUITextSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

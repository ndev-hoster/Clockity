@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Reusable vertical scrollable drum number wheel with haptic tick feedback,
 * sequence numbers on top & bottom, and infinite wrap-around looping.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollableNumberWheel(
    items: List<String>,
    selectedIndex: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 38.dp,
    visibleItemsCount: Int = 5,
    isLooping: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val count = items.size.coerceAtLeast(1)
    val loopMultiplier = if (isLooping && count > 1) 1000 else 1
    val totalVirtualItems = count * loopMultiplier
    val middleBase = if (isLooping && count > 1) (loopMultiplier / 2) * count else 0

    val initialVirtualIdx = (middleBase + (selectedIndex % count)).coerceIn(0, totalVirtualItems - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialVirtualIdx)
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

    // Detect center virtual item
    val currentVirtualIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) initialVirtualIdx
            else {
                val centerOffset = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closest = visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - centerOffset)
                }
                closest?.index ?: initialVirtualIdx
            }
        }
    }

    val currentActualIndex by remember {
        derivedStateOf { (currentVirtualIndex % count).coerceIn(0, count - 1) }
    }

    // Track programmatic scrolling
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    var lastHapticIndex by remember { mutableIntStateOf(selectedIndex % count) }

    // Handle user scroll updates & haptic ticks
    LaunchedEffect(currentActualIndex) {
        if (listState.isScrollInProgress && !isProgrammaticScroll) {
            onValueChange(currentActualIndex)
        }
        if (currentActualIndex != lastHapticIndex) {
            lastHapticIndex = currentActualIndex
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

    // Smoothly animate to target when external selectedIndex changes (e.g. Clear button)
    LaunchedEffect(selectedIndex) {
        val targetActual = selectedIndex % count
        if (targetActual != currentActualIndex && !listState.isScrollInProgress) {
            isProgrammaticScroll = true
            try {
                val diff = targetActual - currentActualIndex
                val targetVirtual = (currentVirtualIndex + diff).coerceIn(0, totalVirtualItems - 1)
                listState.animateScrollToItem(targetVirtual)
            } finally {
                isProgrammaticScroll = false
            }
            lastHapticIndex = targetActual
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemsCount / 2)),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(totalVirtualItems) { virtualIdx ->
                val actualIdx = virtualIdx % count
                val distance = kotlin.math.abs(virtualIdx - currentVirtualIndex)

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onClick?.invoke()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val (fontSize, textColor, fontWeight) = when (distance) {
                        0 -> Triple(24.sp, OneUITextPrimary, FontWeight.Bold)
                        1 -> Triple(17.sp, OneUITextSecondary, FontWeight.Normal)
                        2 -> Triple(13.sp, OneUITextTertiary, FontWeight.Normal)
                        else -> Triple(11.sp, Color.Transparent, FontWeight.Normal)
                    }
                    Text(
                        text = items[actualIdx],
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = textColor,
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
            if (amPmIdx == 1) {
                if (hVal == 12) 12 else hVal + 12
            } else {
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
            .background(OneUICardDark)
            .padding(top = 10.dp, bottom = 14.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Right Mode Indicator & Switcher Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { isKeyboardMode = !isKeyboardMode },
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(OneUICardElevated)
            ) {
                Icon(
                    imageVector = if (isKeyboardMode) Icons.Default.UnfoldMore else Icons.Default.Keyboard,
                    contentDescription = if (isKeyboardMode) "Switch to Scroll Wheel" else "Switch to Keyboard Input",
                    tint = OneUIBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        AnimatedContent(
            targetState = isKeyboardMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "picker_mode"
        ) { keyboardMode ->
            if (keyboardMode) {
                DirectTimeInputRow(
                    hour = hour,
                    minute = minute,
                    is24Hour = is24Hour,
                    onTimeChange = onTimeChange,
                    onDone = { isKeyboardMode = false }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Center Highlight Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(OneUICardElevated)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = OneUITextSecondary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

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
                            ScrollableNumberWheel(
                                items = amPmList,
                                selectedIndex = selectedAmPmIndex,
                                onValueChange = { newAmPm ->
                                    updateTimeFromWheel(selectedHourIndex, selectedMinuteIndex, newAmPm)
                                },
                                onClick = { isKeyboardMode = true },
                                isLooping = false,
                                modifier = Modifier.weight(0.9f)
                            )
                        }
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
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    onDone: () -> Unit
) {
    val isPm = hour >= 12
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }

    val initialHourStr = if (is24Hour) String.format("%02d", hour) else String.format("%02d", hour12)
    val initialMinuteStr = String.format("%02d", minute)

    var hourField by remember {
        mutableStateOf(TextFieldValue(initialHourStr, selection = TextRange(0, initialHourStr.length)))
    }
    var minuteField by remember {
        mutableStateOf(TextFieldValue(initialMinuteStr, selection = TextRange(initialMinuteStr.length)))
    }
    var amPmState by remember {
        mutableStateOf(if (isPm) "PM" else "AM")
    }

    val hourFocusRequester = remember { FocusRequester() }
    val minuteFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(50)
        hourFocusRequester.requestFocus()
        keyboardController?.show()
    }

    // Auto-detect when software keyboard is dismissed to exit back to scroll wheel
    val isImeVisible = WindowInsets.isImeVisible
    var wasImeVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            wasImeVisible = true
        } else if (wasImeVisible) {
            onDone()
            wasImeVisible = false
        }
    }

    fun applyDirectTime(hStr: String, mStr: String, amPm: String) {
        val hParsed = hStr.filter { it.isDigit() }.toIntOrNull() ?: 0
        val mParsed = mStr.filter { it.isDigit() }.toIntOrNull() ?: 0

        val finalH = if (is24Hour) {
            hParsed.coerceIn(0, 23)
        } else {
            val hCapped = if (hParsed in 1..12) hParsed else if (hParsed > 12) 12 else 12
            if (amPm == "PM") {
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
            value = hourField,
            onValueChange = { newVal ->
                val digitsOnly = newVal.text.filter { it.isDigit() }.take(2)
                hourField = newVal.copy(text = digitsOnly, selection = TextRange(digitsOnly.length))
                applyDirectTime(digitsOnly, minuteField.text, amPmState)

                // Auto-advance to minutes when 2 digits typed or when typed 1st digit >= 2 in 12h / >= 3 in 24h
                val num = digitsOnly.toIntOrNull() ?: 0
                if (digitsOnly.length == 2 || (digitsOnly.length == 1 && (if (is24Hour) num >= 3 else num >= 2))) {
                    minuteField = minuteField.copy(selection = TextRange(0, minuteField.text.length))
                    minuteFocusRequester.requestFocus()
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = {
                minuteField = minuteField.copy(selection = TextRange(0, minuteField.text.length))
                minuteFocusRequester.requestFocus()
            }),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = OneUITextPrimary,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .width(76.dp)
                .focusRequester(hourFocusRequester),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OneUIBlue,
                unfocusedBorderColor = OneUIDivider,
                focusedContainerColor = OneUICardElevated,
                unfocusedContainerColor = OneUICardElevated
            )
        )

        Text(
            text = ":",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = OneUITextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Minutes Input
        OutlinedTextField(
            value = minuteField,
            onValueChange = { newVal ->
                val digitsOnly = newVal.text.filter { it.isDigit() }.take(2)
                minuteField = newVal.copy(text = digitsOnly, selection = TextRange(digitsOnly.length))
                applyDirectTime(hourField.text, digitsOnly, amPmState)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                applyDirectTime(hourField.text, minuteField.text, amPmState)
                focusManager.clearFocus()
                keyboardController?.hide()
                onDone()
            }),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = OneUITextPrimary,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .width(76.dp)
                .focusRequester(minuteFocusRequester),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OneUIBlue,
                unfocusedBorderColor = OneUIDivider,
                focusedContainerColor = OneUICardElevated,
                unfocusedContainerColor = OneUICardElevated
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
                            .background(if (isSelected) OneUIBlue else OneUICardElevated)
                            .clickable {
                                amPmState = period
                                applyDirectTime(hourField.text, minuteField.text, period)
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
 * Timer Wheel Duration Picker with 5-row sequence numbers, STATIC inline unit labels,
 * and tap-to-type numeric keyboard activation.
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
    var focusedField by remember { mutableIntStateOf(1) } // 0 = hours, 1 = mins, 2 = secs

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-detect when software keyboard is dismissed by back key/gesture to exit back to scroll wheel
    val isImeVisible = WindowInsets.isImeVisible
    var wasImeVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            wasImeVisible = true
        } else if (wasImeVisible && isKeyboardMode) {
            focusManager.clearFocus()
            isKeyboardMode = false
            wasImeVisible = false
        }
    }

    val hoursList = (0..23).map { String.format("%02d", it) }
    val minutesList = (0..59).map { String.format("%02d", it) }
    val secondsList = (0..59).map { String.format("%02d", it) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(OneUICardDark)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tapping outside inputs closes keypad and returns to drum wheels
                if (isKeyboardMode) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    isKeyboardMode = false
                }
            }
            .padding(top = 10.dp, bottom = 14.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Right Mode Indicator & Switcher Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (isKeyboardMode) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        isKeyboardMode = false
                    } else {
                        focusedField = 1
                        isKeyboardMode = true
                    }
                },
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(OneUICardElevated)
            ) {
                Icon(
                    imageVector = if (isKeyboardMode) Icons.Default.UnfoldMore else Icons.Default.Keyboard,
                    contentDescription = if (isKeyboardMode) "Switch to Scroll Wheel" else "Switch to Numeric Keypad",
                    tint = OneUIBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        AnimatedContent(
            targetState = isKeyboardMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "timer_picker_mode"
        ) { keyboardMode ->
            if (keyboardMode) {
                // Direct Numeric Keypad Typing View
                val hoursFocus = remember { FocusRequester() }
                val minutesFocus = remember { FocusRequester() }
                val secondsFocus = remember { FocusRequester() }

                val initH = if (hours == 0) "00" else String.format("%02d", hours)
                val initM = if (minutes == 0) "00" else String.format("%02d", minutes)
                val initS = if (seconds == 0) "00" else String.format("%02d", seconds)

                var hourField by remember { mutableStateOf(TextFieldValue(initH, selection = TextRange(0, initH.length))) }
                var minField by remember { mutableStateOf(TextFieldValue(initM, selection = TextRange(0, initM.length))) }
                var secField by remember { mutableStateOf(TextFieldValue(initS, selection = TextRange(0, initS.length))) }

                LaunchedEffect(Unit) {
                    delay(50)
                    when (focusedField) {
                        0 -> hoursFocus.requestFocus()
                        1 -> minutesFocus.requestFocus()
                        2 -> secondsFocus.requestFocus()
                    }
                    keyboardController?.show()
                }

                fun applyValues(hStr: String, mStr: String, sStr: String) {
                    val h = hStr.filter { it.isDigit() }.toIntOrNull() ?: 0
                    val m = mStr.filter { it.isDigit() }.toIntOrNull() ?: 0
                    val s = sStr.filter { it.isDigit() }.toIntOrNull() ?: 0
                    onDurationChange(h.coerceIn(0, 23), m.coerceIn(0, 59), s.coerceIn(0, 59))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours Field
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = hourField,
                            onValueChange = { newVal ->
                                val clean = newVal.text.filter { ch -> ch.isDigit() }.take(2)
                                hourField = newVal.copy(text = clean, selection = TextRange(clean.length))
                                applyValues(clean, minField.text, secField.text)
                                if (clean.length == 2 || (clean.length == 1 && (clean.toIntOrNull() ?: 0) >= 3)) {
                                    minField = minField.copy(selection = TextRange(0, minField.text.length))
                                    minutesFocus.requestFocus()
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = {
                                minField = minField.copy(selection = TextRange(0, minField.text.length))
                                minutesFocus.requestFocus()
                            }),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUITextPrimary,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .width(74.dp)
                                .focusRequester(hoursFocus),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OneUIBlue,
                                unfocusedBorderColor = OneUIDivider,
                                focusedContainerColor = OneUICardElevated,
                                unfocusedContainerColor = OneUICardElevated
                            )
                        )
                        Text("hours", fontSize = 11.sp, color = OneUITextSecondary, modifier = Modifier.padding(top = 4.dp))
                    }

                    Text(
                        text = ":",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp).offset(y = (-8).dp)
                    )

                    // Minutes Field
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = minField,
                            onValueChange = { newVal ->
                                val clean = newVal.text.filter { ch -> ch.isDigit() }.take(2)
                                minField = newVal.copy(text = clean, selection = TextRange(clean.length))
                                applyValues(hourField.text, clean, secField.text)
                                if (clean.length == 2 || (clean.length == 1 && (clean.toIntOrNull() ?: 0) >= 6)) {
                                    secField = secField.copy(selection = TextRange(0, secField.text.length))
                                    secondsFocus.requestFocus()
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = {
                                secField = secField.copy(selection = TextRange(0, secField.text.length))
                                secondsFocus.requestFocus()
                            }),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUITextPrimary,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .width(74.dp)
                                .focusRequester(minutesFocus),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OneUIBlue,
                                unfocusedBorderColor = OneUIDivider,
                                focusedContainerColor = OneUICardElevated,
                                unfocusedContainerColor = OneUICardElevated
                            )
                        )
                        Text("min", fontSize = 11.sp, color = OneUITextSecondary, modifier = Modifier.padding(top = 4.dp))
                    }

                    Text(
                        text = ":",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp).offset(y = (-8).dp)
                    )

                    // Seconds Field
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = secField,
                            onValueChange = { newVal ->
                                val clean = newVal.text.filter { ch -> ch.isDigit() }.take(2)
                                secField = newVal.copy(text = clean, selection = TextRange(clean.length))
                                applyValues(hourField.text, minField.text, clean)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                applyValues(hourField.text, minField.text, secField.text)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                isKeyboardMode = false
                            }),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUITextPrimary,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .width(74.dp)
                                .focusRequester(secondsFocus),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OneUIBlue,
                                unfocusedBorderColor = OneUIDivider,
                                focusedContainerColor = OneUICardElevated,
                                unfocusedContainerColor = OneUICardElevated
                            )
                        )
                        Text("sec", fontSize = 11.sp, color = OneUITextSecondary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                // Compact 3-Row Sequence Wheel Drum View with STATIC Unit Suffixes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(114.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Center Highlight Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.96f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(OneUICardElevated)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hours Column (with STATIC "hours" label)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    focusedField = 0
                                    isKeyboardMode = true
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ScrollableNumberWheel(
                                items = hoursList,
                                selectedIndex = hours.coerceIn(0, 23),
                                onValueChange = { newH ->
                                    onDurationChange(newH, minutes, seconds)
                                },
                                onClick = {
                                    focusedField = 0
                                    isKeyboardMode = true
                                },
                                itemHeight = 36.dp,
                                visibleItemsCount = 3,
                                modifier = Modifier.width(36.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "hours",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OneUITextPrimary
                            )
                        }

                        // Minutes Column (with STATIC "min" label)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    focusedField = 1
                                    isKeyboardMode = true
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ScrollableNumberWheel(
                                items = minutesList,
                                selectedIndex = minutes.coerceIn(0, 59),
                                onValueChange = { newM ->
                                    onDurationChange(hours, newM, seconds)
                                },
                                onClick = {
                                    focusedField = 1
                                    isKeyboardMode = true
                                },
                                itemHeight = 36.dp,
                                visibleItemsCount = 3,
                                modifier = Modifier.width(36.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "min",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OneUITextPrimary
                            )
                        }

                        // Seconds Column (with STATIC "sec" label)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    focusedField = 2
                                    isKeyboardMode = true
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ScrollableNumberWheel(
                                items = secondsList,
                                selectedIndex = seconds.coerceIn(0, 59),
                                onValueChange = { newS ->
                                    onDurationChange(hours, minutes, newS)
                                },
                                onClick = {
                                    focusedField = 2
                                    isKeyboardMode = true
                                },
                                itemHeight = 36.dp,
                                visibleItemsCount = 3,
                                modifier = Modifier.width(36.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "sec",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OneUITextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.clockity.app.ui.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.data.models.ActiveTimer
import com.clockity.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Running Timer Card matching One UI circular progress reference design.
 * Progress is Blue when running, Yellow when paused.
 * Automatically adapts dimensions based on active timerCount (1, 2, 3, 4, 5).
 */
@Composable
fun MultiTimerCard(
    timer: ActiveTimer,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    timerCount: Int = 1
) {
    val animatedProgress by animateFloatAsState(
        targetValue = timer.progress,
        label = "timer_progress"
    )

    val targetEndTimeStr = remember(timer.remainingMillis) {
        val targetTime = System.currentTimeMillis() + timer.remainingMillis
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(targetTime))
    }

    val cornerRadius = when {
        timerCount == 1 -> 26.dp
        timerCount == 2 -> 20.dp
        timerCount == 3 -> 18.dp
        else -> 14.dp
    }

    val cardPadding = when {
        timerCount == 1 -> 16.dp
        timerCount == 2 -> 12.dp
        timerCount == 3 -> 8.dp
        else -> 6.dp
    }

    val ringSize = when {
        timerCount == 1 -> 170.dp
        timerCount == 2 -> 125.dp
        timerCount == 3 -> 92.dp
        timerCount == 4 -> 72.dp
        else -> 62.dp
    }

    val strokeWidth = when {
        timerCount == 1 -> 6.dp
        timerCount == 2 -> 5.dp
        timerCount == 3 -> 4.dp
        else -> 3.dp
    }

    val timeFontSize = when {
        timerCount == 1 -> 32.sp
        timerCount == 2 -> 22.sp
        timerCount == 3 -> 16.sp
        timerCount == 4 -> 12.sp
        else -> 10.sp
    }

    val buttonSize = when {
        timerCount == 1 -> 38.dp
        timerCount == 2 -> 32.dp
        timerCount == 3 -> 26.dp
        else -> 20.dp
    }

    val iconSize = when {
        timerCount == 1 -> 18.dp
        timerCount == 2 -> 15.dp
        timerCount == 3 -> 12.dp
        else -> 9.dp
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(OneUICardDark)
            .border(BorderStroke(1.dp, OneUIDivider), RoundedCornerShape(cornerRadius))
            .padding(cardPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = (buttonSize / 3)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Ring with Info in Center
            Box(
                modifier = Modifier
                    .size(ringSize)
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track & Progress Arc (Blue when running, Yellow when paused)
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (timer.isRunning) OneUIBlue else OneUIYellow,
                    strokeWidth = strokeWidth,
                    trackColor = Color(0xFF2C2C30),
                    strokeCap = StrokeCap.Round
                )

                // Inside the ring
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (timerCount <= 2) {
                        // Bell icon & Target End Time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = OneUITextSecondary,
                                modifier = Modifier.size(if (timerCount == 1) 13.dp else 10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = targetEndTimeStr,
                                color = OneUITextSecondary,
                                fontSize = if (timerCount == 1) 12.sp else 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Remaining Countdown Time
                    Text(
                        text = timer.formatRemaining(),
                        color = OneUITextPrimary,
                        fontSize = timeFontSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )

                    // Title (shown if space permits)
                    if (timerCount <= 3) {
                        Text(
                            text = timer.title,
                            color = OneUITextSecondary,
                            fontSize = if (timerCount == 1) 13.sp else if (timerCount == 2) 11.sp else 9.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Action Buttons: Cancel in Bottom-Left, Pause/Resume in Bottom-Right
        // Cancel Button (Bottom-Left)
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(buttonSize)
                .clip(CircleShape)
                .background(OneUIRedCloseBg)
                .border(BorderStroke(1.dp, OneUIRedCloseBorder), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel Timer",
                tint = OneUIRed,
                modifier = Modifier.size(iconSize)
            )
        }

        // Pause / Resume Button (Bottom-Right)
        IconButton(
            onClick = { if (timer.isRunning) onPause() else onResume() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(buttonSize)
                .clip(CircleShape)
                .background(if (timer.isRunning) OneUIYellowPauseBg else OneUIBlueResumeBg)
                .border(
                    BorderStroke(
                        1.dp,
                        if (timer.isRunning) OneUIYellowPauseBorder else OneUIBlueResumeBorder
                    ),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (timer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (timer.isRunning) "Pause" else "Resume",
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

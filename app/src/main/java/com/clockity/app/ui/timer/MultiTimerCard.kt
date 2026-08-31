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
 * Running Timer Card matching One UI circular progress theme.
 * Progress is Blue when running, Yellow when paused.
 * Supports compactMode: 0 = Full width, 1 = Half (50%), 2 = Third (33%).
 */
@Composable
fun MultiTimerCard(
    timer: ActiveTimer,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    compactMode: Int = 0
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

    val cornerRadius = when (compactMode) {
        0 -> 26.dp
        1 -> 20.dp
        else -> 16.dp
    }

    val cardPadding = when (compactMode) {
        0 -> 16.dp
        1 -> 10.dp
        else -> 6.dp
    }

    val ringSize = when (compactMode) {
        0 -> 170.dp
        1 -> 120.dp
        else -> 82.dp
    }

    val strokeWidth = when (compactMode) {
        0 -> 6.dp
        1 -> 4.5.dp
        else -> 3.5.dp
    }

    val timeFontSize = when (compactMode) {
        0 -> 32.sp
        1 -> 21.sp
        else -> 14.sp
    }

    val buttonSize = when (compactMode) {
        0 -> 38.dp
        1 -> 30.dp
        else -> 24.dp
    }

    val iconSize = when (compactMode) {
        0 -> 18.dp
        1 -> 14.dp
        else -> 11.dp
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(OneUICardDark)
            .border(BorderStroke(1.dp, OneUIDivider), RoundedCornerShape(cornerRadius))
            .padding(cardPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
                    if (compactMode < 2) {
                        // Bell icon & Target End Time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = OneUITextSecondary,
                                modifier = Modifier.size(if (compactMode == 0) 13.dp else 10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = targetEndTimeStr,
                                color = OneUITextSecondary,
                                fontSize = if (compactMode == 0) 12.sp else 10.sp,
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

                    // Title
                    Text(
                        text = timer.title,
                        color = OneUITextSecondary,
                        fontSize = if (compactMode == 0) 13.sp else if (compactMode == 1) 11.sp else 9.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (compactMode == 0) 10.dp else 6.dp))

            // Action Buttons Row (Cancel on Left, Pause/Resume on Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Button (Bottom Left)
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
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

                // Pause / Resume Button (Bottom Right)
                IconButton(
                    onClick = { if (timer.isRunning) onPause() else onResume() },
                    modifier = Modifier
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
    }
}

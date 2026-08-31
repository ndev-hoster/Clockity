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
 * Running Timer Card with fixed relative 1/5th horizontal width.
 * Progress is Blue when running, Yellow when paused.
 */
@Composable
fun MultiTimerCard(
    timer: ActiveTimer,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
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

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(OneUICardDark)
            .border(BorderStroke(1.dp, OneUIDivider), RoundedCornerShape(18.dp))
            .padding(8.dp)
    ) {
        val cardWidth = maxWidth
        val isWide = cardWidth >= 110.dp
        val ringSize = (cardWidth - 20.dp).coerceIn(48.dp, 120.dp)
        val strokeWidth = if (isWide) 4.5.dp else 3.dp
        val timeFontSize = if (isWide) 16.sp else if (cardWidth >= 75.dp) 12.sp else 10.sp
        val buttonSize = if (isWide) 28.dp else 20.dp
        val iconSize = if (isWide) 14.dp else 10.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = buttonSize / 2),
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
                    if (isWide) {
                        // Bell icon & Target End Time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = OneUITextSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = targetEndTimeStr,
                                color = OneUITextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(1.dp))
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
                    if (isWide) {
                        Text(
                            text = timer.title,
                            color = OneUITextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Action Buttons: Cancel in Bottom-Left, Pause/Resume in Bottom-Right
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

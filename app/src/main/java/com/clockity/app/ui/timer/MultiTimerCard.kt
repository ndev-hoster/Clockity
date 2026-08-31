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
 * Fully responsive for both narrow phone screens and wide tablet screens.
 * Progress is Blue when running, Gray when paused.
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
            .clip(RoundedCornerShape(24.dp))
            .background(OneUICardDark)
            .border(BorderStroke(1.dp, OneUIDivider), RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        val isCompact = maxWidth < 260.dp
        val ringSize = if (isCompact) 130.dp else 170.dp
        val strokeWidth = if (isCompact) 5.dp else 6.dp
        val timeFontSize = if (isCompact) 24.sp else 34.sp
        val buttonSize = if (isCompact) 34.dp else 40.dp
        val iconSize = if (isCompact) 16.dp else 20.dp

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Ring with Info in Center
            Box(
                modifier = Modifier
                    .size(ringSize)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track & Progress Arc (Blue when running, Gray when paused)
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (timer.isRunning) OneUIBlue else OneUITextSecondary,
                    strokeWidth = strokeWidth,
                    trackColor = Color(0xFF2C2C30),
                    strokeCap = StrokeCap.Round
                )

                // Inside the ring
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Bell icon & Target End Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = OneUITextSecondary,
                            modifier = Modifier.size(if (isCompact) 11.dp else 13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = targetEndTimeStr,
                            color = OneUITextSecondary,
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Remaining Countdown Time
                    Text(
                        text = timer.formatRemaining(),
                        color = OneUITextPrimary,
                        fontSize = timeFontSize,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Title
                    Text(
                        text = timer.title,
                        color = OneUITextSecondary,
                        fontSize = if (isCompact) 12.sp else 13.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row (Cancel on Left, Pause/Resume on Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Button (Bottom-Left)
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

                // Pause / Resume Button (Bottom-Right)
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

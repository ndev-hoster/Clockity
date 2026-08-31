package com.clockity.app.ui.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.data.models.ActiveTimer
import com.clockity.app.ui.theme.*

@Composable
fun MultiTimerCard(
    timer: ActiveTimer,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onAddMinute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = timer.progress,
        label = "timer_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(OneUICardDark)
            .padding(18.dp)
    ) {
        // Header Row: Title & Cancel Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timer.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = OneUITextPrimary
            )

            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(OneUICardElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel Timer",
                    tint = OneUITextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Large Remaining Digits
        Text(
            text = timer.formatRemaining(),
            fontSize = 42.sp,
            fontWeight = FontWeight.Light,
            color = if (timer.remainingMillis == 0L) OneUIRed else OneUITextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Linear Progress Bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (timer.remainingMillis == 0L) OneUIRed else OneUIBlue,
            trackColor = OneUIDivider,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action Controls Row (+1m, Pause/Resume)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // +1m Quick Add Button
            OutlinedButton(
                onClick = onAddMinute,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OneUIBlue),
                modifier = Modifier.height(40.dp)
            ) {
                Text("+1 min", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Pause / Resume Button
            Button(
                onClick = { if (timer.isRunning) onPause() else onResume() },
                shape = RoundedCornerShape(14.dp),
                border = if (timer.isRunning) androidx.compose.foundation.BorderStroke(1.dp, OneUIYellowPauseBorder) else null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timer.isRunning) OneUIYellowPauseBg else OneUIBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(
                    imageVector = if (timer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (timer.isRunning) "Pause" else "Resume",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (timer.isRunning) "Pause" else "Resume",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

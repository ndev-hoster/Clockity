package com.clockity.app.ui.worldclock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.ui.theme.*

@Composable
fun TimeScrubberBar(
    offsetHours: Float,
    onOffsetChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(OneUICardElevated)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Time Zone Converter",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OneUITextSecondary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                val offsetText = when {
                    offsetHours == 0f -> "Current time"
                    offsetHours > 0f -> String.format("+%.1f hrs", offsetHours)
                    else -> String.format("%.1f hrs", offsetHours)
                }

                Text(
                    text = offsetText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (offsetHours != 0f) OneUIBlue else OneUITextSecondary
                )

                if (offsetHours != 0f) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset time scrubber",
                            tint = OneUIBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Slider(
            value = offsetHours,
            onValueChange = onOffsetChange,
            valueRange = -12f..12f,
            steps = 47, // 30 min intervals
            colors = SliderDefaults.colors(
                thumbColor = OneUIBlue,
                activeTrackColor = OneUIBlue,
                inactiveTrackColor = OneUIDivider
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

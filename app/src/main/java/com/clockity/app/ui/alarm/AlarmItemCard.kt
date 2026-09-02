package com.clockity.app.ui.alarm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.data.models.Alarm
import com.clockity.app.ui.components.OneUISwitch
import com.clockity.app.ui.theme.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmItemCard(
    alarm: Alarm,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isInsideGroup: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false
) {
    val (timeStr, amPm) = alarm.formatTime12H()
    val textColor by animateColorAsState(
        targetValue = if (alarm.isEnabled) OneUITextPrimary else OneUITextDisabled,
        label = "alarm_text_color"
    )
    val subTextColor by animateColorAsState(
        targetValue = if (alarm.isEnabled) OneUITextSecondary else OneUITextDisabled,
        label = "alarm_sub_color"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (isInsideGroup) 16.dp else 24.dp))
            .background(
                if (isSelected) OneUIBlue.copy(alpha = 0.18f)
                else if (isInsideGroup) OneUICardElevated
                else OneUICardDark
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) OneUIBlue else OneUIDivider),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = OneUIBlack,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
        }

        // Time & Details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = timeStr,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = amPm,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Label & Days
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (alarm.label.isNotBlank()) {
                    Text(
                        text = alarm.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    Text(
                        text = " • ",
                        fontSize = 14.sp,
                        color = subTextColor
                    )
                }
                if (alarm.specificDateMillis != null) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Specific Date",
                        tint = OneUIYellow,
                        modifier = Modifier.size(13.dp).padding(end = 2.dp)
                    )
                }
                Text(
                    text = alarm.formatDaysSummary(),
                    fontSize = 14.sp,
                    color = if (alarm.specificDateMillis != null) OneUIYellow else subTextColor
                )
                if (alarm.isGentleWakeUp && alarm.isEnabled) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(OneUIBlue.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Gentle",
                            fontSize = 10.sp,
                            color = OneUIBlueLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (!isSelectionMode) {
            // Toggle Switch
            OneUISwitch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() },
                checkedTrackColor = OneUIBlue
            )
        }
    }
}

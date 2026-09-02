package com.clockity.app.ui.alarm

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.data.models.Alarm
import com.clockity.app.data.models.AlarmGroup
import com.clockity.app.ui.components.OneUISwitch
import com.clockity.app.ui.theme.*

@Composable
fun AlarmGroupCard(
    group: AlarmGroup,
    alarms: List<Alarm>,
    onToggleGroup: () -> Unit,
    onToggleExpansion: () -> Unit,
    onToggleAlarm: (Alarm) -> Unit,
    onAlarmClick: (Alarm) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    onAddAlarmToGroup: () -> Unit,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClickAlarm: ((Alarm) -> Unit)? = null,
    isSelectionMode: Boolean = false,
    selectedAlarmIds: Set<Long> = emptySet()
) {
    var showMenu by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (group.isExpanded) 180f else 0f,
        label = "arrow_rotation"
    )

    val activeCount = alarms.count { it.isEnabled }
    val totalCount = alarms.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(OneUICardDark)
            .padding(bottom = if (group.isExpanded && alarms.isNotEmpty()) 12.dp else 0.dp)
    ) {
        // Group Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpansion() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group Icon Pill
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OneUIBlue)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Group Name & Alarm Count Summary
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OneUITextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = OneUITextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(arrowRotation)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (totalCount == 0) "No alarms in group" else "$activeCount of $totalCount active",
                    fontSize = 13.sp,
                    color = OneUITextSecondary
                )
            }

            // More Options Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Group options",
                        tint = OneUITextSecondary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(OneUICardElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Group", color = OneUITextPrimary) },
                        onClick = {
                            showMenu = false
                            onEditGroup()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Group", color = OneUIRed) },
                        onClick = {
                            showMenu = false
                            onDeleteGroup()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Master Group Toggle Switch
            OneUISwitch(
                checked = group.isEnabled,
                onCheckedChange = { onToggleGroup() },
                checkedTrackColor = OneUIBlue
            )
        }

        // Expanded Nested Alarms List
        AnimatedVisibility(
            visible = group.isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                if (alarms.isEmpty()) {
                    Text(
                        text = "No alarms added to this group yet.",
                        fontSize = 13.sp,
                        color = OneUITextTertiary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                } else {
                    alarms.forEach { alarm ->
                        AlarmItemCard(
                            alarm = alarm,
                            onToggle = { onToggleAlarm(alarm) },
                            onClick = { onAlarmClick(alarm) },
                            onDelete = { onDeleteAlarm(alarm) },
                            isInsideGroup = true,
                            onLongClick = { onLongClickAlarm?.invoke(alarm) },
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedAlarmIds.contains(alarm.id),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Add Alarm to Group Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onAddAlarmToGroup() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Alarm",
                        tint = OneUIBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add alarm to ${group.name}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = OneUIBlue
                    )
                }
            }
        }
    }
}

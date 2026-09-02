package com.clockity.app.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.data.models.Alarm
import com.clockity.app.data.models.AlarmGroup
import com.clockity.app.ui.components.OneUIHeader
import com.clockity.app.ui.theme.*

@Composable
fun AlarmScreen(
    viewModel: AlarmViewModel,
    modifier: Modifier = Modifier,
    onOpenBackup: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    var showEditAlarmDialog by remember { mutableStateOf(false) }
    var selectedAlarmForEdit by remember { mutableStateOf<Alarm?>(null) }
    var initialGroupIdForNewAlarm by remember { mutableStateOf<Long?>(null) }

    var showGroupDialog by remember { mutableStateOf(false) }
    var selectedGroupForEdit by remember { mutableStateOf<AlarmGroup?>(null) }

    var longPressedAlarm by remember { mutableStateOf<Alarm?>(null) }
    var showMoveToGroupDialog by remember { mutableStateOf(false) }
    var alarmToMove by remember { mutableStateOf<Alarm?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OneUIBlack)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // One UI Header
            OneUIHeader(
                title = "Alarm",
                subtitle = uiState.nextAlarmSummary,
                onMenuClick = onOpenBackup,
                extraContent = {
                    // Secondary Action Row (Create New Group button)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        AssistChip(
                            onClick = {
                                selectedGroupForEdit = null
                                showGroupDialog = true
                            },
                            label = { Text("+ New Group", color = OneUIBlue, fontSize = 13.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = OneUICardDark
                            ),
                            border = null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            )

        // Alarms & Groups List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp)
        ) {
            // 1. Alarm Groups
            items(uiState.groups, key = { "group_${it.id}" }) { group ->
                val groupAlarms = uiState.alarms.filter { it.groupId == group.id }
                AlarmGroupCard(
                    group = group,
                    alarms = groupAlarms,
                    onToggleGroup = { viewModel.toggleGroup(group) },
                    onToggleExpansion = { viewModel.toggleGroupExpansion(group) },
                    onToggleAlarm = { viewModel.toggleAlarm(it) },
                    onAlarmClick = {
                        selectedAlarmForEdit = it
                        initialGroupIdForNewAlarm = group.id
                        showEditAlarmDialog = true
                    },
                    onDeleteAlarm = { viewModel.deleteAlarm(it) },
                    onAddAlarmToGroup = {
                        selectedAlarmForEdit = null
                        initialGroupIdForNewAlarm = group.id
                        showEditAlarmDialog = true
                    },
                    onEditGroup = {
                        selectedGroupForEdit = group
                        showGroupDialog = true
                    },
                    onDeleteGroup = { viewModel.deleteGroup(group) },
                    onLongClickAlarm = { alarm ->
                        longPressedAlarm = alarm
                    }
                )
            }

            // 2. Ungrouped Alarms
            val ungroupedAlarms = uiState.alarms.filter { it.groupId == null }
            if (ungroupedAlarms.isNotEmpty()) {
                item {
                    if (uiState.groups.isNotEmpty()) {
                        Text(
                            text = "Other Alarms",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OneUITextSecondary,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                }

                items(ungroupedAlarms, key = { "alarm_${it.id}" }) { alarm ->
                    AlarmItemCard(
                        alarm = alarm,
                        onToggle = { viewModel.toggleAlarm(alarm) },
                        onClick = {
                            selectedAlarmForEdit = alarm
                            initialGroupIdForNewAlarm = null
                            showEditAlarmDialog = true
                        },
                        onDelete = { viewModel.deleteAlarm(alarm) },
                        onLongClick = {
                            longPressedAlarm = alarm
                        }
                    )
                }
            }

            // Empty state
            if (uiState.groups.isEmpty() && uiState.alarms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No alarms yet. Tap + to add one.",
                            fontSize = 15.sp,
                            color = OneUITextTertiary
                        )
                    }
                }
            }
        }
    }

    // Hovering Bottom-Right Floating Action Button for Adding New Alarm
        FloatingActionButton(
            onClick = {
                selectedAlarmForEdit = null
                initialGroupIdForNewAlarm = null
                showEditAlarmDialog = true
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = OneUIBlue,
            contentColor = OneUIBlack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .size(58.dp),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Alarm",
                tint = OneUIBlack,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Dialog: Long-Press Alarm Quick Actions (Move to Group, Delete, Edit)
    if (longPressedAlarm != null) {
        val targetAlarm = longPressedAlarm!!
        val (timeStr, amPm) = targetAlarm.formatTime12H()
        val currentGroup = uiState.groups.firstOrNull { it.id == targetAlarm.groupId }

        AlertDialog(
            onDismissRequest = { longPressedAlarm = null },
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$timeStr $amPm",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = OneUITextPrimary
                        )
                    }
                    if (targetAlarm.label.isNotBlank() || currentGroup != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = listOfNotNull(
                                targetAlarm.label.ifBlank { null },
                                currentGroup?.name?.let { "In $it" }
                            ).joinToString(" • "),
                            fontSize = 13.sp,
                            color = OneUITextSecondary
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option: Move to Group
                    Surface(
                        onClick = {
                            alarmToMove = targetAlarm
                            longPressedAlarm = null
                            showMoveToGroupDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = OneUICardDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = OneUIBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (currentGroup != null) "Change / Remove Group" else "Move to Group",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OneUITextPrimary
                                )
                                Text(
                                    text = if (currentGroup != null) "Currently in '${currentGroup.name}'" else "Assign alarm to a group",
                                    fontSize = 12.sp,
                                    color = OneUITextSecondary
                                )
                            }
                        }
                    }

                    // Option: Edit Alarm
                    Surface(
                        onClick = {
                            selectedAlarmForEdit = targetAlarm
                            initialGroupIdForNewAlarm = targetAlarm.groupId
                            longPressedAlarm = null
                            showEditAlarmDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = OneUICardDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = OneUITextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Edit Alarm",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OneUITextPrimary
                            )
                        }
                    }

                    // Option: Delete Alarm (Instant delete without opening full editor)
                    Surface(
                        onClick = {
                            viewModel.deleteAlarm(targetAlarm)
                            longPressedAlarm = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = OneUIRed.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = OneUIRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Delete Alarm",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OneUIRed
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { longPressedAlarm = null }) {
                    Text("Cancel", color = OneUITextSecondary)
                }
            },
            containerColor = OneUICardElevated,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Dialog: Move Alarm To Group Selection
    if (showMoveToGroupDialog && alarmToMove != null) {
        val targetAlarm = alarmToMove!!
        val currentGroupId = targetAlarm.groupId

        AlertDialog(
            onDismissRequest = {
                showMoveToGroupDialog = false
                alarmToMove = null
            },
            title = {
                Text(
                    text = "Select Alarm Group",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.groups.isEmpty()) {
                        Text(
                            text = "No groups yet. Create a new group first!",
                            fontSize = 14.sp,
                            color = OneUITextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        uiState.groups.forEach { group ->
                            val isSelected = group.id == currentGroupId
                            Surface(
                                onClick = {
                                    viewModel.moveAlarmToGroup(targetAlarm, group.id)
                                    showMoveToGroupDialog = false
                                    alarmToMove = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) OneUIBlue.copy(alpha = 0.18f) else OneUICardDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                try {
                                                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(group.colorHex))
                                                } catch (_: Exception) {
                                                    OneUIBlue
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = group.name,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) OneUIBlue else OneUITextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = "Current",
                                            fontSize = 12.sp,
                                            color = OneUIBlue,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Remove from Group Option (if currently in a group)
                    if (currentGroupId != null) {
                        Surface(
                            onClick = {
                                viewModel.moveAlarmToGroup(targetAlarm, null)
                                showMoveToGroupDialog = false
                                alarmToMove = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = OneUICardDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Remove from Group (Ungroup)",
                                    fontSize = 14.sp,
                                    color = OneUIYellow,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Create New Group Shortcut
                    TextButton(
                        onClick = {
                            showMoveToGroupDialog = false
                            selectedGroupForEdit = null
                            showGroupDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Create New Group", color = OneUIBlue, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showMoveToGroupDialog = false
                    alarmToMove = null
                }) {
                    Text("Close", color = OneUITextSecondary)
                }
            },
            containerColor = OneUICardElevated,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Dialog: Edit / Add Alarm
    if (showEditAlarmDialog) {
        EditAlarmDialog(
            alarm = selectedAlarmForEdit,
            initialGroupId = initialGroupIdForNewAlarm,
            groups = uiState.groups,
            onDismiss = { showEditAlarmDialog = false },
            onSave = { viewModel.saveAlarm(it) },
            onDelete = { viewModel.deleteAlarm(it) }
        )
    }

    // Dialog: Create / Edit Group
    if (showGroupDialog) {
        EditGroupDialog(
            group = selectedGroupForEdit,
            existingGroups = uiState.groups,
            onDismiss = { showGroupDialog = false },
            onSave = { name ->
                if (selectedGroupForEdit == null) {
                    viewModel.createGroup(name)
                } else {
                    viewModel.updateGroup(selectedGroupForEdit!!.copy(name = name))
                }
            }
        )
    }
}

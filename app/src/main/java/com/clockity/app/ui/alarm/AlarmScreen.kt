package com.clockity.app.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
                    onDeleteGroup = { viewModel.deleteGroup(group) }
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
                        onDelete = { viewModel.deleteAlarm(alarm) }
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

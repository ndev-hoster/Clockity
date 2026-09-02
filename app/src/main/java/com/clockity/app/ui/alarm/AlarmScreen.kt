package com.clockity.app.ui.alarm

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedAlarmIds by remember { mutableStateOf(setOf<Long>()) }

    var showEditAlarmDialog by remember { mutableStateOf(false) }
    var selectedAlarmForEdit by remember { mutableStateOf<Alarm?>(null) }
    var initialGroupIdForNewAlarm by remember { mutableStateOf<Long?>(null) }

    var showGroupDialog by remember { mutableStateOf(false) }
    var selectedGroupForEdit by remember { mutableStateOf<AlarmGroup?>(null) }

    var showMoveToGroupDialog by remember { mutableStateOf(false) }
    var showSelectionMenu by remember { mutableStateOf(false) }

    // Intercept back button when in selection mode
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedAlarmIds = emptySet()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OneUIBlack)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isSelectionMode) {
                // Multi-Select Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OneUIBlack)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(OneUICardElevated)
                                .clickable {
                                    isSelectionMode = false
                                    selectedAlarmIds = emptySet()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel selection",
                                tint = OneUITextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "${selectedAlarmIds.size} selected",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = OneUITextPrimary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Select All / Deselect All
                        val allSelected = selectedAlarmIds.size == uiState.alarms.size && uiState.alarms.isNotEmpty()
                        TextButton(onClick = {
                            selectedAlarmIds = if (allSelected) {
                                emptySet()
                            } else {
                                uiState.alarms.map { it.id }.toSet()
                            }
                        }) {
                            Text(
                                text = if (allSelected) "Deselect all" else "Select all",
                                color = OneUIBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // 3-dot Menu for Selection Mode Actions
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(OneUICardElevated)
                                    .clickable { showSelectionMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More actions",
                                    tint = OneUITextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showSelectionMenu,
                                onDismissRequest = { showSelectionMenu = false },
                                modifier = Modifier.background(OneUICardElevated)
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = OneUIBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    text = { Text("Move to group", color = OneUITextPrimary) },
                                    onClick = {
                                        showSelectionMenu = false
                                        showMoveToGroupDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = OneUIRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    text = { Text("Delete selected", color = OneUIRed) },
                                    onClick = {
                                        showSelectionMenu = false
                                        val toDelete = uiState.alarms.filter { it.id in selectedAlarmIds }
                                        viewModel.deleteAlarms(toDelete)
                                        isSelectionMode = false
                                        selectedAlarmIds = emptySet()
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // One UI Header (No 3-dot menu by default)
                OneUIHeader(
                    title = "Alarm",
                    subtitle = uiState.nextAlarmSummary,
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
            }

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
                        onAlarmClick = { alarm ->
                            if (isSelectionMode) {
                                val next = if (selectedAlarmIds.contains(alarm.id)) selectedAlarmIds - alarm.id else selectedAlarmIds + alarm.id
                                selectedAlarmIds = next
                                if (next.isEmpty()) isSelectionMode = false
                            } else {
                                selectedAlarmForEdit = alarm
                                initialGroupIdForNewAlarm = group.id
                                showEditAlarmDialog = true
                            }
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
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedAlarmIds = setOf(alarm.id)
                            }
                        },
                        isSelectionMode = isSelectionMode,
                        selectedAlarmIds = selectedAlarmIds
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
                                if (isSelectionMode) {
                                    val next = if (selectedAlarmIds.contains(alarm.id)) selectedAlarmIds - alarm.id else selectedAlarmIds + alarm.id
                                    selectedAlarmIds = next
                                    if (next.isEmpty()) isSelectionMode = false
                                } else {
                                    selectedAlarmForEdit = alarm
                                    initialGroupIdForNewAlarm = null
                                    showEditAlarmDialog = true
                                }
                            },
                            onDelete = { viewModel.deleteAlarm(alarm) },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedAlarmIds = setOf(alarm.id)
                                }
                            },
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedAlarmIds.contains(alarm.id)
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

        // Hovering Bottom-Right Floating Action Button for Adding New Alarm (hidden in selection mode)
        if (!isSelectionMode) {
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
    }

    // Dialog: Move Selected Alarms To Group
    if (showMoveToGroupDialog && selectedAlarmIds.isNotEmpty()) {
        val selectedAlarms = uiState.alarms.filter { it.id in selectedAlarmIds }

        AlertDialog(
            onDismissRequest = { showMoveToGroupDialog = false },
            title = {
                Text(
                    text = "Move ${selectedAlarms.size} Alarm${if (selectedAlarms.size > 1) "s" else ""} to Group",
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
                            Surface(
                                onClick = {
                                    viewModel.moveAlarmsToGroup(selectedAlarms, group.id)
                                    showMoveToGroupDialog = false
                                    isSelectionMode = false
                                    selectedAlarmIds = emptySet()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = OneUICardDark,
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
                                        fontWeight = FontWeight.Medium,
                                        color = OneUITextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Remove from Group Option (Ungroup)
                    Surface(
                        onClick = {
                            viewModel.moveAlarmsToGroup(selectedAlarms, null)
                            showMoveToGroupDialog = false
                            isSelectionMode = false
                            selectedAlarmIds = emptySet()
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
                TextButton(onClick = { showMoveToGroupDialog = false }) {
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

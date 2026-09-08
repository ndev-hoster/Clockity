package com.clockity.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color.parseColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clockity.app.data.local.ClockityDatabase
import com.clockity.app.service.NotificationHelper
import com.clockity.app.ui.components.OneUIHeader
import com.clockity.app.ui.components.OneUISwitch
import com.clockity.app.ui.theme.*
import com.clockity.app.utils.AppLogger
import com.clockity.app.utils.BackupManager
import com.clockity.app.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Preferences states
    val currentAccentHex by PreferencesManager.accentColorHex.collectAsState()
    val isAmoledBlack by PreferencesManager.isAmoledBlack.collectAsState()
    val isTimerFlashEnabled by PreferencesManager.isTimerFlashEnabled.collectAsState()
    val volumeKeyBehavior by PreferencesManager.volumeKeyBehavior.collectAsState()
    val defaultSnoozeMins by PreferencesManager.defaultSnoozeMins.collectAsState()
    val defaultSnoozeRepeat by PreferencesManager.defaultSnoozeRepeatCount.collectAsState()
    val alarmSilenceMins by PreferencesManager.alarmSilenceMins.collectAsState()
    val isDebugLogsEnabled by PreferencesManager.isDebugLogsEnabled.collectAsState()
    val lastBackupTimestamp by PreferencesManager.lastBackupTimestamp.collectAsState()

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }

    val accentColor = remember(currentAccentHex) {
        try { Color(parseColor(currentAccentHex)) } catch (_: Exception) { OneUIBlue }
    }

    // Export Document Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            statusMessage = "Exporting data..."
            coroutineScope.launch {
                val res = BackupManager.exportBackup(context, uri)
                isProcessing = false
                res.onSuccess { msg ->
                    statusMessage = msg
                    Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    statusMessage = "Export failed: ${err.message}"
                }
            }
        }
    }

    // Import Document Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OneUIBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // One UI Header
            OneUIHeader(
                title = "Settings",
                subtitle = "Preferences, Appearance & Data"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ==========================================
                // SECTION 1: ALARM & SNOOZE PREFERENCES
                // ==========================================
                Text(
                    text = "Alarm & Snooze Defaults",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUITextSecondary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = OneUICardDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Volume Key Action
                        Column {
                            Text(
                                text = "Volume Key Behavior",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OneUITextPrimary
                            )
                            Text(
                                text = "Action triggered when pressing hardware volume keys during alarm",
                                fontSize = 12.sp,
                                color = OneUITextSecondary,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Snooze" to "Snooze (Default)", "Dismiss" to "Dismiss", "None" to "Do nothing").forEach { (key, label) ->
                                    val isSelected = volumeKeyBehavior == key
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) accentColor else OneUICardElevated)
                                            .clickable { PreferencesManager.setVolumeKeyBehavior(context, key) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) OneUIBlack else OneUITextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = OneUIDivider, thickness = 0.5.dp)

                        // Default Snooze Duration
                        Column {
                            Text(
                                text = "Default Snooze Duration",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OneUITextPrimary
                            )
                            Text(
                                text = "Initial duration for newly created alarms",
                                fontSize = 12.sp,
                                color = OneUITextSecondary,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(5 to "5 mins", 10 to "10 mins", 15 to "15 mins", 30 to "30 mins").forEach { (mins, label) ->
                                    val isSelected = defaultSnoozeMins == mins
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) accentColor else OneUICardElevated)
                                            .clickable { PreferencesManager.setDefaultSnoozeMins(context, mins) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) OneUIBlack else OneUITextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = OneUIDivider, thickness = 0.5.dp)

                        // Default Snooze Repeat Count
                        Column {
                            Text(
                                text = "Default Snooze Repeat",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OneUITextPrimary
                            )
                            Text(
                                text = "How many times new alarms can be snoozed",
                                fontSize = 12.sp,
                                color = OneUITextSecondary,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(1 to "1 time", 3 to "3 times", 5 to "5 times", 10 to "10 times", 0 to "Never").forEach { (count, label) ->
                                    val isSelected = defaultSnoozeRepeat == count
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) accentColor else OneUICardElevated)
                                            .clickable { PreferencesManager.setDefaultSnoozeRepeatCount(context, count) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) OneUIBlack else OneUITextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = OneUIDivider, thickness = 0.5.dp)

                        // Silence Alarm After
                        Column {
                            Text(
                                text = "Silence Alarm After",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OneUITextPrimary
                            )
                            Text(
                                text = "Duration the alarm will sound before automatically stopping and marking as missed",
                                fontSize = 12.sp,
                                color = OneUITextSecondary,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(1 to "1 min", 2 to "2 mins", 5 to "5 mins", 10 to "10 mins", 15 to "15 mins", 0 to "Never").forEach { (mins, label) ->
                                    val isSelected = alarmSilenceMins == mins
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) accentColor else OneUICardElevated)
                                            .clickable { PreferencesManager.setAlarmSilenceMins(context, mins) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) OneUIBlack else OneUITextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // SECTION 2: TIMER & FOCUS SETTINGS (3.3)
                // ==========================================
                Text(
                    text = "Timer & Focus",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUITextSecondary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = OneUICardDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Screen Flash on Completion",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OneUITextPrimary
                            )
                            Text(
                                text = "Pulse screen edges when a timer or pomodoro interval ends",
                                fontSize = 12.sp,
                                color = OneUITextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OneUISwitch(
                            checked = isTimerFlashEnabled,
                            onCheckedChange = { PreferencesManager.setTimerFlashEnabled(context, it) },
                            checkedTrackColor = accentColor
                        )
                    }
                }

                // ==========================================
                // SECTION 3: APPEARANCE & ACCENT COLORS (4)
                // ==========================================
                Text(
                    text = "Appearance & Customization",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUITextSecondary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = OneUICardDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Accent Color Palette
                        Column {
                            Text(
                                text = "Accent Color",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OneUITextPrimary
                            )
                            Text(
                                text = "Choose theme highlight and active control color",
                                fontSize = 12.sp,
                                color = OneUITextSecondary,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val colors = listOf(
                                    "#3E82F7" to "Blue",
                                    "#8E54E9" to "Violet",
                                    "#00C853" to "Emerald",
                                    "#FF6D00" to "Sunset",
                                    "#FF3B30" to "Rose"
                                )
                                colors.forEach { (hex, _) ->
                                    val parsed = try { Color(parseColor(hex)) } catch (_: Exception) { OneUIBlue }
                                    val isSelected = currentAccentHex.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(parsed)
                                            .clickable { PreferencesManager.setAccentColor(context, hex) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = OneUIBlack,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = OneUIDivider, thickness = 0.5.dp)

                        // AMOLED Pure Black
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AMOLED Pure Black",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OneUITextPrimary
                                )
                                Text(
                                    text = "Render pure #000000 background cards to save battery on OLED displays",
                                    fontSize = 12.sp,
                                    color = OneUITextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OneUISwitch(
                                checked = isAmoledBlack,
                                onCheckedChange = { PreferencesManager.setAmoledBlack(context, it) },
                                checkedTrackColor = accentColor
                            )
                        }
                    }
                }

                // ==========================================
                // SECTION 4: BACKUP & DATA MANAGEMENT (5)
                // ==========================================
                Text(
                    text = "Backup & Data Management",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUITextSecondary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = OneUICardDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Last Backup Info
                        val lastBackupStr = if (lastBackupTimestamp > 0) {
                            val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                            sdf.format(Date(lastBackupTimestamp))
                        } else {
                            "Never"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Last Backup Taken",
                                fontSize = 14.sp,
                                color = OneUITextSecondary
                            )
                            Text(
                                text = lastBackupStr,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (lastBackupTimestamp > 0) accentColor else OneUITextTertiary
                            )
                        }

                        if (isProcessing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = accentColor,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = statusMessage ?: "Processing...",
                                    fontSize = 13.sp,
                                    color = accentColor
                                )
                            }
                        } else if (statusMessage != null) {
                            Text(
                                text = statusMessage!!,
                                fontSize = 12.sp,
                                color = if (statusMessage!!.contains("failed", ignoreCase = true)) OneUIRed else OneUIBlueLight,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        // Export Button
                        Surface(
                            onClick = {
                                val filename = "clockity_backup_${System.currentTimeMillis() / 1000}.json"
                                exportLauncher.launch(filename)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = accentColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Export",
                                    tint = OneUIBlack,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Export Backup (JSON)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUIBlack
                                )
                            }
                        }

                        // Restore Button
                        Surface(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = OneUICardElevated,
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Restore",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Restore from JSON File",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor
                                )
                            }
                        }

                        HorizontalDivider(color = OneUIDivider, thickness = 0.5.dp)

                        // Factory Reset Button
                        Surface(
                            onClick = { showResetConfirmDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            color = OneUIRed.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Reset",
                                    tint = OneUIRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Reset All Data to Defaults",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OneUIRed
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // SECTION: DIAGNOSTICS & DEBUGGING
                // ==========================================
                Text(
                    text = "Diagnostics & Debugging",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUITextSecondary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = OneUICardDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Debug Logging Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Debug Logging",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OneUITextPrimary
                                )
                                Text(
                                    text = "Record detailed system events for alarms, timers, and notifications",
                                    fontSize = 12.sp,
                                    color = OneUITextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            OneUISwitch(
                                checked = isDebugLogsEnabled,
                                onCheckedChange = { PreferencesManager.setDebugLogsEnabled(context, it) },
                                checkedTrackColor = accentColor
                            )
                        }

                        HorizontalDivider(color = OneUIDivider, thickness = 0.5.dp)

                        // View Logs Button & Test Missed Alarm Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showLogsDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OneUICardElevated,
                                    contentColor = OneUITextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = "View Logs",
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "View Logs", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    val nowStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                                    NotificationHelper.showMissedAlarmNotification(
                                        context,
                                        System.currentTimeMillis(),
                                        "Test Missed Alarm",
                                        nowStr
                                    )
                                    Toast.makeText(context, "Test missed alarm notification sent!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OneUICardElevated,
                                    contentColor = OneUITextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Test Missed",
                                    tint = OneUIYellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Test Missed", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // ==========================================
                // SECTION 5: ABOUT CLOCKITY
                // ==========================================
                Text(
                    text = "About",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUITextSecondary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = OneUICardDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "App Name", fontSize = 15.sp, color = OneUITextPrimary)
                            Text(text = "Clockity", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
                        }

                        HorizontalDivider(color = OneUIDivider, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Version", fontSize = 15.sp, color = OneUITextPrimary)
                            Text(text = "1.7.1-rc1", fontSize = 15.sp, color = OneUITextSecondary)
                        }

                        HorizontalDivider(color = OneUIDivider, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Style Theme", fontSize = 15.sp, color = OneUITextPrimary)
                            Text(text = "Samsung One UI Dark", fontSize = 15.sp, color = OneUITextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Confirmation Dialog before Overwrite Restore
    if (showRestoreConfirmDialog && pendingRestoreUri != null) {
        val targetUri = pendingRestoreUri!!
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = OneUIYellow,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Overwrite Existing Data?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary
                )
            },
            text = {
                Text(
                    text = "Restoring from this JSON backup will overwrite and replace all current alarms, alarm groups, world clock cities, and timer presets. Are you sure you want to proceed?",
                    fontSize = 14.sp,
                    color = OneUITextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        isProcessing = true
                        statusMessage = "Importing & scheduling alarms..."
                        coroutineScope.launch {
                            val res = BackupManager.importBackup(context, targetUri)
                            isProcessing = false
                            pendingRestoreUri = null
                            res.onSuccess { msg ->
                                statusMessage = msg
                                Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                statusMessage = "Restore failed: ${err.message}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OneUIRed,
                        contentColor = OneUITextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Overwrite & Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    pendingRestoreUri = null
                }) {
                    Text("Cancel", color = OneUITextSecondary)
                }
            },
            containerColor = OneUICardElevated,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Confirmation Dialog before Factory Reset All Data
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Reset Warning",
                    tint = OneUIRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Reset All Data to Factory Defaults?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary
                )
            },
            text = {
                Text(
                    text = "This will delete all custom alarms, groups, world cities, and timer presets, and restore default initial content. This action cannot be undone.",
                    fontSize = 14.sp,
                    color = OneUITextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        isProcessing = true
                        statusMessage = "Resetting database to defaults..."
                        coroutineScope.launch {
                            ClockityDatabase.resetToFactoryDefaults(context)
                            isProcessing = false
                            statusMessage = "Factory reset completed successfully!"
                            Toast.makeText(context, "All data reset to defaults", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OneUIRed,
                        contentColor = OneUITextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Reset Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = OneUITextSecondary)
                }
            },
            containerColor = OneUICardElevated,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Diagnostic Logs Viewer Dialog
    if (showLogsDialog) {
        LogsViewerDialog(
            accentColor = accentColor,
            onDismiss = { showLogsDialog = false }
        )
    }
}

@Composable
fun LogsViewerDialog(
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val logs by AppLogger.logsFlow.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, searchQuery, selectedLevel) {
        logs.filter { entry ->
            val matchesLevel = when (selectedLevel) {
                "ALL" -> true
                "ERROR" -> entry.level == "E"
                "WARN" -> entry.level == "W"
                "INFO" -> entry.level == "I"
                "DEBUG" -> entry.level == "D"
                else -> true
            }
            val matchesQuery = if (searchQuery.isBlank()) true else {
                entry.message.contains(searchQuery, ignoreCase = true) ||
                entry.tag.contains(searchQuery, ignoreCase = true)
            }
            matchesLevel && matchesQuery
        }.reversed() // Most recent first
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = OneUIBlack,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Diagnostic Logs",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = OneUITextPrimary
                        )
                        Text(
                            text = "${filteredLogs.size} of ${logs.size} entries",
                            fontSize = 12.sp,
                            color = OneUITextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OneUITextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search logs...", fontSize = 13.sp, color = OneUITextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = OneUITextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = OneUITextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = OneUIDivider,
                        focusedContainerColor = OneUICardDark,
                        unfocusedContainerColor = OneUICardDark,
                        focusedTextColor = OneUITextPrimary,
                        unfocusedTextColor = OneUITextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Level Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("ALL", "ERROR", "WARN", "INFO", "DEBUG").forEach { lvl ->
                        val isSelected = selectedLevel == lvl
                        val chipColor = when (lvl) {
                            "ERROR" -> OneUIRed
                            "WARN" -> OneUIYellow
                            "INFO" -> accentColor
                            "DEBUG" -> OneUITextSecondary
                            else -> accentColor
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) chipColor.copy(alpha = 0.25f) else OneUICardDark)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) chipColor else Color.Transparent
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedLevel = lvl }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lvl,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) chipColor else OneUITextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Log List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OneUICardDark)
                        .padding(8.dp)
                ) {
                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (logs.isEmpty()) "No logs captured yet.\nEnable 'Debug Logging' to record events." else "No matching logs found",
                                fontSize = 13.sp,
                                color = OneUITextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        SelectionContainer {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredLogs, key = { it.id }) { entry ->
                                    val levelColor = when (entry.level) {
                                        "E" -> OneUIRed
                                        "W" -> OneUIYellow
                                        "I" -> accentColor
                                        else -> OneUITextSecondary
                                    }
                                    val timeFormatted = remember(entry.timestamp) {
                                        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(entry.timestamp))
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(OneUICardElevated.copy(alpha = 0.5f))
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(levelColor.copy(alpha = 0.2f))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = entry.level,
                                                    color = levelColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Text(
                                                text = timeFormatted,
                                                color = OneUITextSecondary,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = entry.tag,
                                                color = accentColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = entry.message,
                                            color = OneUITextPrimary,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Copy to clipboard
                    Button(
                        onClick = {
                            val formatted = AppLogger.getFormattedLogs()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Clockity Logs", formatted)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OneUICardElevated,
                            contentColor = OneUITextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", fontSize = 12.sp)
                    }

                    // Share logs
                    Button(
                        onClick = {
                            val formatted = AppLogger.getFormattedLogs()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, formatted)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Clockity Logs"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OneUICardElevated,
                            contentColor = OneUITextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = OneUITextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 12.sp)
                    }

                    // Clear logs
                    Button(
                        onClick = {
                            AppLogger.clearLogs()
                            Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OneUIRed.copy(alpha = 0.2f),
                            contentColor = OneUIRed
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = OneUIRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

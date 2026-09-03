package com.clockity.app.ui.settings

import android.graphics.Color.parseColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.data.local.ClockityDatabase
import com.clockity.app.ui.components.OneUIHeader
import com.clockity.app.ui.components.OneUISwitch
import com.clockity.app.ui.theme.*
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
    val lastBackupTimestamp by PreferencesManager.lastBackupTimestamp.collectAsState()

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

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
                            Text(text = "1.6.3", fontSize = 15.sp, color = OneUITextSecondary)
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
}

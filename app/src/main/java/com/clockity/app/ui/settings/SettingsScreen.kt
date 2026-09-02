package com.clockity.app.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.ui.components.OneUIHeader
import com.clockity.app.ui.theme.*
import com.clockity.app.utils.BackupManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

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
            // Header
            OneUIHeader(
                title = "Settings",
                subtitle = "Data backup & preferences"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Backup & Restore
                Text(
                    text = "Backup & Restore",
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
                        Text(
                            text = "Manage your Clockity data with structured JSON backups. Restoring overwrites current alarms and configures active alarms seamlessly.",
                            fontSize = 13.sp,
                            color = OneUITextSecondary,
                            lineHeight = 18.sp
                        )

                        if (isProcessing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = OneUIBlue,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = statusMessage ?: "Processing...",
                                    fontSize = 13.sp,
                                    color = OneUIBlue
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
                            color = OneUIBlue,
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
                            border = BorderStroke(1.dp, OneUIBlue.copy(alpha = 0.5f)),
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
                                    tint = OneUIBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Restore from JSON File",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OneUIBlue
                                )
                            }
                        }
                    }
                }

                // Section: App Information
                Text(
                    text = "About",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUITextSecondary,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
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
                            Text(
                                text = "App Name",
                                fontSize = 15.sp,
                                color = OneUITextPrimary
                            )
                            Text(
                                text = "Clockity",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OneUIBlue
                            )
                        }

                        HorizontalDivider(color = OneUIDivider, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Version",
                                fontSize = 15.sp,
                                color = OneUITextPrimary
                            )
                            Text(
                                text = "1.5.0",
                                fontSize = 15.sp,
                                color = OneUITextSecondary
                            )
                        }

                        HorizontalDivider(color = OneUIDivider, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Theme",
                                fontSize = 15.sp,
                                color = OneUITextPrimary
                            )
                            Text(
                                text = "One UI Dark",
                                fontSize = 15.sp,
                                color = OneUITextSecondary
                            )
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
}

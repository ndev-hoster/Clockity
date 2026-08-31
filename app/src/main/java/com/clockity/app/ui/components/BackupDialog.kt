package com.clockity.app.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.clockity.app.ui.theme.*
import com.clockity.app.utils.BackupManager
import kotlinx.coroutines.launch

@Composable
fun BackupDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

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
            isProcessing = true
            statusMessage = "Importing data..."
            coroutineScope.launch {
                val res = BackupManager.importBackup(context, uri)
                isProcessing = false
                res.onSuccess { msg ->
                    statusMessage = msg
                    Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    statusMessage = "Import failed: ${err.message}"
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = OneUIBlack,
            border = BorderStroke(1.dp, OneUIDivider)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Backup & Restore",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary
                )

                Text(
                    text = "Export all your alarms, groups, world cities, and presets to a JSON file, or restore from a previous backup.",
                    fontSize = 13.sp,
                    color = OneUITextSecondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )

                if (isProcessing) {
                    CircularProgressIndicator(
                        color = OneUIBlue,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(bottom = 16.dp)
                    )
                }

                statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = if (msg.startsWith("Export failed") || msg.startsWith("Import failed")) OneUIRed else OneUIBlue,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = {
                        val defaultFilename = "clockity_backup_${System.currentTimeMillis() / 1000}.json"
                        exportLauncher.launch(defaultFilename)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OneUIBlue,
                        contentColor = OneUIBlack
                    )
                ) {
                    Text("Export Backup to JSON", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OneUIBlue),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OneUIBlue
                    )
                ) {
                    Text("Restore from JSON File", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = OneUITextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

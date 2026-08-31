package com.clockity.app.ui.timer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.clockity.app.data.models.TimerPreset
import com.clockity.app.ui.theme.*

@Composable
fun AddPresetDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, durationSecs: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var minutes by remember { mutableIntStateOf(5) }
    var seconds by remember { mutableIntStateOf(0) }

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
                    .padding(24.dp)
            ) {
                Text(
                    text = "New Timer Preset",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Preset Title (e.g. Pasta, Nap, Workout)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OneUITextPrimary,
                        unfocusedTextColor = OneUITextPrimary,
                        focusedBorderColor = OneUIBlue,
                        unfocusedBorderColor = OneUIDivider
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Duration: ${minutes}m ${seconds}s",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUITextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Minutes Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 3, 5, 10, 15, 30, 45).forEach { m ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (minutes == m && seconds == 0) OneUIBlue else OneUICardElevated)
                                .clickable {
                                    minutes = m
                                    seconds = 0
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${m}m",
                                color = if (minutes == m && seconds == 0) OneUIBlack else OneUITextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Steppers for Custom Minutes & Seconds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minutes
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minutes", fontSize = 12.sp, color = OneUITextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (minutes > 0) minutes-- }) {
                                Text("-", fontSize = 20.sp, color = OneUIBlue, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = String.format("%02d", minutes),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUITextPrimary
                            )
                            IconButton(onClick = { if (minutes < 180) minutes++ }) {
                                Text("+", fontSize = 20.sp, color = OneUIBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Seconds
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Seconds", fontSize = 12.sp, color = OneUITextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (seconds >= 5) seconds -= 5 else seconds = 0 }) {
                                Text("-", fontSize = 20.sp, color = OneUIBlue, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = String.format("%02d", seconds),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUITextPrimary
                            )
                            IconButton(onClick = { if (seconds <= 50) seconds += 5 else seconds = 55 }) {
                                Text("+", fontSize = 20.sp, color = OneUIBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel", color = OneUITextSecondary)
                    }

                    Button(
                        onClick = {
                            val totalSecs = (minutes * 60L) + seconds
                            if (title.isNotBlank() && totalSecs > 0) {
                                onSave(title.trim(), totalSecs)
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank() && ((minutes * 60L) + seconds > 0),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OneUIBlue,
                            contentColor = OneUIBlack
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditPresetDialog(
    preset: TimerPreset,
    onDismiss: () -> Unit,
    onSave: (TimerPreset) -> Unit,
    onDelete: (TimerPreset) -> Unit
) {
    var title by remember { mutableStateOf(preset.title) }
    var minutes by remember { mutableIntStateOf((preset.totalSeconds / 60).toInt()) }
    var seconds by remember { mutableIntStateOf((preset.totalSeconds % 60).toInt()) }

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
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Preset",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextPrimary
                    )

                    IconButton(onClick = {
                        onDelete(preset)
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Preset",
                            tint = OneUIRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Preset Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OneUITextPrimary,
                        unfocusedTextColor = OneUITextPrimary,
                        focusedBorderColor = OneUIBlue,
                        unfocusedBorderColor = OneUIDivider
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Duration: ${minutes}m ${seconds}s",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUITextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Minutes Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 3, 5, 10, 15, 30, 45).forEach { m ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (minutes == m && seconds == 0) OneUIBlue else OneUICardElevated)
                                .clickable {
                                    minutes = m
                                    seconds = 0
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${m}m",
                                color = if (minutes == m && seconds == 0) OneUIBlack else OneUITextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Steppers for Custom Minutes & Seconds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minutes
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minutes", fontSize = 12.sp, color = OneUITextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (minutes > 0) minutes-- }) {
                                Text("-", fontSize = 20.sp, color = OneUIBlue, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = String.format("%02d", minutes),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUITextPrimary
                            )
                            IconButton(onClick = { if (minutes < 180) minutes++ }) {
                                Text("+", fontSize = 20.sp, color = OneUIBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Seconds
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Seconds", fontSize = 12.sp, color = OneUITextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (seconds >= 5) seconds -= 5 else seconds = 0 }) {
                                Text("-", fontSize = 20.sp, color = OneUIBlue, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = String.format("%02d", seconds),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUITextPrimary
                            )
                            IconButton(onClick = { if (seconds <= 50) seconds += 5 else seconds = 55 }) {
                                Text("+", fontSize = 20.sp, color = OneUIBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel", color = OneUITextSecondary)
                    }

                    Button(
                        onClick = {
                            val totalSecs = (minutes * 60L) + seconds
                            if (title.isNotBlank() && totalSecs > 0) {
                                onSave(preset.copy(title = title.trim(), totalSeconds = totalSecs, emoji = ""))
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank() && ((minutes * 60L) + seconds > 0),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OneUIBlue,
                            contentColor = OneUIBlack
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


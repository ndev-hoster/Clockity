package com.clockity.app.ui.alarm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.clockity.app.data.models.AlarmGroup
import com.clockity.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun EditGroupDialog(
    group: AlarmGroup?,
    existingGroups: List<AlarmGroup> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit
) {
    val isNew = group == null
    var name by remember { mutableStateOf(group?.name ?: "") }
    val isDuplicate = existingGroups.any { it.id != group?.id && it.name.equals(name.trim(), ignoreCase = true) }
    val isValid = name.isNotBlank() && !isDuplicate
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(200)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
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
                    .padding(24.dp)
            ) {
                Text(
                    text = if (isNew) "Create Alarm Group" else "Edit Group",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    isError = isDuplicate,
                    supportingText = {
                        if (isDuplicate) {
                            Text("A group with this name already exists", color = OneUIRed, fontSize = 12.sp)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OneUITextPrimary,
                        unfocusedTextColor = OneUITextPrimary,
                        focusedBorderColor = OneUIBlue,
                        unfocusedBorderColor = OneUIDivider,
                        errorBorderColor = OneUIRed
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

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
                            if (isValid) {
                                onSave(name.trim())
                                onDismiss()
                            }
                        },
                        enabled = isValid,
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


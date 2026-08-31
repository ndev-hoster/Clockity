package com.clockity.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_groups")
data class AlarmGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val isExpanded: Boolean = false,
    val colorHex: String = "#3E82F7"
)

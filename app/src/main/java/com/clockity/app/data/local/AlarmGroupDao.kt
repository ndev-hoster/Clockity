package com.clockity.app.data.local

import androidx.room.*
import com.clockity.app.data.models.AlarmGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmGroupDao {

    @Query("SELECT * FROM alarm_groups ORDER BY id ASC")
    fun getAllGroups(): Flow<List<AlarmGroup>>

    @Query("SELECT * FROM alarm_groups ORDER BY id ASC")
    suspend fun getAllGroupsSync(): List<AlarmGroup>

    @Query("SELECT * FROM alarm_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): AlarmGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: AlarmGroup): Long

    @Update
    suspend fun updateGroup(group: AlarmGroup)

    @Delete
    suspend fun deleteGroup(group: AlarmGroup)

    @Query("UPDATE alarm_groups SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setGroupEnabled(id: Long, isEnabled: Boolean)

    @Query("UPDATE alarm_groups SET isExpanded = :isExpanded WHERE id = :id")
    suspend fun setGroupExpanded(id: Long, isExpanded: Boolean)
}

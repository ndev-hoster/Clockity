package com.clockity.app.data.local

import androidx.room.*
import com.clockity.app.data.models.TimerPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerPresetDao {

    @Query("SELECT * FROM timer_presets ORDER BY id ASC")
    fun getAllPresets(): Flow<List<TimerPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: TimerPreset): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(presets: List<TimerPreset>)

    @Query("SELECT COUNT(*) FROM timer_presets")
    suspend fun getCount(): Int

    @Update
    suspend fun updatePreset(preset: TimerPreset)

    @Delete
    suspend fun deletePreset(preset: TimerPreset)
}

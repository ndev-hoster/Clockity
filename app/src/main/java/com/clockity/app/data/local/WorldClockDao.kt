package com.clockity.app.data.local

import androidx.room.*
import com.clockity.app.data.models.WorldCity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldClockDao {

    @Query("SELECT * FROM world_cities ORDER BY displayOrder ASC, id ASC")
    fun getAllCities(): Flow<List<WorldCity>>

    @Query("SELECT * FROM world_cities ORDER BY displayOrder ASC, id ASC")
    suspend fun getAllCitiesSync(): List<WorldCity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: WorldCity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCities(cities: List<WorldCity>)

    @Query("SELECT COUNT(*) FROM world_cities")
    suspend fun getCount(): Int

    @Delete
    suspend fun deleteCity(city: WorldCity)

    @Query("DELETE FROM world_cities WHERE id = :id")
    suspend fun deleteCityById(id: Long)
}

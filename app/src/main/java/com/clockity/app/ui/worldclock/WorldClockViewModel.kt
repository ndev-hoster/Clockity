package com.clockity.app.ui.worldclock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clockity.app.data.local.ClockityDatabase
import com.clockity.app.data.models.WorldCity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime

data class WorldClockUiState(
    val cities: List<WorldCity> = emptyList(),
    val scrubberOffsetHours: Float = 0f,
    val localTime: ZonedDateTime = ZonedDateTime.now()
)

class WorldClockViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ClockityDatabase.getDatabase(application)
    private val worldClockDao = db.worldClockDao()

    private val _scrubberOffset = MutableStateFlow(0f)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (worldClockDao.getCount() == 0) {
                val defaultCities = listOf(
                    WorldCity(cityName = "London", countryName = "United Kingdom", timeZoneId = "Europe/London", displayOrder = 1),
                    WorldCity(cityName = "New York", countryName = "United States", timeZoneId = "America/New_York", displayOrder = 2),
                    WorldCity(cityName = "Tokyo", countryName = "Japan", timeZoneId = "Asia/Tokyo", displayOrder = 3),
                    WorldCity(cityName = "San Francisco", countryName = "United States", timeZoneId = "America/Los_Angeles", displayOrder = 4),
                    WorldCity(cityName = "Sydney", countryName = "Australia", timeZoneId = "Australia/Sydney", displayOrder = 5),
                    WorldCity(cityName = "Paris", countryName = "France", timeZoneId = "Europe/Paris", displayOrder = 6),
                    WorldCity(cityName = "Dubai", countryName = "United Arab Emirates", timeZoneId = "Asia/Dubai", displayOrder = 7)
                )
                worldClockDao.insertCities(defaultCities)
            }
        }
    }

    val uiState: StateFlow<WorldClockUiState> = combine(
        worldClockDao.getAllCities(),
        _scrubberOffset
    ) { cities, offset ->
        WorldClockUiState(
            cities = cities,
            scrubberOffsetHours = offset,
            localTime = ZonedDateTime.now(ZoneId.systemDefault()).plusMinutes((offset * 60).toLong())
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorldClockUiState()
    )

    fun setScrubberOffset(hours: Float) {
        _scrubberOffset.value = hours
    }

    fun resetScrubber() {
        _scrubberOffset.value = 0f
    }

    fun addCity(cityName: String, countryName: String, timeZoneId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val city = WorldCity(
                cityName = cityName,
                countryName = countryName,
                timeZoneId = timeZoneId,
                displayOrder = (uiState.value.cities.maxOfOrNull { it.displayOrder } ?: 0) + 1
            )
            worldClockDao.insertCity(city)
        }
    }

    fun deleteCity(city: WorldCity) {
        viewModelScope.launch(Dispatchers.IO) {
            worldClockDao.deleteCity(city)
        }
    }
}

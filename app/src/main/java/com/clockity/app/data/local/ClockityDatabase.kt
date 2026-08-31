package com.clockity.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.clockity.app.data.models.Alarm
import com.clockity.app.data.models.AlarmGroup
import com.clockity.app.data.models.TimerPreset
import com.clockity.app.data.models.WorldCity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Alarm::class,
        AlarmGroup::class,
        WorldCity::class,
        TimerPreset::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ClockityDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
    abstract fun alarmGroupDao(): AlarmGroupDao
    abstract fun worldClockDao(): WorldClockDao
    abstract fun timerPresetDao(): TimerPresetDao

    companion object {
        @Volatile
        private var INSTANCE: ClockityDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): ClockityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClockityDatabase::class.java,
                    "clockity_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(db: ClockityDatabase) {
            val groupDao = db.alarmGroupDao()
            val alarmDao = db.alarmDao()
            val worldClockDao = db.worldClockDao()
            val timerPresetDao = db.timerPresetDao()

            // 1. Seed Alarm Groups (Using Blue and Yellow)
            val workGroupId = groupDao.insertGroup(
                AlarmGroup(name = "Workdays", isEnabled = true, isExpanded = true, colorHex = "#3E82F7")
            )
            val gymGroupId = groupDao.insertGroup(
                AlarmGroup(name = "Fitness & Gym", isEnabled = true, isExpanded = true, colorHex = "#FFD60A")
            )

            // 2. Seed Default Alarms (Bitmask: 0b01111100 = Mon-Fri)
            alarmDao.insertAlarm(
                Alarm(
                    hour = 6,
                    minute = 30,
                    label = "Morning Wake up",
                    isEnabled = true,
                    daysOfWeek = 0b01111100, // Mon to Fri
                    groupId = workGroupId,
                    isGentleWakeUp = true
                )
            )
            alarmDao.insertAlarm(
                Alarm(
                    hour = 7,
                    minute = 15,
                    label = "Leave for Office",
                    isEnabled = true,
                    daysOfWeek = 0b01111100,
                    groupId = workGroupId,
                    isGentleWakeUp = false
                )
            )
            alarmDao.insertAlarm(
                Alarm(
                    hour = 18,
                    minute = 0,
                    label = "Evening Workout",
                    isEnabled = true,
                    daysOfWeek = 0b01111100,
                    groupId = gymGroupId,
                    isGentleWakeUp = false
                )
            )

            // 3. Seed Default World Cities
            worldClockDao.insertCity(
                WorldCity(cityName = "London", countryName = "United Kingdom", timeZoneId = "Europe/London", displayOrder = 1)
            )
            worldClockDao.insertCity(
                WorldCity(cityName = "New York", countryName = "United States", timeZoneId = "America/New_York", displayOrder = 2)
            )
            worldClockDao.insertCity(
                WorldCity(cityName = "Tokyo", countryName = "Japan", timeZoneId = "Asia/Tokyo", displayOrder = 3)
            )
            worldClockDao.insertCity(
                WorldCity(cityName = "San Francisco", countryName = "United States", timeZoneId = "America/Los_Angeles", displayOrder = 4)
            )
            worldClockDao.insertCity(
                WorldCity(cityName = "Sydney", countryName = "Australia", timeZoneId = "Australia/Sydney", displayOrder = 5)
            )

            // 4. Seed Default Timer Presets without emojis
            val presets = listOf(
                TimerPreset(title = "Boil Eggs", totalSeconds = 480, emoji = ""),
                TimerPreset(title = "Green Tea", totalSeconds = 180, emoji = ""),
                TimerPreset(title = "Power Nap", totalSeconds = 1200, emoji = ""),
                TimerPreset(title = "HIIT Interval", totalSeconds = 45, emoji = ""),
                TimerPreset(title = "Baking", totalSeconds = 900, emoji = ""),
                TimerPreset(title = "Laundry", totalSeconds = 2700, emoji = "")
            )
            timerPresetDao.insertAll(presets)
        }
    }
}

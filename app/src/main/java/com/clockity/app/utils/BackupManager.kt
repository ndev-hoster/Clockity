package com.clockity.app.utils

import android.content.Context
import android.net.Uri
import com.clockity.app.data.local.ClockityDatabase
import com.clockity.app.data.models.Alarm
import com.clockity.app.data.models.AlarmGroup
import com.clockity.app.data.models.TimerPreset
import com.clockity.app.data.models.WorldCity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object BackupManager {

    suspend fun exportBackup(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val db = ClockityDatabase.getDatabase(context)
            val alarms = db.alarmDao().getAllAlarmsSync()
            val groups = db.alarmGroupDao().getAllGroupsSync()
            val cities = db.worldClockDao().getAllCitiesSync()
            val presets = db.timerPresetDao().getAllPresetsSync()

            val rootJson = JSONObject().apply {
                put("app", "Clockity")
                put("version", 1)
                put("timestamp", System.currentTimeMillis())

                // Groups
                val groupsArray = JSONArray()
                groups.forEach { g ->
                    groupsArray.put(JSONObject().apply {
                        put("id", g.id)
                        put("name", g.name)
                        put("colorHex", g.colorHex)
                        put("isEnabled", g.isEnabled)
                        put("isExpanded", g.isExpanded)
                    })
                }
                put("groups", groupsArray)

                // Alarms
                val alarmsArray = JSONArray()
                alarms.forEach { a ->
                    alarmsArray.put(JSONObject().apply {
                        put("id", a.id)
                        put("hour", a.hour)
                        put("minute", a.minute)
                        put("label", a.label)
                        put("isEnabled", a.isEnabled)
                        put("daysOfWeek", a.daysOfWeek)
                        put("specificDateMillis", a.specificDateMillis ?: -1L)
                        put("groupId", a.groupId ?: -1L)
                        put("snoozeDurationMinutes", a.snoozeDurationMinutes)
                        put("snoozeRepeatCount", a.snoozeRepeatCount)
                        put("isGentleWakeUp", a.isGentleWakeUp)
                        put("vibrationPattern", a.vibrationPattern)
                        put("soundTitle", a.soundTitle)
                        put("soundUri", a.soundUri)
                    })
                }
                put("alarms", alarmsArray)

                // Cities
                val citiesArray = JSONArray()
                cities.forEach { c ->
                    citiesArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("cityName", c.cityName)
                        put("countryName", c.countryName)
                        put("timeZoneId", c.timeZoneId)
                        put("displayOrder", c.displayOrder)
                    })
                }
                put("cities", citiesArray)

                // Presets
                val presetsArray = JSONArray()
                presets.forEach { p ->
                    presetsArray.put(JSONObject().apply {
                        put("id", p.id)
                        put("title", p.title)
                        put("totalSeconds", p.totalSeconds)
                        put("emoji", p.emoji)
                    })
                }
                put("timerPresets", presetsArray)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.failure(Exception("Unable to open output stream"))

            Result.success("Exported ${alarms.size} alarms, ${groups.size} groups, ${cities.size} cities, ${presets.size} presets.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackup(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        jsonString.append(line)
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.failure(Exception("Unable to open input stream"))

            val rootJson = JSONObject(jsonString.toString())
            if (!rootJson.has("app") || rootJson.optString("app") != "Clockity") {
                return@withContext Result.failure(Exception("Invalid backup file: Not a Clockity backup"))
            }

            val db = ClockityDatabase.getDatabase(context)

            // Import Groups
            val existingGroups = db.alarmGroupDao().getAllGroupsSync()
            val groupsArray = rootJson.optJSONArray("groups") ?: JSONArray()
            var importedGroups = 0
            for (i in 0 until groupsArray.length()) {
                val gJson = groupsArray.getJSONObject(i)
                val name = gJson.getString("name").trim()
                if (existingGroups.none { it.name.equals(name, ignoreCase = true) }) {
                    val newGroup = AlarmGroup(
                        name = name,
                        colorHex = gJson.optString("colorHex", "#3E82F7"),
                        isEnabled = gJson.optBoolean("isEnabled", true),
                        isExpanded = gJson.optBoolean("isExpanded", false)
                    )
                    db.alarmGroupDao().insertGroup(newGroup)
                    importedGroups++
                }
            }

            // Refresh groups mapping
            val currentGroups = db.alarmGroupDao().getAllGroupsSync()

            // Import Alarms
            val existingAlarms = db.alarmDao().getAllAlarmsSync()
            val alarmsArray = rootJson.optJSONArray("alarms") ?: JSONArray()
            var importedAlarms = 0
            for (i in 0 until alarmsArray.length()) {
                val aJson = alarmsArray.getJSONObject(i)
                val hour = aJson.getInt("hour")
                val minute = aJson.getInt("minute")
                val daysOfWeek = aJson.optInt("daysOfWeek", 0)
                val rawDate = aJson.optLong("specificDateMillis", -1L)
                val specificDateMillis = if (rawDate > 0) rawDate else null
                val label = aJson.optString("label", "Alarm")

                val isDuplicate = existingAlarms.any {
                    it.hour == hour && it.minute == minute && it.daysOfWeek == daysOfWeek && it.specificDateMillis == specificDateMillis
                }

                if (!isDuplicate) {
                    val rawGroupId = aJson.optLong("groupId", -1L)
                    val matchedGroup = if (rawGroupId > 0) currentGroups.firstOrNull()?.id else null

                    val alarm = Alarm(
                        hour = hour,
                        minute = minute,
                        label = label,
                        isEnabled = aJson.optBoolean("isEnabled", true),
                        daysOfWeek = daysOfWeek,
                        specificDateMillis = specificDateMillis,
                        groupId = matchedGroup,
                        snoozeDurationMinutes = aJson.optInt("snoozeDurationMinutes", 5),
                        snoozeRepeatCount = aJson.optInt("snoozeRepeatCount", 3),
                        isGentleWakeUp = aJson.optBoolean("isGentleWakeUp", true),
                        vibrationPattern = aJson.optString("vibrationPattern", "Basic"),
                        soundTitle = aJson.optString("soundTitle", "One UI Bell"),
                        soundUri = aJson.optString("soundUri", "default")
                    )
                    db.alarmDao().insertAlarm(alarm)
                    importedAlarms++
                }
            }

            // Import Cities
            val existingCities = db.worldClockDao().getAllCitiesSync()
            val citiesArray = rootJson.optJSONArray("cities") ?: JSONArray()
            var importedCities = 0
            for (i in 0 until citiesArray.length()) {
                val cJson = citiesArray.getJSONObject(i)
                val cityName = cJson.getString("cityName")
                val tzId = cJson.getString("timeZoneId")

                if (existingCities.none { it.cityName.equals(cityName, ignoreCase = true) || it.timeZoneId == tzId }) {
                    val city = WorldCity(
                        cityName = cityName,
                        countryName = cJson.optString("countryName", ""),
                        timeZoneId = tzId,
                        displayOrder = cJson.optInt("displayOrder", existingCities.size + importedCities)
                    )
                    db.worldClockDao().insertCity(city)
                    importedCities++
                }
            }

            // Import Presets
            val existingPresets = db.timerPresetDao().getAllPresetsSync()
            val presetsArray = rootJson.optJSONArray("timerPresets") ?: JSONArray()
            var importedPresets = 0
            for (i in 0 until presetsArray.length()) {
                val pJson = presetsArray.getJSONObject(i)
                val title = pJson.getString("title")
                val totalSeconds = pJson.getLong("totalSeconds")

                if (existingPresets.none { it.title.equals(title, ignoreCase = true) && it.totalSeconds == totalSeconds }) {
                    val preset = TimerPreset(
                        title = title,
                        totalSeconds = totalSeconds,
                        emoji = pJson.optString("emoji", "")
                    )
                    db.timerPresetDao().insertPreset(preset)
                    importedPresets++
                }
            }

            Result.success("Successfully imported $importedAlarms alarms, $importedGroups groups, $importedCities cities, $importedPresets timer presets.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

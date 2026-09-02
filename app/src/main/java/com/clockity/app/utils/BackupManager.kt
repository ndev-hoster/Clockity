package com.clockity.app.utils

import android.content.Context
import android.net.Uri
import android.os.Build
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object BackupManager {

    private const val SCHEMA_VERSION = 2

    /**
     * Exports full app data to a structured, hierarchical JSON schema (v2).
     */
    suspend fun exportBackup(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val db = ClockityDatabase.getDatabase(context)
            val alarms = db.alarmDao().getAllAlarmsSync()
            val groups = db.alarmGroupDao().getAllGroupsSync()
            val cities = db.worldClockDao().getAllCitiesSync()
            val presets = db.timerPresetDao().getAllPresetsSync()

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val nowMillis = System.currentTimeMillis()

            fun serializeAlarm(alarm: Alarm): JSONObject {
                val (timeStr, amPm) = alarm.formatTime12H()
                return JSONObject().apply {
                    put("id", alarm.id)
                    put("time", JSONObject().apply {
                        put("hour", alarm.hour)
                        put("minute", alarm.minute)
                        put("formatted", "$timeStr $amPm")
                    })
                    put("label", alarm.label)
                    put("isEnabled", alarm.isEnabled)
                    put("schedule", JSONObject().apply {
                        put("daysOfWeek", alarm.daysOfWeek)
                        put("daysFormatted", alarm.formatDaysSummary())
                        put("specificDateMillis", alarm.specificDateMillis ?: -1L)
                    })
                    put("audio", JSONObject().apply {
                        put("soundTitle", alarm.soundTitle)
                        put("soundUri", alarm.soundUri)
                        put("isGentleWakeUp", alarm.isGentleWakeUp)
                        put("vibrationPattern", alarm.vibrationPattern)
                    })
                    put("snooze", JSONObject().apply {
                        put("durationMinutes", alarm.snoozeDurationMinutes)
                        put("repeatCount", alarm.snoozeRepeatCount)
                    })
                }
            }

            val rootJson = JSONObject().apply {
                put("\$schema", "https://clockity.app/schemas/backup-v2.json")

                // 1. Metadata
                put("metadata", JSONObject().apply {
                    put("app", "Clockity")
                    put("schemaVersion", SCHEMA_VERSION)
                    put("appVersion", "1.5.0")
                    put("createdAt", isoFormat.format(Date(nowMillis)))
                    put("timestamp", nowMillis)
                    put("device", JSONObject().apply {
                        put("model", Build.MODEL ?: "Unknown")
                        put("sdkInt", Build.VERSION.SDK_INT)
                    })
                    put("counts", JSONObject().apply {
                        put("alarms", alarms.size)
                        put("groups", groups.size)
                        put("worldCities", cities.size)
                        put("timerPresets", presets.size)
                    })
                })

                // 2. Data Payload
                put("data", JSONObject().apply {
                    // Alarms & Nested Groups
                    put("alarms", JSONObject().apply {
                        val groupsArray = JSONArray()
                        groups.forEach { group ->
                            val memberAlarms = alarms.filter { it.groupId == group.id }
                            val groupObj = JSONObject().apply {
                                put("id", group.id)
                                put("name", group.name)
                                put("colorHex", group.colorHex)
                                put("isEnabled", group.isEnabled)
                                put("isExpanded", group.isExpanded)

                                val groupAlarmsArray = JSONArray()
                                memberAlarms.forEach { groupAlarmsArray.put(serializeAlarm(it)) }
                                put("alarms", groupAlarmsArray)
                            }
                            groupsArray.put(groupObj)
                        }
                        put("groups", groupsArray)

                        val ungroupedAlarms = alarms.filter { it.groupId == null }
                        val ungroupedArray = JSONArray()
                        ungroupedAlarms.forEach { ungroupedArray.put(serializeAlarm(it)) }
                        put("ungrouped", ungroupedArray)
                    })

                    // World Clock
                    put("worldClock", JSONObject().apply {
                        val citiesArray = JSONArray()
                        cities.forEach { city ->
                            citiesArray.put(JSONObject().apply {
                                put("id", city.id)
                                put("cityName", city.cityName)
                                put("countryName", city.countryName)
                                put("timeZoneId", city.timeZoneId)
                                put("displayOrder", city.displayOrder)
                            })
                        }
                        put("cities", citiesArray)
                    })

                    // Timers
                    put("timers", JSONObject().apply {
                        val presetsArray = JSONArray()
                        presets.forEach { preset ->
                            presetsArray.put(JSONObject().apply {
                                put("id", preset.id)
                                put("title", preset.title)
                                put("totalSeconds", preset.totalSeconds)
                                put("emoji", preset.emoji)
                            })
                        }
                        put("presets", presetsArray)
                    })
                })
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.failure(Exception("Unable to open output stream"))

            PreferencesManager.setLastBackupTimestamp(context, nowMillis)
            Result.success("Exported ${alarms.size} alarms across ${groups.size} groups, ${cities.size} cities, and ${presets.size} presets.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports data with complete OVERWRITE and proper group ID mapping,
     * supporting both structured v2 and legacy flat v1 backups.
     */
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

            // Validate Clockity backup
            val isV2 = rootJson.has("data")
            val isV1 = rootJson.has("app") && rootJson.optString("app") == "Clockity"
            if (!isV2 && !isV1) {
                return@withContext Result.failure(Exception("Invalid backup file: Not a valid Clockity backup"))
            }

            val db = ClockityDatabase.getDatabase(context)

            // Step 1: Cancel all existing scheduled alarms in AlarmManager before wipe
            val existingAlarms = db.alarmDao().getAllAlarmsSync()
            existingAlarms.forEach { alarm ->
                AlarmScheduler.cancelAlarm(context, alarm.id)
            }

            // Step 2: Wipe all existing records (True OVERWRITE)
            db.alarmDao().deleteAll()
            db.alarmGroupDao().deleteAll()
            db.worldClockDao().deleteAll()
            db.timerPresetDao().deleteAll()

            var importedAlarms = 0
            var importedGroups = 0
            var importedCities = 0
            var importedPresets = 0

            if (isV2) {
                // Parse Structured Schema v2
                val dataObj = rootJson.getJSONObject("data")

                // A. Groups and their nested member alarms
                val alarmsObj = dataObj.optJSONObject("alarms")
                if (alarmsObj != null) {
                    val groupsArray = alarmsObj.optJSONArray("groups") ?: JSONArray()
                    for (i in 0 until groupsArray.length()) {
                        val groupObj = groupsArray.getJSONObject(i)
                        val newGroup = AlarmGroup(
                            name = groupObj.getString("name").trim(),
                            colorHex = groupObj.optString("colorHex", "#3E82F7"),
                            isEnabled = groupObj.optBoolean("isEnabled", true),
                            isExpanded = groupObj.optBoolean("isExpanded", true)
                        )
                        val newGroupId = db.alarmGroupDao().insertGroup(newGroup)
                        importedGroups++

                        // Member alarms
                        val memberAlarmsArray = groupObj.optJSONArray("alarms") ?: JSONArray()
                        for (j in 0 until memberAlarmsArray.length()) {
                            val alarmObj = memberAlarmsArray.getJSONObject(j)
                            val alarm = parseV2Alarm(alarmObj, newGroupId)
                            val insertedId = db.alarmDao().insertAlarm(alarm)
                            val insertedAlarm = alarm.copy(id = insertedId)
                            if (insertedAlarm.isEnabled) {
                                AlarmScheduler.scheduleAlarm(context, insertedAlarm)
                            }
                            importedAlarms++
                        }
                    }

                    // Ungrouped alarms
                    val ungroupedArray = alarmsObj.optJSONArray("ungrouped") ?: JSONArray()
                    for (i in 0 until ungroupedArray.length()) {
                        val alarmObj = ungroupedArray.getJSONObject(i)
                        val alarm = parseV2Alarm(alarmObj, null)
                        val insertedId = db.alarmDao().insertAlarm(alarm)
                        val insertedAlarm = alarm.copy(id = insertedId)
                        if (insertedAlarm.isEnabled) {
                            AlarmScheduler.scheduleAlarm(context, insertedAlarm)
                        }
                        importedAlarms++
                    }
                }

                // B. World Clock Cities
                val worldClockObj = dataObj.optJSONObject("worldClock")
                if (worldClockObj != null) {
                    val citiesArray = worldClockObj.optJSONArray("cities") ?: JSONArray()
                    for (i in 0 until citiesArray.length()) {
                        val cJson = citiesArray.getJSONObject(i)
                        val city = WorldCity(
                            cityName = cJson.getString("cityName"),
                            countryName = cJson.optString("countryName", ""),
                            timeZoneId = cJson.getString("timeZoneId"),
                            displayOrder = cJson.optInt("displayOrder", i)
                        )
                        db.worldClockDao().insertCity(city)
                        importedCities++
                    }
                }

                // C. Timer Presets
                val timersObj = dataObj.optJSONObject("timers")
                if (timersObj != null) {
                    val presetsArray = timersObj.optJSONArray("presets") ?: JSONArray()
                    for (i in 0 until presetsArray.length()) {
                        val pJson = presetsArray.getJSONObject(i)
                        val preset = TimerPreset(
                            title = pJson.getString("title"),
                            totalSeconds = pJson.getLong("totalSeconds"),
                            emoji = pJson.optString("emoji", "")
                        )
                        db.timerPresetDao().insertPreset(preset)
                        importedPresets++
                    }
                }

            } else {
                // Parse Legacy Flat Schema v1 with Group ID Mapping
                val groupIdMap = mutableMapOf<Long, Long>()

                val groupsArray = rootJson.optJSONArray("groups") ?: JSONArray()
                for (i in 0 until groupsArray.length()) {
                    val gJson = groupsArray.getJSONObject(i)
                    val oldId = gJson.optLong("id", -1L)
                    val newGroup = AlarmGroup(
                        name = gJson.getString("name").trim(),
                        colorHex = gJson.optString("colorHex", "#3E82F7"),
                        isEnabled = gJson.optBoolean("isEnabled", true),
                        isExpanded = gJson.optBoolean("isExpanded", false)
                    )
                    val newId = db.alarmGroupDao().insertGroup(newGroup)
                    if (oldId > 0) {
                        groupIdMap[oldId] = newId
                    }
                    importedGroups++
                }

                val alarmsArray = rootJson.optJSONArray("alarms") ?: JSONArray()
                for (i in 0 until alarmsArray.length()) {
                    val aJson = alarmsArray.getJSONObject(i)
                    val rawDate = aJson.optLong("specificDateMillis", -1L)
                    val specificDateMillis = if (rawDate > 0) rawDate else null
                    val oldGroupId = aJson.optLong("groupId", -1L)
                    val mappedGroupId = if (oldGroupId > 0) groupIdMap[oldGroupId] else null

                    val alarm = Alarm(
                        hour = aJson.getInt("hour"),
                        minute = aJson.getInt("minute"),
                        label = aJson.optString("label", "Alarm"),
                        isEnabled = aJson.optBoolean("isEnabled", true),
                        daysOfWeek = aJson.optInt("daysOfWeek", 0),
                        specificDateMillis = specificDateMillis,
                        groupId = mappedGroupId,
                        snoozeDurationMinutes = aJson.optInt("snoozeDurationMinutes", 5),
                        snoozeRepeatCount = aJson.optInt("snoozeRepeatCount", 3),
                        isGentleWakeUp = aJson.optBoolean("isGentleWakeUp", true),
                        vibrationPattern = aJson.optString("vibrationPattern", "Basic"),
                        soundTitle = aJson.optString("soundTitle", "One UI Bell"),
                        soundUri = aJson.optString("soundUri", "default")
                    )
                    val insertedId = db.alarmDao().insertAlarm(alarm)
                    val insertedAlarm = alarm.copy(id = insertedId)
                    if (insertedAlarm.isEnabled) {
                        AlarmScheduler.scheduleAlarm(context, insertedAlarm)
                    }
                    importedAlarms++
                }

                val citiesArray = rootJson.optJSONArray("cities") ?: JSONArray()
                for (i in 0 until citiesArray.length()) {
                    val cJson = citiesArray.getJSONObject(i)
                    val city = WorldCity(
                        cityName = cJson.getString("cityName"),
                        countryName = cJson.optString("countryName", ""),
                        timeZoneId = cJson.getString("timeZoneId"),
                        displayOrder = cJson.optInt("displayOrder", i)
                    )
                    db.worldClockDao().insertCity(city)
                    importedCities++
                }

                val presetsArray = rootJson.optJSONArray("timerPresets") ?: JSONArray()
                for (i in 0 until presetsArray.length()) {
                    val pJson = presetsArray.getJSONObject(i)
                    val preset = TimerPreset(
                        title = pJson.getString("title"),
                        totalSeconds = pJson.getLong("totalSeconds"),
                        emoji = pJson.optString("emoji", "")
                    )
                    db.timerPresetDao().insertPreset(preset)
                    importedPresets++
                }
            }

            Result.success("Restored $importedAlarms alarms across $importedGroups groups, $importedCities world cities, and $importedPresets timer presets.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseV2Alarm(alarmObj: JSONObject, groupId: Long?): Alarm {
        val timeObj = alarmObj.optJSONObject("time")
        val hour = timeObj?.optInt("hour") ?: alarmObj.optInt("hour", 7)
        val minute = timeObj?.optInt("minute") ?: alarmObj.optInt("minute", 0)

        val scheduleObj = alarmObj.optJSONObject("schedule")
        var daysOfWeek = scheduleObj?.optInt("daysOfWeek") ?: alarmObj.optInt("daysOfWeek", 0)

        // Smart normalization for manual 0-indexed or 1-indexed bitmasks:
        if (daysOfWeek == 127) {
            daysOfWeek = 254 // 0b11111110 (Every day)
        } else if (daysOfWeek == 31) {
            daysOfWeek = 62 // 0b01111100 (Weekdays)
        } else if (daysOfWeek == 96) {
            daysOfWeek = 192 // 0b11000000 (Weekends)
        } else if ((daysOfWeek and 1) != 0 && daysOfWeek <= 127) {
            // 0-indexed bitmask shifted to 1-indexed
            daysOfWeek = (daysOfWeek shl 1) and 0b11111110
        }

        val rawDate = scheduleObj?.optLong("specificDateMillis", -1L) ?: alarmObj.optLong("specificDateMillis", -1L)
        val specificDate = if (rawDate > 0) rawDate else null

        val audioObj = alarmObj.optJSONObject("audio")
        val soundTitle = audioObj?.optString("soundTitle") ?: alarmObj.optString("soundTitle", "One UI Bell")
        val soundUri = audioObj?.optString("soundUri") ?: alarmObj.optString("soundUri", "default")
        val isGentleWakeUp = audioObj?.optBoolean("isGentleWakeUp") ?: alarmObj.optBoolean("isGentleWakeUp", true)
        val vibrationPattern = audioObj?.optString("vibrationPattern") ?: alarmObj.optString("vibrationPattern", "Basic")

        val snoozeObj = alarmObj.optJSONObject("snooze")
        val snoozeDurationMinutes = snoozeObj?.optInt("durationMinutes") ?: alarmObj.optInt("snoozeDurationMinutes", 5)
        val snoozeRepeatCount = snoozeObj?.optInt("repeatCount") ?: alarmObj.optInt("snoozeRepeatCount", 3)

        return Alarm(
            hour = hour,
            minute = minute,
            label = alarmObj.optString("label", "Alarm"),
            isEnabled = alarmObj.optBoolean("isEnabled", true),
            daysOfWeek = daysOfWeek,
            specificDateMillis = specificDate,
            groupId = groupId,
            snoozeDurationMinutes = snoozeDurationMinutes,
            snoozeRepeatCount = snoozeRepeatCount,
            isGentleWakeUp = isGentleWakeUp,
            vibrationPattern = vibrationPattern,
            soundTitle = soundTitle,
            soundUri = soundUri
        )
    }
}

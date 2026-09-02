package com.clockity.app.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PreferencesManager {

    private const val PREFS_NAME = "clockity_preferences"

    private const val KEY_ACCENT_COLOR = "accent_color"
    private const val KEY_AMOLED_BLACK = "amoled_black"
    private const val KEY_TIMER_FLASH = "timer_flash"
    private const val KEY_VOLUME_BEHAVIOR = "volume_behavior"
    private const val KEY_DEFAULT_SNOOZE_MINS = "default_snooze_mins"
    private const val KEY_DEFAULT_SNOOZE_COUNT = "default_snooze_count"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"

    // Default Values
    const val DEFAULT_ACCENT_COLOR = "#3E82F7" // One UI Blue
    const val DEFAULT_VOLUME_BEHAVIOR = "Snooze"

    private val _accentColorHex = MutableStateFlow(DEFAULT_ACCENT_COLOR)
    val accentColorHex: StateFlow<String> = _accentColorHex.asStateFlow()

    private val _isAmoledBlack = MutableStateFlow(false)
    val isAmoledBlack: StateFlow<Boolean> = _isAmoledBlack.asStateFlow()

    private val _isTimerFlashEnabled = MutableStateFlow(true)
    val isTimerFlashEnabled: StateFlow<Boolean> = _isTimerFlashEnabled.asStateFlow()

    private val _volumeKeyBehavior = MutableStateFlow(DEFAULT_VOLUME_BEHAVIOR)
    val volumeKeyBehavior: StateFlow<String> = _volumeKeyBehavior.asStateFlow()

    private val _defaultSnoozeMins = MutableStateFlow(5)
    val defaultSnoozeMins: StateFlow<Int> = _defaultSnoozeMins.asStateFlow()

    private val _defaultSnoozeRepeatCount = MutableStateFlow(3)
    val defaultSnoozeRepeatCount: StateFlow<Int> = _defaultSnoozeRepeatCount.asStateFlow()

    private val _lastBackupTimestamp = MutableStateFlow(0L)
    val lastBackupTimestamp: StateFlow<Long> = _lastBackupTimestamp.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        val prefs = getPrefs(context)
        _accentColorHex.value = prefs.getString(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR) ?: DEFAULT_ACCENT_COLOR
        _isAmoledBlack.value = prefs.getBoolean(KEY_AMOLED_BLACK, false)
        _isTimerFlashEnabled.value = prefs.getBoolean(KEY_TIMER_FLASH, true)
        _volumeKeyBehavior.value = prefs.getString(KEY_VOLUME_BEHAVIOR, DEFAULT_VOLUME_BEHAVIOR) ?: DEFAULT_VOLUME_BEHAVIOR
        _defaultSnoozeMins.value = prefs.getInt(KEY_DEFAULT_SNOOZE_MINS, 5)
        _defaultSnoozeRepeatCount.value = prefs.getInt(KEY_DEFAULT_SNOOZE_COUNT, 3)
        _lastBackupTimestamp.value = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    fun setAccentColor(context: Context, hex: String) {
        _accentColorHex.value = hex
        getPrefs(context).edit().putString(KEY_ACCENT_COLOR, hex).apply()
    }

    fun setAmoledBlack(context: Context, enabled: Boolean) {
        _isAmoledBlack.value = enabled
        getPrefs(context).edit().putBoolean(KEY_AMOLED_BLACK, enabled).apply()
    }

    fun setTimerFlashEnabled(context: Context, enabled: Boolean) {
        _isTimerFlashEnabled.value = enabled
        getPrefs(context).edit().putBoolean(KEY_TIMER_FLASH, enabled).apply()
    }

    fun setVolumeKeyBehavior(context: Context, behavior: String) {
        _volumeKeyBehavior.value = behavior
        getPrefs(context).edit().putString(KEY_VOLUME_BEHAVIOR, behavior).apply()
    }

    fun getVolumeKeyBehavior(context: Context): String {
        return getPrefs(context).getString(KEY_VOLUME_BEHAVIOR, DEFAULT_VOLUME_BEHAVIOR) ?: DEFAULT_VOLUME_BEHAVIOR
    }

    fun setDefaultSnoozeMins(context: Context, mins: Int) {
        _defaultSnoozeMins.value = mins
        getPrefs(context).edit().putInt(KEY_DEFAULT_SNOOZE_MINS, mins).apply()
    }

    fun setDefaultSnoozeRepeatCount(context: Context, count: Int) {
        _defaultSnoozeRepeatCount.value = count
        getPrefs(context).edit().putInt(KEY_DEFAULT_SNOOZE_COUNT, count).apply()
    }

    fun setLastBackupTimestamp(context: Context, timestamp: Long) {
        _lastBackupTimestamp.value = timestamp
        getPrefs(context).edit().putLong(KEY_LAST_BACKUP_TIME, timestamp).apply()
    }
}

package com.zjdavis.volimiter

import android.content.Context
import androidx.core.content.edit

data class LimiterConfig(
    val maxVolume: Int = VolimiterSettings.DEFAULT_MAX_VOLUME,
    val quietHoursEnabled: Boolean = false,
    val quietHoursVolume: Int = VolimiterSettings.DEFAULT_QUIET_VOLUME,
    val quietHoursStartMinutes: Int = VolimiterSettings.DEFAULT_QUIET_START_MINUTES,
    val quietHoursEndMinutes: Int = VolimiterSettings.DEFAULT_QUIET_END_MINUTES
)

object VolimiterSettings {
    const val DEFAULT_MAX_VOLUME = 5
    const val DEFAULT_QUIET_VOLUME = 0
    const val DEFAULT_QUIET_START_MINUTES = 22 * 60
    const val DEFAULT_QUIET_END_MINUTES = 7 * 60
    const val EXTRA_MAX_VOLUME = "com.zjdavis.volimiter.extra.MAX_VOLUME"

    private const val APP_PREFS = "volimiter"
    private const val BOOT_PREFS = "volimiter_boot"
    private const val KEY_MAX_VOLUME = "max_volume"
    private const val KEY_PENDING_VOLUME = "pending_volume"
    private const val KEY_WAS_RUNNING = "was_running"
    private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
    private const val KEY_QUIET_ENABLED = "quiet_hours_enabled"
    private const val KEY_QUIET_VOLUME = "quiet_hours_volume"
    private const val KEY_QUIET_START = "quiet_hours_start_minutes"
    private const val KEY_QUIET_END = "quiet_hours_end_minutes"

    private fun appPreferences(context: Context) =
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)

    private fun bootPreferences(context: Context) =
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(BOOT_PREFS, Context.MODE_PRIVATE)

    fun getConfig(context: Context): LimiterConfig {
        val prefs = appPreferences(context)
        return readConfig(prefs::getInt, prefs::getBoolean)
    }

    fun saveConfig(context: Context, config: LimiterConfig) {
        appPreferences(context).edit { writeConfig(config) }
    }

    fun getPendingVolume(context: Context): Int =
        appPreferences(context).getInt(KEY_PENDING_VOLUME, DEFAULT_MAX_VOLUME)

    fun setPendingVolume(context: Context, volume: Int) {
        appPreferences(context).edit { putInt(KEY_PENDING_VOLUME, volume) }
    }

    fun hasSeenOnboarding(context: Context): Boolean =
        appPreferences(context).getBoolean(KEY_HAS_SEEN_ONBOARDING, false)

    fun markOnboardingSeen(context: Context) {
        appPreferences(context).edit { putBoolean(KEY_HAS_SEEN_ONBOARDING, true) }
    }

    fun isLimiterActive(context: Context): Boolean =
        bootPreferences(context).getBoolean(KEY_WAS_RUNNING, false)

    fun getBootConfig(context: Context): LimiterConfig {
        val prefs = bootPreferences(context)
        return readConfig(prefs::getInt, prefs::getBoolean)
    }

    fun wasRunningAtShutdown(context: Context): Boolean = isLimiterActive(context)

    fun saveBootState(context: Context, running: Boolean, config: LimiterConfig) {
        bootPreferences(context).edit {
            putBoolean(KEY_WAS_RUNNING, running)
            writeConfig(config)
        }
    }

    private fun readConfig(
        getInt: (String, Int) -> Int,
        getBoolean: (String, Boolean) -> Boolean
    ) = LimiterConfig(
        maxVolume = getInt(KEY_MAX_VOLUME, DEFAULT_MAX_VOLUME),
        quietHoursEnabled = getBoolean(KEY_QUIET_ENABLED, false),
        quietHoursVolume = getInt(KEY_QUIET_VOLUME, DEFAULT_QUIET_VOLUME),
        quietHoursStartMinutes = getInt(KEY_QUIET_START, DEFAULT_QUIET_START_MINUTES),
        quietHoursEndMinutes = getInt(KEY_QUIET_END, DEFAULT_QUIET_END_MINUTES)
    )

    private fun android.content.SharedPreferences.Editor.writeConfig(config: LimiterConfig) {
        putInt(KEY_MAX_VOLUME, config.maxVolume)
        putBoolean(KEY_QUIET_ENABLED, config.quietHoursEnabled)
        putInt(KEY_QUIET_VOLUME, config.quietHoursVolume)
        putInt(KEY_QUIET_START, config.quietHoursStartMinutes)
        putInt(KEY_QUIET_END, config.quietHoursEndMinutes)
    }
}

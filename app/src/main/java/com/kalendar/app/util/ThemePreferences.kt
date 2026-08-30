package com.kalendar.app.util

import android.content.Context
import android.content.SharedPreferences
import com.kalendar.app.ui.theme.ThemeMode

/**
 * Manages all persisted user preferences (Theme, Widget Theme, Week Start, Default Reminders).
 */
object ThemePreferences {
    private const val PREFS_NAME = "kalendar_user_prefs"
    private const val KEY_THEME_MODE = "key_theme_mode"
    private const val KEY_WIDGET_THEME_MODE = "key_widget_theme_mode"
    private const val KEY_WEEK_START = "key_week_start"
    private const val KEY_REMINDER_TIME = "key_reminder_time"
    private const val KEY_ALL_DAY_REMINDER = "key_all_day_reminder"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // App Theme Mode
    fun getThemeMode(context: Context): ThemeMode {
        val saved = getPrefs(context).getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(saved ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        getPrefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    // Widget Theme Mode: "MATCH_APP", "DARK", "LIGHT", "SYSTEM"
    fun getWidgetThemeMode(context: Context): String {
        return getPrefs(context).getString(KEY_WIDGET_THEME_MODE, "MATCH_APP") ?: "MATCH_APP"
    }

    fun setWidgetThemeMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_WIDGET_THEME_MODE, mode).apply()
    }

    fun isWidgetDark(context: Context): Boolean {
        return when (getWidgetThemeMode(context)) {
            "DARK" -> true
            "LIGHT" -> false
            "SYSTEM" -> {
                val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
            else -> { // MATCH_APP
                when (getThemeMode(context)) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> {
                        val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                        nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    }
                }
            }
        }
    }

    // Week Start
    fun getWeekStart(context: Context): String {
        return getPrefs(context).getString(KEY_WEEK_START, "Region default") ?: "Region default"
    }

    fun setWeekStart(context: Context, start: String) {
        getPrefs(context).edit().putString(KEY_WEEK_START, start).apply()
    }

    // Default Reminder Time
    fun getDefaultReminderTime(context: Context): String {
        return getPrefs(context).getString(KEY_REMINDER_TIME, "30 minutes before") ?: "30 minutes before"
    }

    fun setDefaultReminderTime(context: Context, time: String) {
        getPrefs(context).edit().putString(KEY_REMINDER_TIME, time).apply()
    }

    // Default All-day Reminder Time
    fun getDefaultAllDayReminderTime(context: Context): String {
        return getPrefs(context).getString(KEY_ALL_DAY_REMINDER, "8:00 am") ?: "8:00 am"
    }

    fun setDefaultAllDayReminderTime(context: Context, time: String) {
        getPrefs(context).edit().putString(KEY_ALL_DAY_REMINDER, time).apply()
    }

    // App Icon Theme: "WHITE", "DARK"
    private const val KEY_APP_ICON_THEME = "key_app_icon_theme"

    fun getAppIconTheme(context: Context): String {
        return getPrefs(context).getString(KEY_APP_ICON_THEME, "WHITE") ?: "WHITE"
    }

    fun setAppIconTheme(context: Context, theme: String) {
        getPrefs(context).edit().putString(KEY_APP_ICON_THEME, theme).apply()
    }
}

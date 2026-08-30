package com.kalendar.app.ui.settings

import android.app.Application
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kalendar.app.data.local.KalendarDatabase
import com.kalendar.app.data.local.entity.AccountEntity
import com.kalendar.app.data.local.entity.CalendarEntity
import com.kalendar.app.data.repository.CalendarRepository
import com.kalendar.app.ui.theme.CalendarColorList
import com.kalendar.app.ui.theme.ThemeMode
import com.kalendar.app.util.DynamicIconManager
import com.kalendar.app.util.ThemePreferences
import com.kalendar.app.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val widgetThemeMode: String = "MATCH_APP", // MATCH_APP, DARK, LIGHT, SYSTEM
    val appIconTheme: String = "WHITE", // WHITE, DARK
    val accounts: List<AccountEntity> = emptyList(),
    val calendars: List<CalendarEntity> = emptyList(),
    val weekStart: String = "Region default",
    val defaultReminderTime: String = "30 minutes before",
    val defaultAllDayReminderTime: String = "8:00 am",
    val isLoading: Boolean = true
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KalendarDatabase.getInstance(application)
    private val calendarRepository = CalendarRepository(database.calendarDao(), database.accountDao())

    private val _themeMode = MutableStateFlow(ThemePreferences.getThemeMode(application))
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _widgetThemeMode = MutableStateFlow(ThemePreferences.getWidgetThemeMode(application))
    val widgetThemeMode: StateFlow<String> = _widgetThemeMode

    private val _appIconTheme = MutableStateFlow(ThemePreferences.getAppIconTheme(application))
    val appIconTheme: StateFlow<String> = _appIconTheme

    private val _weekStart = MutableStateFlow(ThemePreferences.getWeekStart(application))
    private val _defaultReminderTime = MutableStateFlow(ThemePreferences.getDefaultReminderTime(application))
    private val _defaultAllDayReminderTime = MutableStateFlow(ThemePreferences.getDefaultAllDayReminderTime(application))

    val uiState: StateFlow<SettingsUiState> = combine(
        _themeMode,
        _widgetThemeMode,
        _appIconTheme,
        calendarRepository.getAllAccounts(),
        calendarRepository.getAllCalendars(),
        _weekStart,
        _defaultReminderTime,
        _defaultAllDayReminderTime
    ) { params: Array<Any> ->
        val theme = params[0] as ThemeMode
        val widgetTheme = params[1] as String
        val iconTheme = params[2] as String
        @Suppress("UNCHECKED_CAST")
        val accounts = params[3] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val calendars = params[4] as List<CalendarEntity>
        val weekStart = params[5] as String
        val remTime = params[6] as String
        val allDayRemTime = params[7] as String

        SettingsUiState(
            themeMode = theme,
            widgetThemeMode = widgetTheme,
            appIconTheme = iconTheme,
            accounts = accounts,
            calendars = calendars,
            weekStart = weekStart,
            defaultReminderTime = remTime,
            defaultAllDayReminderTime = allDayRemTime,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        ThemePreferences.setThemeMode(getApplication(), mode)
        WidgetUpdateHelper.updateAllWidgets(getApplication())
    }

    fun setWidgetThemeMode(mode: String) {
        _widgetThemeMode.value = mode
        ThemePreferences.setWidgetThemeMode(getApplication(), mode)
        WidgetUpdateHelper.updateAllWidgets(getApplication())
    }

    fun setAppIconTheme(theme: String) {
        _appIconTheme.value = theme
        ThemePreferences.setAppIconTheme(getApplication(), theme)
        DynamicIconManager.updateAppIcon(getApplication())
    }

    fun setWeekStart(start: String) {
        _weekStart.value = start
        ThemePreferences.setWeekStart(getApplication(), start)
    }

    fun setDefaultReminderTime(time: String) {
        _defaultReminderTime.value = time
        ThemePreferences.setDefaultReminderTime(getApplication(), time)
    }

    fun setDefaultAllDayReminderTime(time: String) {
        _defaultAllDayReminderTime.value = time
        ThemePreferences.setDefaultAllDayReminderTime(getApplication(), time)
    }

    fun toggleCalendarVisibility(calendarId: Long, isVisible: Boolean) {
        viewModelScope.launch {
            calendarRepository.toggleCalendarVisibility(calendarId, isVisible)
            WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun toggleAccountEnabled(accountId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            calendarRepository.toggleAccountEnabled(accountId, isEnabled)
            WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun addAccount(email: String, calendarName: String) {
        viewModelScope.launch {
            val accountId = calendarRepository.createAccount(
                AccountEntity(
                    googleAccountEmail = email,
                    displayName = email.substringBefore("@"),
                    isEnabled = true
                )
            )
            val randomColor = CalendarColorList.random().toArgb()
            calendarRepository.createCalendar(
                CalendarEntity(
                    accountId = accountId,
                    name = calendarName,
                    color = randomColor,
                    isVisible = true
                )
            )
            WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }
}

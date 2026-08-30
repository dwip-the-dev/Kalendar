package com.kalendar.app.ui.dayview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kalendar.app.data.local.KalendarDatabase
import com.kalendar.app.data.local.entity.CalendarEntity
import com.kalendar.app.data.local.entity.EventEntity
import com.kalendar.app.data.repository.CalendarRepository
import com.kalendar.app.data.repository.EventRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DayViewUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val events: List<EventEntity> = emptyList(),
    val calendars: Map<Long, CalendarEntity> = emptyMap(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class DayViewViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KalendarDatabase.getInstance(application)
    private val eventRepository = EventRepository(database.eventDao(), database.calendarDao())
    private val calendarRepository = CalendarRepository(database.calendarDao(), database.accountDao())

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<DayViewUiState> = combine(
        _selectedDate,
        _selectedDate.flatMapLatest { date -> eventRepository.getEventsForDay(date) },
        calendarRepository.getAllCalendars()
    ) { date, events, calendars ->
        DayViewUiState(
            selectedDate = date,
            events = events,
            calendars = calendars.associateBy { it.id },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DayViewUiState()
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun goToNextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun goToPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }
}

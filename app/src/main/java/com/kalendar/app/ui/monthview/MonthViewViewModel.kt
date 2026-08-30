package com.kalendar.app.ui.monthview

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
import java.time.LocalDate
import java.time.YearMonth

data class MonthViewUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val events: List<EventEntity> = emptyList(),
    val daysWithEvents: Set<Int> = emptySet(),
    val calendars: Map<Long, CalendarEntity> = emptyMap(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class MonthViewViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KalendarDatabase.getInstance(application)
    private val eventRepository = EventRepository(database.eventDao(), database.calendarDao())
    private val calendarRepository = CalendarRepository(database.calendarDao(), database.accountDao())

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<MonthViewUiState> = combine(
        _yearMonth,
        _selectedDate,
        _yearMonth.flatMapLatest { ym ->
            eventRepository.getEventsForMonth(ym.year, ym.monthValue)
        },
        calendarRepository.getAllCalendars()
    ) { yearMonth, selectedDate, events, calendars ->
        val daysWithEvents = events.map { event ->
            com.kalendar.app.util.DateUtils.toLocalDate(event.startTime).dayOfMonth
        }.toSet()

        MonthViewUiState(
            yearMonth = yearMonth,
            selectedDate = selectedDate,
            events = events,
            daysWithEvents = daysWithEvents,
            calendars = calendars.associateBy { it.id },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthViewUiState()
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun goToNextMonth() {
        _yearMonth.value = _yearMonth.value.plusMonths(1)
    }

    fun goToPreviousMonth() {
        _yearMonth.value = _yearMonth.value.minusMonths(1)
    }

    fun setYearMonth(yearMonth: YearMonth) {
        _yearMonth.value = yearMonth
        _selectedDate.value = yearMonth.atDay(1)
    }

    fun goToToday() {
        val today = LocalDate.now()
        _yearMonth.value = YearMonth.from(today)
        _selectedDate.value = today
    }
}

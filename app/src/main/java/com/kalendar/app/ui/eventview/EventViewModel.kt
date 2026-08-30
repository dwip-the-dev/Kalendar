package com.kalendar.app.ui.eventview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kalendar.app.data.local.KalendarDatabase
import com.kalendar.app.data.local.entity.*
import com.kalendar.app.data.repository.CalendarRepository
import com.kalendar.app.data.repository.EventRepository
import com.kalendar.app.data.sync.DeviceCalendarManager
import com.kalendar.app.util.DateUtils
import com.kalendar.app.util.NotificationHelper
import com.kalendar.app.util.ThemePreferences
import com.kalendar.app.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone

data class EventDetailUiState(
    val event: EventEntity? = null,
    val calendar: CalendarEntity? = null,
    val isLoading: Boolean = true
)

data class EventEditUiState(
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endDate: LocalDate = LocalDate.now(),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val isAllDay: Boolean = false,
    val calendarId: Long = 0,
    val eventType: String = "Event",
    val guests: String = "",
    val timeZone: String = TimeZone.getDefault().id,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val reminder: ReminderTime = ReminderTime.MINUTES_30,
    val calendars: List<CalendarEntity> = emptyList(),
    val isSaving: Boolean = false,
    val isEdit: Boolean = false,
    val eventId: Long? = null
)

data class EventsListUiState(
    val events: List<EventEntity> = emptyList(),
    val calendars: Map<Long, CalendarEntity> = emptyMap(),
    val isLoading: Boolean = true
)

class EventViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KalendarDatabase.getInstance(application)
    private val eventRepository = EventRepository(database.eventDao(), database.calendarDao())
    private val calendarRepository = CalendarRepository(database.calendarDao(), database.accountDao())

    val eventsListState: StateFlow<EventsListUiState> = combine(
        eventRepository.getAllEvents(),
        calendarRepository.getAllCalendars()
    ) { events, calendars ->
        EventsListUiState(
            events = events,
            calendars = calendars.associateBy { it.id },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EventsListUiState()
    )

    private val _eventDetailState = MutableStateFlow(EventDetailUiState())
    val eventDetailState: StateFlow<EventDetailUiState> = _eventDetailState

    private val _eventEditState = MutableStateFlow(EventEditUiState())
    val eventEditState: StateFlow<EventEditUiState> = _eventEditState

    fun loadEventDetail(eventId: Long) {
        viewModelScope.launch {
            eventRepository.getEventById(eventId).collect { event ->
                if (event != null) {
                    val calendar = calendarRepository.getCalendarById(event.calendarId)
                    _eventDetailState.value = EventDetailUiState(
                        event = event,
                        calendar = calendar,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun mapReminderPreference(pref: String): ReminderTime {
        return when (pref) {
            "5 minutes before" -> ReminderTime.MINUTES_5
            "15 minutes before" -> ReminderTime.MINUTES_15
            "30 minutes before" -> ReminderTime.MINUTES_30
            "1 hour before" -> ReminderTime.HOURS_1
            "1 day before" -> ReminderTime.DAYS_1
            "None" -> ReminderTime.NONE
            else -> ReminderTime.MINUTES_30
        }
    }

    fun initCreateEvent(prefillDate: LocalDate? = null) {
        viewModelScope.launch {
            calendarRepository.getAllCalendars().first().let { calendars ->
                val startDate = prefillDate ?: LocalDate.now()
                val now = LocalTime.now()
                val defaultStartTime = if (startDate == LocalDate.now()) {
                    now.plusHours(1).withMinute(0).withSecond(0)
                } else {
                    LocalTime.of(9, 0)
                }
                val defaultEndTime = defaultStartTime.plusHours(1)

                val defaultReminder = mapReminderPreference(
                    ThemePreferences.getDefaultReminderTime(getApplication())
                )

                _eventEditState.value = EventEditUiState(
                    startDate = startDate,
                    startTime = defaultStartTime,
                    endDate = startDate,
                    endTime = defaultEndTime,
                    calendarId = calendars.firstOrNull()?.id ?: 0,
                    reminder = defaultReminder,
                    calendars = calendars,
                    eventType = "Event",
                    guests = "",
                    timeZone = TimeZone.getDefault().id,
                    isEdit = false
                )
            }
        }
    }

    fun initEditEvent(eventId: Long) {
        viewModelScope.launch {
            val event = database.eventDao().getEventByIdOnce(eventId) ?: return@launch
            val calendars = calendarRepository.getAllCalendars().first()

            _eventEditState.value = EventEditUiState(
                title = event.title,
                description = event.description,
                location = event.location,
                startDate = DateUtils.toLocalDate(event.startTime),
                startTime = DateUtils.toLocalTime(event.startTime),
                endDate = DateUtils.toLocalDate(event.endTime),
                endTime = DateUtils.toLocalTime(event.endTime),
                isAllDay = event.isAllDay,
                calendarId = event.calendarId,
                eventType = event.eventType,
                guests = event.guests,
                timeZone = if (event.timeZone.isNotEmpty()) event.timeZone else TimeZone.getDefault().id,
                repeatRule = event.repeatRule,
                reminder = event.reminder,
                calendars = calendars,
                isEdit = true,
                eventId = event.id
            )
        }
    }

    fun updateTitle(title: String) {
        _eventEditState.value = _eventEditState.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _eventEditState.value = _eventEditState.value.copy(description = description)
    }

    fun updateLocation(location: String) {
        _eventEditState.value = _eventEditState.value.copy(location = location)
    }

    fun updateStartDate(date: LocalDate) {
        _eventEditState.value = _eventEditState.value.copy(startDate = date)
    }

    fun updateStartTime(time: LocalTime) {
        _eventEditState.value = _eventEditState.value.copy(startTime = time)
    }

    fun updateEndDate(date: LocalDate) {
        _eventEditState.value = _eventEditState.value.copy(endDate = date)
    }

    fun updateEndTime(time: LocalTime) {
        _eventEditState.value = _eventEditState.value.copy(endTime = time)
    }

    fun updateIsAllDay(isAllDay: Boolean) {
        _eventEditState.value = _eventEditState.value.copy(isAllDay = isAllDay)
    }

    fun updateEventType(type: String) {
        _eventEditState.value = _eventEditState.value.copy(eventType = type)
    }

    fun updateGuests(guests: String) {
        _eventEditState.value = _eventEditState.value.copy(guests = guests)
    }

    fun updateTimeZone(tz: String) {
        _eventEditState.value = _eventEditState.value.copy(timeZone = tz)
    }

    fun updateRepeatRule(repeatRule: RepeatRule) {
        _eventEditState.value = _eventEditState.value.copy(repeatRule = repeatRule)
    }

    fun updateReminder(reminder: ReminderTime) {
        _eventEditState.value = _eventEditState.value.copy(reminder = reminder)
    }

    fun updateCalendarId(calendarId: Long) {
        _eventEditState.value = _eventEditState.value.copy(calendarId = calendarId)
    }

    fun saveEvent(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _eventEditState.value
            _eventEditState.value = state.copy(isSaving = true)

            val now = LocalTime.now()
            val finalStartTime = if (!state.isAllDay && state.startDate == LocalDate.now() && state.startTime.isBefore(now)) {
                now.plusMinutes(5)
            } else {
                state.startTime
            }

            val finalEndTime = if (state.endDate == state.startDate && finalStartTime.isAfter(state.endTime)) {
                finalStartTime.plusHours(1)
            } else {
                state.endTime
            }

            val startMillis = DateUtils.toEpochMillis(state.startDate, finalStartTime)
            val endMillis = DateUtils.toEpochMillis(state.endDate, finalEndTime)

            val selectedCalendar = database.calendarDao().getCalendarByIdOnce(state.calendarId)
            val deviceCalId = selectedCalendar?.googleCalendarId?.toLongOrNull()

            val savedEvent: EventEntity
            if (state.isEdit && state.eventId != null) {
                val existingEvent = database.eventDao().getEventByIdOnce(state.eventId)
                if (existingEvent != null) {
                    var googleEventId = existingEvent.googleEventId
                    if (deviceCalId != null) {
                        val deviceEventId = googleEventId?.toLongOrNull()
                        if (deviceEventId != null) {
                            DeviceCalendarManager.updateEventOnDevice(
                                getApplication(),
                                deviceEventId,
                                state.title,
                                state.description,
                                state.location,
                                startMillis,
                                endMillis,
                                state.isAllDay,
                                state.timeZone
                            )
                        } else {
                            val newDeviceEventId = DeviceCalendarManager.insertEventToDevice(
                                getApplication(),
                                deviceCalId,
                                state.title,
                                state.description,
                                state.location,
                                startMillis,
                                endMillis,
                                state.isAllDay,
                                state.timeZone
                            )
                            if (newDeviceEventId != null) {
                                googleEventId = newDeviceEventId.toString()
                            }
                        }
                    }

                    savedEvent = existingEvent.copy(
                        title = state.title,
                        description = state.description,
                        location = state.location,
                        startTime = startMillis,
                        endTime = endMillis,
                        isAllDay = state.isAllDay,
                        calendarId = state.calendarId,
                        eventType = state.eventType,
                        guests = state.guests,
                        timeZone = state.timeZone,
                        repeatRule = state.repeatRule,
                        reminder = state.reminder,
                        googleEventId = googleEventId
                    )
                    eventRepository.updateEvent(savedEvent)
                } else {
                    savedEvent = EventEntity(
                        title = state.title,
                        description = state.description,
                        location = state.location,
                        startTime = startMillis,
                        endTime = endMillis,
                        isAllDay = state.isAllDay,
                        calendarId = state.calendarId,
                        eventType = state.eventType,
                        guests = state.guests,
                        timeZone = state.timeZone,
                        repeatRule = state.repeatRule,
                        reminder = state.reminder
                    )
                }
            } else {
                var googleEventId: String? = null
                if (deviceCalId != null) {
                    val newDeviceEventId = DeviceCalendarManager.insertEventToDevice(
                        getApplication(),
                        deviceCalId,
                        state.title,
                        state.description,
                        state.location,
                        startMillis,
                        endMillis,
                        state.isAllDay,
                        state.timeZone
                    )
                    if (newDeviceEventId != null) {
                        googleEventId = newDeviceEventId.toString()
                    }
                }

                val newId = eventRepository.createEvent(
                    EventEntity(
                        title = state.title,
                        description = state.description,
                        location = state.location,
                        startTime = startMillis,
                        endTime = endMillis,
                        isAllDay = state.isAllDay,
                        calendarId = state.calendarId,
                        eventType = state.eventType,
                        guests = state.guests,
                        timeZone = state.timeZone,
                        repeatRule = state.repeatRule,
                        reminder = state.reminder,
                        googleEventId = googleEventId
                    )
                )
                savedEvent = EventEntity(
                    id = newId,
                    title = state.title,
                    description = state.description,
                    location = state.location,
                    startTime = startMillis,
                    endTime = endMillis,
                    isAllDay = state.isAllDay,
                    calendarId = state.calendarId,
                    eventType = state.eventType,
                    guests = state.guests,
                    timeZone = state.timeZone,
                    repeatRule = state.repeatRule,
                    reminder = state.reminder,
                    googleEventId = googleEventId
                )
            }

            // Schedule real system alarm for notification
            NotificationHelper.scheduleEventReminder(getApplication(), savedEvent)

            // Update all widgets
            WidgetUpdateHelper.updateAllWidgets(getApplication())

            _eventEditState.value = state.copy(isSaving = false)
            onComplete()
        }
    }

    fun deleteEvent(eventId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            val existing = database.eventDao().getEventByIdOnce(eventId)
            if (existing?.googleEventId != null) {
                existing.googleEventId.toLongOrNull()?.let { devId ->
                    DeviceCalendarManager.deleteEventFromDevice(getApplication(), devId)
                }
            }
            NotificationHelper.cancelEventReminder(getApplication(), eventId)
            eventRepository.deleteEvent(eventId)
            WidgetUpdateHelper.updateAllWidgets(getApplication())
            onComplete()
        }
    }
}

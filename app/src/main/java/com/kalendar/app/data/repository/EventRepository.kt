package com.kalendar.app.data.repository

import com.kalendar.app.data.local.dao.CalendarDao
import com.kalendar.app.data.local.dao.EventDao
import com.kalendar.app.data.local.entity.EventEntity
import com.kalendar.app.data.local.entity.PendingAction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

/**
 * Repository for event operations. All reads come from Room (offline-first).
 * Write operations mark changes with pendingAction for sync.
 */
class EventRepository(
    private val eventDao: EventDao,
    private val calendarDao: CalendarDao
) {
    fun getEventsForDay(date: LocalDate): Flow<List<EventEntity>> {
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return eventDao.getEventsForDay(dayStart, dayEnd)
    }

    fun getEventsForMonth(year: Int, month: Int): Flow<List<EventEntity>> {
        val zone = ZoneId.systemDefault()
        val monthStart = LocalDate.of(year, month, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return eventDao.getEventsForMonth(monthStart, monthEnd)
    }

    fun getDaysWithEventsInMonth(year: Int, month: Int): Flow<List<Long>> {
        val zone = ZoneId.systemDefault()
        val monthStart = LocalDate.of(year, month, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return eventDao.getDaysWithEventsInMonth(monthStart, monthEnd)
    }

    fun getAllEvents(): Flow<List<EventEntity>> = eventDao.getAllEvents()

    fun getEventById(id: Long): Flow<EventEntity?> = eventDao.getEventById(id)

    suspend fun createEvent(event: EventEntity): Long {
        val newEvent = event.copy(
            pendingAction = if (event.googleEventId != null) PendingAction.CREATE else PendingAction.NONE,
            lastModified = System.currentTimeMillis()
        )
        return eventDao.insert(newEvent)
    }

    suspend fun updateEvent(event: EventEntity) {
        val updated = event.copy(
            pendingAction = if (event.googleEventId != null) PendingAction.UPDATE else event.pendingAction,
            lastModified = System.currentTimeMillis()
        )
        eventDao.update(updated)
    }

    suspend fun deleteEvent(id: Long) {
        val event = eventDao.getEventByIdOnce(id)
        if (event != null) {
            if (event.googleEventId != null) {
                // Soft delete — sync engine will push the delete to Google
                eventDao.softDelete(id)
            } else {
                // Local-only event — hard delete immediately
                eventDao.hardDelete(id)
            }
        }
    }

    suspend fun getPendingEvents(): List<EventEntity> = eventDao.getPendingEvents()

    suspend fun getDeletedPendingEvents(): List<EventEntity> = eventDao.getDeletedPendingEvents()

    suspend fun markSynced(id: Long) {
        eventDao.updatePendingAction(id, PendingAction.NONE)
    }

    suspend fun cleanupSyncedDeletes() = eventDao.cleanupSyncedDeletes()
}

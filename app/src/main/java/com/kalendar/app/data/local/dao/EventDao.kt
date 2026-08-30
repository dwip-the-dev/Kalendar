package com.kalendar.app.data.local.dao

import androidx.room.*
import com.kalendar.app.data.local.entity.EventEntity
import com.kalendar.app.data.local.entity.PendingAction
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("""
        SELECT * FROM events 
        WHERE isDeleted = 0 
        AND (
            (startTime >= :dayStart AND startTime < :dayEnd) OR
            (endTime > :dayStart AND endTime <= :dayEnd) OR
            (startTime <= :dayStart AND endTime >= :dayEnd)
        )
        AND (calendarId = 0 OR calendarId IN (SELECT id FROM calendars WHERE isVisible = 1))
        ORDER BY startTime ASC
    """)
    fun getEventsForDay(dayStart: Long, dayEnd: Long): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events 
        WHERE isDeleted = 0 
        AND startTime >= :monthStart AND startTime < :monthEnd
        AND (calendarId = 0 OR calendarId IN (SELECT id FROM calendars WHERE isVisible = 1))
        ORDER BY startTime ASC
    """)
    fun getEventsForMonth(monthStart: Long, monthEnd: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE isDeleted = 0 ORDER BY startTime ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Long): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventByIdOnce(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE googleEventId = :googleId LIMIT 1")
    suspend fun getEventByGoogleId(googleId: String): EventEntity?

    @Query("""
        SELECT * FROM events 
        WHERE isDeleted = 0 
        AND (endTime >= :now OR startTime >= :now)
        AND (calendarId = 0 OR calendarId IN (SELECT id FROM calendars WHERE isVisible = 1))
        ORDER BY startTime ASC 
        LIMIT 1
    """)
    suspend fun getNextUpcomingEvent(now: Long): EventEntity?

    @Query("""
        SELECT * FROM events 
        WHERE isDeleted = 0 
        AND (
            (startTime >= :start AND startTime < :end) OR
            (endTime > :start AND endTime <= :end) OR
            (startTime <= :start AND endTime >= :end)
        )
        AND (calendarId = 0 OR calendarId IN (SELECT id FROM calendars WHERE isVisible = 1))
        ORDER BY startTime ASC
    """)
    suspend fun getEventsForTimeRangeOnce(start: Long, end: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE pendingAction != 'NONE' AND isDeleted = 0")
    suspend fun getPendingEvents(): List<EventEntity>

    @Query("SELECT * FROM events WHERE pendingAction = 'DELETE'")
    suspend fun getDeletedPendingEvents(): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Query("UPDATE events SET pendingAction = :action, lastModified = :time WHERE id = :id")
    suspend fun updatePendingAction(id: Long, action: PendingAction, time: Long = System.currentTimeMillis())

    @Query("UPDATE events SET isDeleted = 1, pendingAction = 'DELETE', lastModified = :time WHERE id = :id")
    suspend fun softDelete(id: Long, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("DELETE FROM events WHERE isDeleted = 1 AND pendingAction = 'NONE'")
    suspend fun cleanupSyncedDeletes()

    @Upsert
    suspend fun upsert(event: EventEntity)

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Query("""
        SELECT DISTINCT CAST(startTime / 86400000 AS INTEGER) as dayEpoch
        FROM events 
        WHERE isDeleted = 0 
        AND startTime >= :monthStart AND startTime < :monthEnd
        AND (calendarId = 0 OR calendarId IN (SELECT id FROM calendars WHERE isVisible = 1))
    """)
    fun getDaysWithEventsInMonth(monthStart: Long, monthEnd: Long): Flow<List<Long>>
}

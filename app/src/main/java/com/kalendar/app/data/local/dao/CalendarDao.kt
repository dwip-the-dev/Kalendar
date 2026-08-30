package com.kalendar.app.data.local.dao

import androidx.room.*
import com.kalendar.app.data.local.entity.CalendarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {

    @Query("SELECT * FROM calendars ORDER BY isPrimary DESC, name ASC")
    fun getAllCalendars(): Flow<List<CalendarEntity>>

    @Query("SELECT * FROM calendars WHERE accountId = :accountId ORDER BY isPrimary DESC, name ASC")
    fun getCalendarsByAccount(accountId: Long): Flow<List<CalendarEntity>>

    @Query("SELECT * FROM calendars WHERE accountId = :accountId ORDER BY isPrimary DESC, name ASC")
    suspend fun getCalendarsForAccountList(accountId: Long): List<CalendarEntity>

    @Query("SELECT * FROM calendars WHERE isVisible = 1 ORDER BY isPrimary DESC, name ASC")
    fun getVisibleCalendars(): Flow<List<CalendarEntity>>

    @Query("SELECT * FROM calendars WHERE id = :id")
    suspend fun getCalendarById(id: Long): CalendarEntity?

    @Query("SELECT * FROM calendars WHERE id = :id")
    suspend fun getCalendarByIdOnce(id: Long): CalendarEntity?

    @Query("SELECT * FROM calendars WHERE googleCalendarId = :googleId")
    suspend fun getCalendarByGoogleId(googleId: String): CalendarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(calendar: CalendarEntity): Long

    @Update
    suspend fun update(calendar: CalendarEntity)

    @Delete
    suspend fun delete(calendar: CalendarEntity)

    @Query("UPDATE calendars SET isVisible = :isVisible WHERE id = :id")
    suspend fun setVisibility(id: Long, isVisible: Boolean)

    @Upsert
    suspend fun upsertAll(calendars: List<CalendarEntity>)

    @Query("SELECT COUNT(*) FROM calendars")
    suspend fun getCount(): Int
}

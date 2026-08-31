package com.kalendar.app.data.sync

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.kalendar.app.data.local.KalendarDatabase
import com.kalendar.app.data.local.dao.EventDao
import com.kalendar.app.data.local.entity.AccountEntity
import com.kalendar.app.data.local.entity.CalendarEntity
import com.kalendar.app.data.local.entity.EventEntity
import com.kalendar.app.data.local.entity.PendingAction
import com.kalendar.app.data.local.entity.ReminderTime
import com.kalendar.app.data.local.entity.RepeatRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages two-way sync with Android's built-in CalendarProvider.
 * Auto-fetches all Google accounts (e.g. Gmail/Google Workspace) and device calendars,
 * and pushes created/edited events directly to Google Calendar.
 */
object DeviceCalendarManager {

    private const val TAG = "DeviceCalendarManager"

    fun hasCalendarPermissions(context: Context): Boolean {
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        return read && write
    }

    /**
     * Auto-fetches all accounts and calendars from device's CalendarContract into Room database.
     */
    suspend fun syncDeviceCalendars(context: Context) = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions(context)) {
            Log.d(TAG, "Calendar permissions not granted, skipping device calendar sync")
            return@withContext
        }

        try {
            val db = KalendarDatabase.getInstance(context)
            val accountDao = db.accountDao()
            val calendarDao = db.calendarDao()
            val eventDao = db.eventDao()

            val contentResolver = context.contentResolver

            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR,
                CalendarContract.Calendars.VISIBLE,
                CalendarContract.Calendars.IS_PRIMARY
            )

            val cursor: Cursor? = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val accountNameCol = c.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val displayNameCol = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val colorCol = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                val visibleCol = c.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE)

                while (c.moveToNext()) {
                    val deviceCalId = c.getLong(idCol)
                    val accountEmail = c.getString(accountNameCol) ?: "Device Account"
                    val calName = c.getString(displayNameCol) ?: "Calendar"
                    val calColor = if (!c.isNull(colorCol)) c.getInt(colorCol) else 0xFF2F70F2.toInt()
                    val isVisible = c.getInt(visibleCol) != 0

                    // 1. Find or create Account in Room
                    val existingAccount = accountDao.getAccountByEmail(accountEmail)
                    val accountId = if (existingAccount != null) {
                        existingAccount.id
                    } else {
                        accountDao.insert(
                            AccountEntity(
                                googleAccountEmail = accountEmail,
                                displayName = accountEmail.substringBefore("@"),
                                isEnabled = true
                            )
                        )
                    }

                    // 2. Find or create Calendar in Room
                    val existingCalendars = calendarDao.getCalendarsForAccountList(accountId)
                    val existingCal = existingCalendars.find { it.name == calName || it.googleCalendarId == deviceCalId.toString() }
                    val roomCalId = if (existingCal != null) {
                        if (existingCal.googleCalendarId == null) {
                            calendarDao.update(existingCal.copy(googleCalendarId = deviceCalId.toString()))
                        }
                        existingCal.id
                    } else {
                        calendarDao.insert(
                            CalendarEntity(
                                accountId = accountId,
                                googleCalendarId = deviceCalId.toString(),
                                name = calName,
                                color = calColor,
                                isVisible = isVisible
                            )
                        )
                    }

                    // 3. Fetch events for this calendar
                    syncEventsForCalendar(context, deviceCalId, roomCalId, eventDao)
                }
            }

            // Reschedule reminders for newly synced events
            com.kalendar.app.util.NotificationHelper.rescheduleAllUpcomingReminders(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing device calendars", e)
        }
    }

    private suspend fun syncEventsForCalendar(
        context: Context,
        deviceCalId: Long,
        roomCalId: Long,
        eventDao: EventDao
    ) {
        val eventProjection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.EVENT_TIMEZONE
        )

        val uri = CalendarContract.Events.CONTENT_URI
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DELETED} = 0"
        val selectionArgs = arrayOf(deviceCalId.toString())

        val cursor = context.contentResolver.query(
            uri,
            eventProjection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val titleCol = c.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val descCol = c.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
            val locCol = c.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
            val startCol = c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val endCol = c.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
            val allDayCol = c.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
            val tzCol = c.getColumnIndexOrThrow(CalendarContract.Events.EVENT_TIMEZONE)

            while (c.moveToNext()) {
                val remoteId = c.getLong(idCol).toString()
                val title = c.getString(titleCol) ?: "Untitled Event"
                val description = c.getString(descCol) ?: ""
                val location = c.getString(locCol) ?: ""
                val dtStart = c.getLong(startCol)
                val dtEnd = if (!c.isNull(endCol)) c.getLong(endCol) else dtStart + 3600000L
                val isAllDay = c.getInt(allDayCol) == 1
                val timeZone = c.getString(tzCol) ?: java.util.TimeZone.getDefault().id

                val existing = eventDao.getEventByGoogleId(remoteId)
                if (existing == null) {
                    eventDao.insert(
                        EventEntity(
                            calendarId = roomCalId,
                            googleEventId = remoteId,
                            title = title,
                            description = description,
                            location = location,
                            startTime = dtStart,
                            endTime = dtEnd,
                            isAllDay = isAllDay,
                            timeZone = timeZone,
                            reminder = ReminderTime.MINUTES_30,
                            repeatRule = RepeatRule.NONE,
                            pendingAction = PendingAction.NONE
                        )
                    )
                }
            }
        }
    }

    /**
     * Add event to Android Device / Google Calendar provider directly so it syncs with Google Calendar.
     */
    suspend fun insertEventToDevice(
        context: Context,
        deviceCalId: Long,
        title: String,
        description: String,
        location: String,
        startTime: Long,
        endTime: Long,
        isAllDay: Boolean,
        timeZone: String = java.util.TimeZone.getDefault().id
    ): Long? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions(context)) return@withContext null

        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, deviceCalId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.EVENT_LOCATION, location)
                put(CalendarContract.Events.DTSTART, startTime)
                put(CalendarContract.Events.DTEND, endTime)
                put(CalendarContract.Events.ALL_DAY, if (isAllDay) 1 else 0)
                put(CalendarContract.Events.EVENT_TIMEZONE, if (timeZone.isNotEmpty()) timeZone else java.util.TimeZone.getDefault().id)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting event to device calendar", e)
            null
        }
    }

    suspend fun updateEventOnDevice(
        context: Context,
        deviceEventId: Long,
        title: String,
        description: String,
        location: String,
        startTime: Long,
        endTime: Long,
        isAllDay: Boolean,
        timeZone: String = java.util.TimeZone.getDefault().id
    ): Boolean = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions(context)) return@withContext false

        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.EVENT_LOCATION, location)
                put(CalendarContract.Events.DTSTART, startTime)
                put(CalendarContract.Events.DTEND, endTime)
                put(CalendarContract.Events.ALL_DAY, if (isAllDay) 1 else 0)
                put(CalendarContract.Events.EVENT_TIMEZONE, if (timeZone.isNotEmpty()) timeZone else java.util.TimeZone.getDefault().id)
            }

            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, deviceEventId)
            val rows = context.contentResolver.update(uri, values, null, null)
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error updating event on device calendar", e)
            false
        }
    }

    suspend fun deleteEventFromDevice(
        context: Context,
        deviceEventId: Long
    ): Boolean = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions(context)) return@withContext false

        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, deviceEventId)
            val rows = context.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting event from device calendar", e)
            false
        }
    }
}

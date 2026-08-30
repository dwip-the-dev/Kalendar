package com.kalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks the sync state of local changes that need to be pushed to Google Calendar.
 */
enum class PendingAction {
    NONE,
    CREATE,
    UPDATE,
    DELETE
}

/**
 * Repeat rule for recurring events.
 */
enum class RepeatRule {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

/**
 * Reminder time before event start.
 */
enum class ReminderTime {
    NONE,
    MINUTES_5,
    MINUTES_15,
    MINUTES_30,
    HOURS_1,
    DAYS_1;

    fun toMinutes(): Long = when (this) {
        NONE -> 0
        MINUTES_5 -> 5
        MINUTES_15 -> 15
        MINUTES_30 -> 30
        HOURS_1 -> 60
        DAYS_1 -> 1440
    }
}

/**
 * Core event entity. This is the single source of truth for all calendar events.
 * The UI reads from Room, sync engine pushes/pulls to/from Google Calendar API.
 */
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = CalendarEntity::class,
            parentColumns = ["id"],
            childColumns = ["calendarId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("calendarId"),
        Index("startTime"),
        Index("endTime"),
        Index("pendingAction")
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val googleEventId: String? = null,
    val title: String,
    val description: String = "",
    val location: String = "",
    val startTime: Long,      // epoch millis
    val endTime: Long,         // epoch millis
    val isAllDay: Boolean = false,
    val calendarId: Long,
    val color: Int = 0,
    val eventType: String = "Event", // Event, Birthday, Meeting, Holiday, Task, Anniversary
    val guests: String = "",         // Comma separated email addresses
    val timeZone: String = "",       // Zone ID, e.g. "America/New_York", "UTC"
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val reminder: ReminderTime = ReminderTime.MINUTES_30,
    val pendingAction: PendingAction = PendingAction.NONE,
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

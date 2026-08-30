package com.kalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a calendar (e.g. "Personal", "Work", "School") belonging to a Google account.
 * Each account can have multiple calendars.
 */
@Entity(
    tableName = "calendars",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId")]
)
data class CalendarEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val googleCalendarId: String? = null,
    val name: String,
    val color: Int,
    val accountId: Long,
    val isVisible: Boolean = true,
    val isPrimary: Boolean = false
)

package com.kalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a Google account connected to the app.
 * Each account can have multiple calendars.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val googleAccountEmail: String,
    val displayName: String = "",
    val isEnabled: Boolean = true,
    val lastSyncTime: Long = 0,
    val syncToken: String? = null
)

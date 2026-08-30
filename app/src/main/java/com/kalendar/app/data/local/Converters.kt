package com.kalendar.app.data.local

import androidx.room.TypeConverter
import com.kalendar.app.data.local.entity.PendingAction
import com.kalendar.app.data.local.entity.ReminderTime
import com.kalendar.app.data.local.entity.RepeatRule

/**
 * Room type converters for enum types.
 */
class Converters {

    @TypeConverter
    fun fromPendingAction(value: PendingAction): String = value.name

    @TypeConverter
    fun toPendingAction(value: String): PendingAction = PendingAction.valueOf(value)

    @TypeConverter
    fun fromRepeatRule(value: RepeatRule): String = value.name

    @TypeConverter
    fun toRepeatRule(value: String): RepeatRule = RepeatRule.valueOf(value)

    @TypeConverter
    fun fromReminderTime(value: ReminderTime): String = value.name

    @TypeConverter
    fun toReminderTime(value: String): ReminderTime = ReminderTime.valueOf(value)
}

package com.kalendar.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kalendar.app.data.local.dao.AccountDao
import com.kalendar.app.data.local.dao.CalendarDao
import com.kalendar.app.data.local.dao.EventDao
import com.kalendar.app.data.local.entity.AccountEntity
import com.kalendar.app.data.local.entity.CalendarEntity
import com.kalendar.app.data.local.entity.EventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        EventEntity::class,
        CalendarEntity::class,
        AccountEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KalendarDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun calendarDao(): CalendarDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: KalendarDatabase? = null

        fun getInstance(context: Context): KalendarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KalendarDatabase::class.java,
                    "kalendar_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Pre-populate the database with a default "Local" account and calendar
     * so the app works immediately without Google sign-in.
     */
    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    // Create a default local account
                    val accountId = database.accountDao().insert(
                        AccountEntity(
                            googleAccountEmail = "local",
                            displayName = "Local Calendar",
                            isEnabled = true
                        )
                    )
                    // Create a default local calendar
                    database.calendarDao().insert(
                        CalendarEntity(
                            name = "Personal",
                            color = 0xFF7C4DFF.toInt(), // Primary purple
                            accountId = accountId,
                            isPrimary = true
                        )
                    )
                }
            }
        }
    }
}

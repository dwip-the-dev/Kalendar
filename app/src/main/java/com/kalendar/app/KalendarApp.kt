package com.kalendar.app

import android.app.Application
import com.kalendar.app.data.local.KalendarDatabase
import com.kalendar.app.data.sync.SyncWorker
import com.kalendar.app.util.DynamicIconManager
import com.kalendar.app.util.HeroImageManager
import com.kalendar.app.util.NotificationHelper
import com.kalendar.app.widget.WidgetUpdateHelper

/**
 * Application class for Kalendar Calendar.
 * Initializes the Room database, notification channels, dynamic icons, and sync services.
 */
class KalendarApp : Application() {

    lateinit var database: KalendarDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize database
        database = KalendarDatabase.getInstance(this)

        // Schedule periodic sync with Google Calendar
        SyncWorker.schedule(this)

        // Initialize notification channel for reminders
        NotificationHelper.createNotificationChannel(this)

        // Reschedule all upcoming event reminders
        NotificationHelper.rescheduleAllUpcomingReminders(this)

        // Prefetch 3-day hero images for offline support
        HeroImageManager.prefetchRecentImages(this)

        // Refresh all home screen widgets
        WidgetUpdateHelper.updateAllWidgets(this)

        // Update 24-hour dynamic calendar app icon and schedule midnight refresh
        DynamicIconManager.updateAppIcon(this)
        DynamicIconManager.scheduleDailyMidnightUpdate(this)
    }

    companion object {
        lateinit var instance: KalendarApp
            private set
    }
}

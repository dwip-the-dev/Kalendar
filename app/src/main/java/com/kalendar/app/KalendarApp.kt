package com.kalendar.app

import android.app.Application
import com.kalendar.app.data.local.KalendarDatabase
import com.kalendar.app.data.sync.SyncWorker

/**
 * Application class for Kalendar Calendar.
 * Initializes the Room database and schedules periodic sync.
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
        com.kalendar.app.util.NotificationHelper.createNotificationChannel(this)

        // Prefetch 3-day hero images for offline support
        com.kalendar.app.util.HeroImageManager.prefetchRecentImages(this)

        // Refresh all home screen widgets
        com.kalendar.app.widget.WidgetUpdateHelper.updateAllWidgets(this)

        // Update 24-hour dynamic calendar app icon and schedule midnight refresh
        com.kalendar.app.util.DynamicIconManager.updateAppIcon(this)
        com.kalendar.app.util.DynamicIconManager.scheduleDailyMidnightUpdate(this)
    }

    companion object {
        lateinit var instance: KalendarApp
            private set
    }
}

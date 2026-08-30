package com.kalendar.app.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.kalendar.app.data.local.KalendarDatabase
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for periodic background calendar sync.
 * Runs every 15 minutes when the device has network connectivity.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "kalendar_calendar_sync"

        /**
         * Schedule periodic sync with WorkManager.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

            Log.d(TAG, "Periodic sync scheduled (every 15 minutes)")
        }

        /**
         * Trigger an immediate one-time sync.
         */
        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
            Log.d(TAG, "One-time sync triggered")
        }

        /**
         * Cancel all scheduled syncs.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Periodic sync cancelled")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting sync work...")
            val database = KalendarDatabase.getInstance(applicationContext)
            val syncManager = SyncManager(applicationContext, database)
            
            val success = syncManager.sync()
            if (success) {
                Log.d(TAG, "Sync work completed successfully")
                Result.success()
            } else {
                Log.w(TAG, "Sync work failed, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync work error", e)
            Result.retry()
        }
    }
}

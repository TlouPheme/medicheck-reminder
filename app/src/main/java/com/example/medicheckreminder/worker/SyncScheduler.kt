package com.example.medicheckreminder.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Queues a sync that WorkManager holds until the phone is online (network constraint), so a
 * change made in airplane mode is sent as soon as the connection returns, even if the app is closed.
 *
 * https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work#work-constraints
 */
object SyncScheduler {
    const val UNIQUE_WORK_NAME = "medicheck_sync"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // APPEND_OR_REPLACE: a change made while a sync is running gets its own run afterwards,
        // instead of cancelling the running one or being dropped.
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
        Log.d(TAG, "Sync queued; it runs when the phone is online")
    }

    private const val TAG = "SyncScheduler"
}

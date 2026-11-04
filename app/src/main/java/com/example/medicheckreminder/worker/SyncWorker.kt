package com.example.medicheckreminder.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medicheckreminder.MediCheckApp

/**
 * Runs [com.example.medicheckreminder.data.sync.FirestoreSyncEngine] once a network is available
 * (see [SyncScheduler]). Without Firebase or a signed-in Firebase user there is nothing to sync
 * to, so the data simply stays on the phone with its pending flag until there is.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as MediCheckApp).container
        val gateway = container.firebaseAuthGateway
        if (!gateway.isAvailable) {
            Log.d(TAG, "Firebase is not configured; skipping sync")
            return Result.success()
        }
        val uid = gateway.currentUser?.uid
        if (uid == null) {
            Log.d(TAG, "No Firebase user; changes stay on the phone until sign-in")
            return Result.success()
        }
        return try {
            container.syncEngine.sync(uid)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed (attempt ${runAttemptCount + 1})", e)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val TAG = "SyncWorker"
        const val MAX_ATTEMPTS = 5
    }
}

package com.example.medicheckreminder.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.medicheckreminder.data.local.DoseLogDao
import com.example.medicheckreminder.data.local.HealthMeasurementDao
import com.example.medicheckreminder.data.local.MedicationDao
import com.example.medicheckreminder.worker.SyncScheduler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class SyncState { SYNCED, PENDING, SYNCING, OFFLINE }

data class SyncStatus(val state: SyncState, val pendingCount: Int)

object SyncStatusRules {

    /** What the offline / syncing banner shows. Nothing is pending when there is no account to sync to. */
    fun statusOf(online: Boolean, syncing: Boolean, pending: Int, canSync: Boolean): SyncStatus {
        val waiting = if (canSync) pending else 0
        val state = when {
            !online -> SyncState.OFFLINE
            syncing && waiting > 0 -> SyncState.SYNCING
            waiting > 0 -> SyncState.PENDING
            else -> SyncState.SYNCED
        }
        return SyncStatus(state, waiting)
    }
}

/** Combines the network, the WorkManager sync job and the pending-row counts into one status. */
class SyncStatusMonitor(
    context: Context,
    medicationDao: MedicationDao,
    doseLogDao: DoseLogDao,
    healthDao: HealthMeasurementDao,
    private val canSync: () -> Boolean
) {
    private val appContext = context.applicationContext

    private val pending: Flow<Int> = combine(
        medicationDao.observePendingCount(),
        doseLogDao.observePendingCount(),
        healthDao.observePendingCount()
    ) { meds, doses, health -> meds + doses + health }

    private val syncing: Flow<Boolean> = WorkManager.getInstance(appContext)
        .getWorkInfosForUniqueWorkFlow(SyncScheduler.UNIQUE_WORK_NAME)
        .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } }

    val status: Flow<SyncStatus> = combine(online(), syncing, pending) { online, running, count ->
        SyncStatusRules.statusOf(online, running, count, canSync())
    }.distinctUntilChanged()

    /** https://developer.android.com/develop/connectivity/network-ops/reading-network-state */
    private fun online(): Flow<Boolean> = callbackFlow {
        val manager = appContext.getSystemService(ConnectivityManager::class.java)
        fun current(): Boolean = manager.getNetworkCapabilities(manager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(current())
            }
        }
        trySend(current())
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}

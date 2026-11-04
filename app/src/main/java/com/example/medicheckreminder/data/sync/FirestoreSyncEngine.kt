package com.example.medicheckreminder.data.sync

import android.content.Context
import android.util.Log
import com.example.medicheckreminder.data.local.DoseLogDao
import com.example.medicheckreminder.data.local.HealthMeasurementDao
import com.example.medicheckreminder.data.local.MedicationDao
import com.example.medicheckreminder.data.sync.SyncMappers.toRemote
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * Two-way sync between Room (the phone) and Firestore at `users/{uid}/{collection}/{id}`.
 *
 * Every record is compared with [SyncResolver] (last write wins on `updatedAt`). Records changed
 * offline carry pendingSync = true and are sent here when WorkManager sees a network again; records
 * changed on another phone are pulled down, so a new phone gets the same data after signing in.
 *
 * Firestore docs: https://firebase.google.com/docs/firestore/manage-data/transactions#batched-writes
 */
class FirestoreSyncEngine(
    context: Context,
    private val medicationDao: MedicationDao,
    private val doseLogDao: DoseLogDao,
    private val healthDao: HealthMeasurementDao
) {

    private val ownerPrefs = context.applicationContext
        .getSharedPreferences(OWNER_PREFS, Context.MODE_PRIVATE)

    data class Report(val pushed: Int, val pulled: Int, val deleted: Int)

    private class Table<T>(
        val collection: String,
        val loadLocal: suspend () -> List<T>,
        val stamp: (T) -> SyncStamp,
        val toRemote: (T) -> Map<String, Any?>,
        val fromRemote: (String, Map<String, Any?>) -> T,
        val save: suspend (T) -> Unit,
        val deleteLocal: suspend (String) -> Unit,
        val markSynced: suspend (String, Long) -> Unit
    )

    private val tables: List<Table<*>> = listOf(
        Table(
            collection = "medications",
            loadLocal = { medicationDao.allIncludingDeleted() },
            stamp = { SyncStamp(it.id, it.updatedAt, it.pendingSync, it.deleted) },
            toRemote = { it.toRemote() },
            fromRemote = SyncMappers::medicationFromRemote,
            save = { medicationDao.upsert(it) },
            deleteLocal = { medicationDao.deleteById(it) },
            markSynced = { id, at -> medicationDao.markSynced(id, at) }
        ),
        Table(
            collection = "doseLogs",
            loadLocal = { doseLogDao.all() },
            stamp = { SyncStamp(it.id, it.updatedAt, it.pendingSync) },
            toRemote = { it.toRemote() },
            fromRemote = SyncMappers::doseLogFromRemote,
            save = { doseLogDao.upsert(it) },
            deleteLocal = { doseLogDao.deleteById(it) },
            markSynced = { id, at -> doseLogDao.markSynced(id, at) }
        ),
        Table(
            collection = "health",
            loadLocal = { healthDao.all() },
            stamp = { SyncStamp(it.id, it.updatedAt, it.pendingSync) },
            toRemote = { it.toRemote() },
            fromRemote = SyncMappers::healthFromRemote,
            save = { healthDao.insert(it) },
            deleteLocal = { healthDao.deleteById(it) },
            markSynced = { id, at -> healthDao.markSynced(id, at) }
        )
    )

    suspend fun sync(uid: String): Report {
        claimLocalData(uid)
        var pushed = 0
        var pulled = 0
        var deleted = 0
        tables.forEach { table ->
            val report = syncTable(uid, table)
            pushed += report.pushed
            pulled += report.pulled
            deleted += report.deleted
        }
        Log.d(TAG, "Sync finished: pushed=$pushed pulled=$pulled deleted=$deleted")
        return Report(pushed, pulled, deleted)
    }

    private suspend fun <T> syncTable(uid: String, table: Table<T>): Report {
        val ref = FirebaseFirestore.getInstance()
            .collection(USERS).document(uid).collection(table.collection)
        // Read from the server, not Firestore's own cache, so the comparison is with the latest copy.
        val remote = ref.get(Source.SERVER).await().documents.associate { it.id to (it.data ?: emptyMap()) }
        val local = table.loadLocal().associateBy { table.stamp(it).id }

        val plan = SyncResolver.plan(
            local = local.values.map(table.stamp),
            remote = remote.map { (id, data) -> SyncMappers.stampOf(id, data) }
        )

        val toPush = plan.filterValues { it == SyncAction.PUSH }.keys.mapNotNull { local[it] }
        toPush.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = FirebaseFirestore.getInstance().batch()
            chunk.forEach { row -> batch.set(ref.document(table.stamp(row).id), table.toRemote(row), SetOptions.merge()) }
            batch.commit().await()
        }
        toPush.forEach { row ->
            val stamp = table.stamp(row)
            // A delete that reached Firestore no longer needs its local tombstone.
            if (stamp.deleted) table.deleteLocal(stamp.id) else table.markSynced(stamp.id, stamp.updatedAt)
        }

        var pulled = 0
        var deleted = 0
        plan.forEach { (id, action) ->
            when (action) {
                SyncAction.PULL -> {
                    table.save(table.fromRemote(id, remote.getValue(id)))
                    pulled++
                }
                SyncAction.DELETE_LOCAL -> {
                    table.deleteLocal(id)
                    deleted++
                }
                SyncAction.PUSH, SyncAction.NONE -> Unit
            }
        }
        Log.d(TAG, "${table.collection}: pushed=${toPush.size} pulled=$pulled deleted=$deleted")
        return Report(toPush.size, pulled, deleted)
    }

    /**
     * The phone keeps one person's data. When a different account signs in, the previous
     * account's rows are cleared first so they are never uploaded to the new account.
     */
    private suspend fun claimLocalData(uid: String) {
        val owner = ownerPrefs.getString(KEY_OWNER_UID, null)
        if (owner != null && owner != uid) {
            Log.d(TAG, "Different account signed in; clearing the previous account's local data")
            medicationDao.deleteAll()
            doseLogDao.deleteAll()
            healthDao.deleteAll()
        }
        ownerPrefs.edit().putString(KEY_OWNER_UID, uid).apply()
    }

    private companion object {
        const val TAG = "FirestoreSync"
        const val USERS = "users"
        const val BATCH_LIMIT = 400
        const val OWNER_PREFS = "medicheck_sync"
        const val KEY_OWNER_UID = "owner_uid"
    }
}

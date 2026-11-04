package com.example.medicheckreminder.data.sync

/** The parts of a record that decide which copy wins: the phone's (local) or Firestore's (remote). */
data class SyncStamp(
    val id: String,
    val updatedAt: Long,
    val pendingSync: Boolean = false,
    val deleted: Boolean = false
)

enum class SyncAction {
    /** Send the phone's copy to Firestore. */
    PUSH,

    /** Replace the phone's copy with Firestore's. */
    PULL,

    /** Firestore says the record was deleted on another phone. */
    DELETE_LOCAL,

    /** Both copies already agree. */
    NONE
}

/**
 * Conflict rule from the Part 1 design: last write wins, by comparing `updatedAt` timestamps.
 * A change made offline keeps its timestamp, so it still wins on reconnect if it is the newest.
 */
object SyncResolver {

    fun decide(local: SyncStamp?, remote: SyncStamp?): SyncAction = when {
        local == null && remote == null -> SyncAction.NONE
        local == null -> if (remote!!.deleted) SyncAction.NONE else SyncAction.PULL
        // Firestore has no copy (new record, or removed in the console): keep the phone's data.
        remote == null -> SyncAction.PUSH
        local.updatedAt > remote.updatedAt -> SyncAction.PUSH
        remote.updatedAt > local.updatedAt ->
            if (remote.deleted) SyncAction.DELETE_LOCAL else SyncAction.PULL
        // Same timestamp: an unsent local change is sent again (writes are idempotent).
        local.pendingSync -> SyncAction.PUSH
        else -> SyncAction.NONE
    }

    /** Pairs every record by id and decides each one. */
    fun plan(local: List<SyncStamp>, remote: List<SyncStamp>): Map<String, SyncAction> {
        val localById = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }
        return (localById.keys + remoteById.keys).associateWith { id ->
            decide(localById[id], remoteById[id])
        }
    }
}

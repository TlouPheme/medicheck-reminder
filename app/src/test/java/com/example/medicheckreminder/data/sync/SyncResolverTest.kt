package com.example.medicheckreminder.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncResolverTest {

    private fun local(updatedAt: Long, pending: Boolean = false, deleted: Boolean = false) =
        SyncStamp("m1", updatedAt, pending, deleted)

    private fun remote(updatedAt: Long, deleted: Boolean = false) =
        SyncStamp("m1", updatedAt, deleted = deleted)

    @Test
    fun recordMadeOffline_isPushed() {
        assertEquals(SyncAction.PUSH, SyncResolver.decide(local(100, pending = true), null))
    }

    @Test
    fun recordOnlyInFirestore_isPulledToThePhone() {
        assertEquals(SyncAction.PULL, SyncResolver.decide(null, remote(100)))
    }

    @Test
    fun deletedRecordOnlyInFirestore_isIgnored() {
        assertEquals(SyncAction.NONE, SyncResolver.decide(null, remote(100, deleted = true)))
    }

    @Test
    fun lastWriteWins_newerPhoneChangeIsPushed() {
        assertEquals(SyncAction.PUSH, SyncResolver.decide(local(200, pending = true), remote(100)))
    }

    @Test
    fun lastWriteWins_newerFirestoreChangeReplacesThePhone() {
        // Even a pending offline edit loses if another phone changed the record later.
        assertEquals(SyncAction.PULL, SyncResolver.decide(local(100, pending = true), remote(200)))
    }

    @Test
    fun newerDeleteFromAnotherPhone_removesTheLocalCopy() {
        assertEquals(SyncAction.DELETE_LOCAL, SyncResolver.decide(local(100), remote(200, deleted = true)))
    }

    @Test
    fun localDeleteMadeOffline_isPushed() {
        assertEquals(SyncAction.PUSH, SyncResolver.decide(local(300, pending = true, deleted = true), remote(100)))
    }

    @Test
    fun sameTimestamp_onlyPushesWhenStillPending() {
        assertEquals(SyncAction.NONE, SyncResolver.decide(local(100), remote(100)))
        assertEquals(SyncAction.PUSH, SyncResolver.decide(local(100, pending = true), remote(100)))
    }

    @Test
    fun plan_coversRecordsFromBothSides() {
        val plan = SyncResolver.plan(
            local = listOf(SyncStamp("a", 100, pendingSync = true), SyncStamp("b", 100)),
            remote = listOf(SyncStamp("b", 100), SyncStamp("c", 50))
        )
        assertEquals(mapOf("a" to SyncAction.PUSH, "b" to SyncAction.NONE, "c" to SyncAction.PULL), plan)
    }
}

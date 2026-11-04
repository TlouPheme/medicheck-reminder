package com.example.medicheckreminder.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStatusRulesTest {

    @Test
    fun offline_isShownWhetherOrNotAnythingIsWaiting() {
        assertEquals(SyncStatus(SyncState.OFFLINE, 3), SyncStatusRules.statusOf(false, false, 3, true))
        assertEquals(SyncStatus(SyncState.OFFLINE, 0), SyncStatusRules.statusOf(false, false, 0, true))
    }

    @Test
    fun onlineWithChangesWaiting_isPendingThenSyncing() {
        assertEquals(SyncState.PENDING, SyncStatusRules.statusOf(true, false, 2, true).state)
        assertEquals(SyncState.SYNCING, SyncStatusRules.statusOf(true, true, 2, true).state)
    }

    @Test
    fun nothingWaiting_hidesTheBanner() {
        assertEquals(SyncState.SYNCED, SyncStatusRules.statusOf(true, true, 0, true).state)
    }

    @Test
    fun withoutAnAccountToSyncTo_nothingCountsAsWaiting() {
        assertEquals(SyncStatus(SyncState.SYNCED, 0), SyncStatusRules.statusOf(true, false, 5, false))
    }
}

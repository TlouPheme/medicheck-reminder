package com.example.medicheckreminder.reminder

import com.example.medicheckreminder.domain.model.Dose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPolicyTest {

    @Test
    fun alarm_isTenMinutesAfterTheReminder() {
        assertEquals(1_000L + 10 * 60_000L, AlarmPolicy.alarmAt(1_000L))
    }

    @Test
    fun unansweredDose_rings() {
        assertTrue(AlarmPolicy.shouldRing(null))
        assertTrue(AlarmPolicy.shouldRing(Dose.Status.PENDING))
        assertTrue(AlarmPolicy.shouldRing(Dose.Status.MISSED))
    }

    @Test
    fun answeredDose_staysQuiet() {
        assertFalse(AlarmPolicy.shouldRing(Dose.Status.TAKEN))
        assertFalse(AlarmPolicy.shouldRing(Dose.Status.SKIPPED))
        assertFalse(AlarmPolicy.shouldRing(Dose.Status.SNOOZED))
    }
}

package com.example.medicheckreminder.data.sync

import com.example.medicheckreminder.data.local.DoseLogEntity
import com.example.medicheckreminder.data.local.HealthMeasurementEntity
import com.example.medicheckreminder.data.local.MedicationEntity
import com.example.medicheckreminder.data.sync.SyncMappers.toRemote
import com.example.medicheckreminder.domain.model.Frequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SyncMappersTest {

    private val medication = MedicationEntity(
        id = "m1",
        name = "Metformin",
        dosage = "500 mg",
        stockCount = 28,
        frequency = Frequency.EVERY_X_HOURS,
        times = listOf("06:00"),
        daysOfWeek = listOf(1, 3),
        intervalHours = 8,
        notes = "With food",
        createdAt = 1_000L,
        updatedAt = 2_000L,
        pendingSync = true,
        interactionNotes = "Avoid alcohol",
        deleted = false
    )

    @Test
    fun medication_roundTripsThroughFirestore_andArrivesSynced() {
        val back = SyncMappers.medicationFromRemote("m1", medication.toRemote())
        assertEquals(medication.copy(pendingSync = false), back)
    }

    @Test
    fun firestoreLongs_areReadAsInts() {
        // Firestore hands whole numbers back as Long.
        val data = medication.toRemote() + mapOf("stockCount" to 28L, "intervalHours" to 8L, "daysOfWeek" to listOf(1L, 3L))
        val back = SyncMappers.medicationFromRemote("m1", data)
        assertEquals(28, back.stockCount)
        assertEquals(8, back.intervalHours)
        assertEquals(listOf(1, 3), back.daysOfWeek)
    }

    @Test
    fun missingFields_fallBackToSafeDefaults() {
        val back = SyncMappers.medicationFromRemote("m1", mapOf("name" to "Aspirin"))
        assertEquals(Frequency.DAILY, back.frequency)
        assertEquals(0L, back.updatedAt)
        assertFalse(back.deleted)
    }

    @Test
    fun doseLog_roundTrips() {
        val log = DoseLogEntity("m1|2026-11-04|08:00", "Metformin", "500 mg", "08:00", "2026-11-04", "TAKEN", 5_000L, true)
        assertEquals(log.copy(pendingSync = false), SyncMappers.doseLogFromRemote(log.id, log.toRemote()))
    }

    @Test
    fun healthReading_roundTrips_withDoublesFromFirestore() {
        val reading = HealthMeasurementEntity("h1", "BLOOD_PRESSURE", 120f, 80f, 3_000L, 4_000L, true)
        assertEquals(reading.copy(pendingSync = false), SyncMappers.healthFromRemote("h1", reading.toRemote()))
    }

    @Test
    fun stamp_readsTimestampAndTombstone() {
        val stamp = SyncMappers.stampOf("m1", medication.copy(deleted = true).toRemote())
        assertEquals(SyncStamp("m1", 2_000L, deleted = true), stamp)
    }
}

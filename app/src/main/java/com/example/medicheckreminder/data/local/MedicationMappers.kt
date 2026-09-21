package com.example.medicheckreminder.data.local

import com.example.medicheckreminder.domain.model.Medication

fun MedicationEntity.toDomain(): Medication = Medication(
    id = id,
    name = name,
    dosage = dosage,
    stockCount = stockCount,
    frequency = frequency,
    times = times,
    daysOfWeek = daysOfWeek.toSet(),
    intervalHours = intervalHours,
    notes = notes,
    createdAt = createdAt
)

fun Medication.toEntity(pendingSync: Boolean = true): MedicationEntity = MedicationEntity(
    id = id,
    name = name,
    dosage = dosage,
    stockCount = stockCount,
    frequency = frequency,
    times = times,
    daysOfWeek = daysOfWeek.sorted(),
    intervalHours = intervalHours,
    notes = notes,
    createdAt = createdAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis(),
    pendingSync = pendingSync
)

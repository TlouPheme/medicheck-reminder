package com.example.medicheckreminder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.domain.model.DoseLog

@Entity(tableName = "dose_logs")
data class DoseLogEntity(
    @PrimaryKey val id: String,
    val medicationName: String,
    val dosage: String,
    val scheduledTime: String,
    val dateKey: String,
    val status: String
)

fun DoseLogEntity.toDomain(): DoseLog = DoseLog(
    id = id,
    medicationName = medicationName,
    dosage = dosage,
    scheduledTime = scheduledTime,
    dateKey = dateKey,
    status = runCatching { Dose.Status.valueOf(status) }.getOrDefault(Dose.Status.PENDING)
)

fun Dose.toLogEntity(): DoseLogEntity = DoseLogEntity(
    id = id,
    medicationName = medicationName,
    dosage = dosage,
    scheduledTime = scheduledTime,
    dateKey = dateKey,
    status = status.name
)

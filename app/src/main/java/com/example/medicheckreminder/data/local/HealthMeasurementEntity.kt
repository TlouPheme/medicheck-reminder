package com.example.medicheckreminder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.medicheckreminder.domain.model.HealthMeasurement
import com.example.medicheckreminder.domain.model.MeasurementType

@Entity(tableName = "health_measurements")
data class HealthMeasurementEntity(
    @PrimaryKey val id: String,
    val type: String,
    val value: Float,
    val secondaryValue: Float?,
    val recordedAtMillis: Long
)

fun HealthMeasurementEntity.toDomain(): HealthMeasurement = HealthMeasurement(
    id = id,
    type = runCatching { MeasurementType.valueOf(type) }.getOrDefault(MeasurementType.WEIGHT),
    value = value,
    secondaryValue = secondaryValue,
    recordedAtMillis = recordedAtMillis
)

fun HealthMeasurement.toEntity(): HealthMeasurementEntity = HealthMeasurementEntity(
    id = id,
    type = type.name,
    value = value,
    secondaryValue = secondaryValue,
    recordedAtMillis = recordedAtMillis
)

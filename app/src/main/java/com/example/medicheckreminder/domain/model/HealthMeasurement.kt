package com.example.medicheckreminder.domain.model

enum class MeasurementType {
    WEIGHT,
    BLOOD_PRESSURE,
    BLOOD_SUGAR
}

data class MeasurementTarget(
    val min: Float,
    val max: Float,
    val secondaryMin: Float? = null,
    val secondaryMax: Float? = null
)

data class HealthMeasurement(
    val id: String,
    val type: MeasurementType,
    val value: Float,
    val secondaryValue: Float? = null,
    val recordedAtMillis: Long
)

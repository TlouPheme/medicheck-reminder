package com.example.medicheckreminder.domain.repository

import com.example.medicheckreminder.domain.model.HealthMeasurement
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    fun observeAll(): Flow<List<HealthMeasurement>>
    suspend fun add(measurement: HealthMeasurement)
}

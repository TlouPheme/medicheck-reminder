package com.example.medicheckreminder.data.repository

import com.example.medicheckreminder.data.local.HealthMeasurementDao
import com.example.medicheckreminder.data.local.toDomain
import com.example.medicheckreminder.data.local.toEntity
import com.example.medicheckreminder.domain.model.HealthMeasurement
import com.example.medicheckreminder.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HealthRepositoryImpl(
    private val dao: HealthMeasurementDao
) : HealthRepository {

    override fun observeAll(): Flow<List<HealthMeasurement>> {
        return dao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun add(measurement: HealthMeasurement) {
        dao.insert(measurement.toEntity())
    }
}

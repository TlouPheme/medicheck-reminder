package com.example.medicheckreminder.data.repository

import com.example.medicheckreminder.data.local.DoseLogDao
import com.example.medicheckreminder.data.local.toDomain
import com.example.medicheckreminder.data.local.toLogEntity
import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.domain.model.DoseLog
import com.example.medicheckreminder.domain.repository.DoseLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DoseLogRepositoryImpl(
    private val dao: DoseLogDao
) : DoseLogRepository {

    override fun observeAll(): Flow<List<DoseLog>> {
        return dao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun record(dose: Dose) {
        dao.upsert(dose.toLogEntity())
    }
}

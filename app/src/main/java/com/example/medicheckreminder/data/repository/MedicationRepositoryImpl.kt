package com.example.medicheckreminder.data.repository

import com.example.medicheckreminder.data.local.MedicationDao
import com.example.medicheckreminder.data.local.toDomain
import com.example.medicheckreminder.data.local.toEntity
import com.example.medicheckreminder.domain.model.Medication
import com.example.medicheckreminder.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MedicationRepositoryImpl(
    private val dao: MedicationDao
) : MedicationRepository {

    override fun observeAll(): Flow<List<Medication>> {
        return dao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getById(id: String): Medication? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun upsert(medication: Medication) {
        dao.upsert(medication.toEntity(pendingSync = true))
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}

package com.example.medicheckreminder.domain.repository

import com.example.medicheckreminder.domain.model.Medication
import kotlinx.coroutines.flow.Flow

interface MedicationRepository {
    fun observeAll(): Flow<List<Medication>>
    suspend fun getById(id: String): Medication?
    suspend fun upsert(medication: Medication)
    suspend fun delete(id: String)
}

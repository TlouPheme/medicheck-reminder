package com.example.medicheckreminder.data.repository

import android.util.Log
import com.example.medicheckreminder.data.local.DoseLogDao
import com.example.medicheckreminder.data.local.MedicationDao
import com.example.medicheckreminder.data.local.toDomain
import com.example.medicheckreminder.data.local.toLogEntity
import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.domain.model.DoseLog
import com.example.medicheckreminder.domain.model.Medication
import com.example.medicheckreminder.domain.refill.RefillCalculator
import com.example.medicheckreminder.domain.repository.DoseLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DoseLogRepositoryImpl(
    private val dao: DoseLogDao,
    private val medicationDao: MedicationDao,
    private val onLocalChange: () -> Unit = {},
    private val onLowStock: (Medication) -> Unit = {}
) : DoseLogRepository {

    override fun observeAll(): Flow<List<DoseLog>> {
        return dao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun record(dose: Dose) {
        val previous = dao.getById(dose.id)?.toDomain()?.status
        dao.upsert(dose.toLogEntity())
        val change = RefillCalculator.stockChange(previous, dose.status)
        if (change != 0) {
            adjustStock(dose.medicationId.ifBlank { dose.id.substringBefore('|') }, change)
        }
        onLocalChange()
    }

    /** Refill calculator: Taken uses one from stock, undoing a Taken puts it back. */
    private suspend fun adjustStock(medicationId: String, change: Int) {
        val entity = medicationDao.getById(medicationId)?.takeUnless { it.deleted } ?: return
        val before = entity.toDomain()
        val updated = entity.copy(
            stockCount = (entity.stockCount + change).coerceAtLeast(0),
            updatedAt = System.currentTimeMillis(),
            pendingSync = true
        )
        medicationDao.upsert(updated)
        Log.d(TAG, "${entity.name} stock ${entity.stockCount} -> ${updated.stockCount}")
        val after = updated.toDomain()
        if (RefillCalculator.becameLow(before, after)) onLowStock(after)
    }

    private companion object {
        const val TAG = "DoseLogRepository"
    }
}

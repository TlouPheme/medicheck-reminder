package com.example.medicheckreminder.data.sync

import com.example.medicheckreminder.data.local.DoseLogEntity
import com.example.medicheckreminder.data.local.HealthMeasurementEntity
import com.example.medicheckreminder.data.local.MedicationEntity
import com.example.medicheckreminder.domain.model.Frequency

/**
 * Converts Room rows to Firestore documents and back. Firestore returns whole numbers as Long and
 * decimals as Double, so every number is read through [Number]. A record read back from Firestore
 * is already in sync, so its pendingSync flag is false.
 */
object SyncMappers {

    const val FIELD_UPDATED_AT = "updatedAt"
    const val FIELD_DELETED = "deleted"

    fun MedicationEntity.toRemote(): Map<String, Any?> = mapOf(
        "name" to name,
        "dosage" to dosage,
        "stockCount" to stockCount,
        "frequency" to frequency.name,
        "times" to times,
        "daysOfWeek" to daysOfWeek,
        "intervalHours" to intervalHours,
        "notes" to notes,
        "interactionNotes" to interactionNotes,
        "createdAt" to createdAt,
        FIELD_UPDATED_AT to updatedAt,
        FIELD_DELETED to deleted
    )

    fun medicationFromRemote(id: String, data: Map<String, Any?>): MedicationEntity = MedicationEntity(
        id = id,
        name = data.string("name"),
        dosage = data.string("dosage"),
        stockCount = data.long("stockCount").toInt(),
        frequency = runCatching { Frequency.valueOf(data.string("frequency")) }.getOrDefault(Frequency.DAILY),
        times = data.list("times").map { it.toString() },
        daysOfWeek = data.list("daysOfWeek").mapNotNull { (it as? Number)?.toInt() },
        intervalHours = (data["intervalHours"] as? Number)?.toInt(),
        notes = data.string("notes"),
        createdAt = data.long("createdAt"),
        updatedAt = data.long(FIELD_UPDATED_AT),
        pendingSync = false,
        interactionNotes = data.string("interactionNotes"),
        deleted = data[FIELD_DELETED] as? Boolean ?: false
    )

    fun DoseLogEntity.toRemote(): Map<String, Any?> = mapOf(
        "medicationName" to medicationName,
        "dosage" to dosage,
        "scheduledTime" to scheduledTime,
        "dateKey" to dateKey,
        "status" to status,
        FIELD_UPDATED_AT to updatedAt
    )

    fun doseLogFromRemote(id: String, data: Map<String, Any?>): DoseLogEntity = DoseLogEntity(
        id = id,
        medicationName = data.string("medicationName"),
        dosage = data.string("dosage"),
        scheduledTime = data.string("scheduledTime"),
        dateKey = data.string("dateKey"),
        status = data.string("status"),
        updatedAt = data.long(FIELD_UPDATED_AT),
        pendingSync = false
    )

    fun HealthMeasurementEntity.toRemote(): Map<String, Any?> = mapOf(
        "type" to type,
        "value" to value.toDouble(),
        "secondaryValue" to secondaryValue?.toDouble(),
        "recordedAtMillis" to recordedAtMillis,
        FIELD_UPDATED_AT to updatedAt
    )

    fun healthFromRemote(id: String, data: Map<String, Any?>): HealthMeasurementEntity = HealthMeasurementEntity(
        id = id,
        type = data.string("type"),
        value = (data["value"] as? Number)?.toFloat() ?: 0f,
        secondaryValue = (data["secondaryValue"] as? Number)?.toFloat(),
        recordedAtMillis = data.long("recordedAtMillis"),
        updatedAt = data.long(FIELD_UPDATED_AT),
        pendingSync = false
    )

    /** The fields of a Firestore document that [SyncResolver] needs. */
    fun stampOf(id: String, data: Map<String, Any?>) = SyncStamp(
        id = id,
        updatedAt = data.long(FIELD_UPDATED_AT),
        deleted = data[FIELD_DELETED] as? Boolean ?: false
    )

    private fun Map<String, Any?>.string(key: String): String = this[key] as? String ?: ""

    private fun Map<String, Any?>.long(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L

    private fun Map<String, Any?>.list(key: String): List<Any> =
        (this[key] as? List<*>)?.filterNotNull() ?: emptyList()
}

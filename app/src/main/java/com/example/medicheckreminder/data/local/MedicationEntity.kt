package com.example.medicheckreminder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.medicheckreminder.domain.model.Frequency

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dosage: String,
    val stockCount: Int,
    val frequency: Frequency,
    val times: List<String>,
    val daysOfWeek: List<Int>,
    val intervalHours: Int?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pendingSync: Boolean
)

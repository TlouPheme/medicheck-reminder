package com.example.medicheckreminder.domain.model

data class DoseLog(
    val id: String,
    val medicationName: String,
    val dosage: String,
    val scheduledTime: String,
    val dateKey: String,
    val status: Dose.Status
)

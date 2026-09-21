package com.example.medicheckreminder.domain.model

data class Dose(
    val id: String,
    val medicationName: String,
    val dosage: String,
    val scheduledTime: String, // HH:mm format
    val status: Status = Status.PENDING,
    val medicationId: String = "",
    val dateKey: String = ""
) {
    enum class Status {
        PENDING, TAKEN, SKIPPED, SNOOZED, MISSED
    }
}

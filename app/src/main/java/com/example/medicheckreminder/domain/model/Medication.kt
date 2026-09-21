package com.example.medicheckreminder.domain.model

enum class Frequency {
    DAILY,
    SPECIFIC_DAYS,
    EVERY_X_HOURS,
    AS_NEEDED
}

data class Medication(
    val id: String,
    val name: String,
    val dosage: String = "",
    val stockCount: Int = 0,
    val frequency: Frequency = Frequency.DAILY,
    val times: List<String> = emptyList(),
    val daysOfWeek: Set<Int> = emptySet(),
    val intervalHours: Int? = null,
    val notes: String = "",
    val createdAt: Long = 0L
)

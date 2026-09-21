package com.example.medicheckreminder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MedicationEntity::class, DoseLogEntity::class, HealthMeasurementEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(MedicationConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun healthMeasurementDao(): HealthMeasurementDao
}

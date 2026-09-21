package com.example.medicheckreminder.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.medicheckreminder.data.local.AppDatabase
import com.example.medicheckreminder.data.repository.DoseLogRepositoryImpl
import com.example.medicheckreminder.data.repository.MedicationRepositoryImpl
import com.example.medicheckreminder.data.repository.SettingsRepositoryImpl
import com.example.medicheckreminder.domain.repository.DoseLogRepository
import com.example.medicheckreminder.domain.repository.MedicationRepository
import com.example.medicheckreminder.domain.repository.SettingsRepository

class AppContainer(context: Context) {
    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "medicheck.db"
    ).addMigrations(MIGRATION_2_3)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    val medicationRepository: MedicationRepository =
        MedicationRepositoryImpl(database.medicationDao())

    val doseLogRepository: DoseLogRepository =
        DoseLogRepositoryImpl(database.doseLogDao())

    val settingsRepository: SettingsRepository =
        SettingsRepositoryImpl(context)

    private companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE medications ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE medications SET createdAt = updatedAt WHERE createdAt = 0"
                )
            }
        }
    }
}

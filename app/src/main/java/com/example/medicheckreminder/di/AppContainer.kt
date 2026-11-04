package com.example.medicheckreminder.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.medicheckreminder.data.local.AppDatabase
import com.example.medicheckreminder.data.remote.FirebaseAuthGateway
import com.example.medicheckreminder.data.repository.AccountStore
import com.example.medicheckreminder.data.repository.DoseLogRepositoryImpl
import com.example.medicheckreminder.data.repository.HealthRepositoryImpl
import com.example.medicheckreminder.data.repository.MedicationRepositoryImpl
import com.example.medicheckreminder.data.repository.ProfilePhotoRepository
import com.example.medicheckreminder.data.repository.SettingsRepositoryImpl
import com.example.medicheckreminder.data.sync.FirestoreSyncEngine
import com.example.medicheckreminder.data.sync.SyncStatusMonitor
import com.example.medicheckreminder.reminder.RefillNotifier
import com.example.medicheckreminder.worker.SyncScheduler
import com.example.medicheckreminder.domain.repository.DoseLogRepository
import com.example.medicheckreminder.domain.repository.HealthRepository
import com.example.medicheckreminder.domain.repository.MedicationRepository
import com.example.medicheckreminder.domain.repository.SettingsRepository

class AppContainer(context: Context) {
    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "medicheck.db"
    ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    private val appContext = context.applicationContext

    /** Every local write queues a sync; WorkManager runs it once the phone is online. */
    private val onLocalChange: () -> Unit = { SyncScheduler.enqueue(appContext) }

    val medicationRepository: MedicationRepository =
        MedicationRepositoryImpl(database.medicationDao(), onLocalChange)

    val doseLogRepository: DoseLogRepository =
        DoseLogRepositoryImpl(
            dao = database.doseLogDao(),
            medicationDao = database.medicationDao(),
            onLocalChange = onLocalChange,
            onLowStock = { RefillNotifier.showLowStock(appContext, it) }
        )

    val healthRepository: HealthRepository =
        HealthRepositoryImpl(database.healthMeasurementDao(), onLocalChange)

    val firebaseAuthGateway: FirebaseAuthGateway = FirebaseAuthGateway(context)

    val syncEngine: FirestoreSyncEngine = FirestoreSyncEngine(
        context = appContext,
        medicationDao = database.medicationDao(),
        doseLogDao = database.doseLogDao(),
        healthDao = database.healthMeasurementDao()
    )

    val syncStatusMonitor: SyncStatusMonitor = SyncStatusMonitor(
        context = appContext,
        medicationDao = database.medicationDao(),
        doseLogDao = database.doseLogDao(),
        healthDao = database.healthMeasurementDao(),
        canSync = { firebaseAuthGateway.isAvailable && firebaseAuthGateway.currentUser != null }
    )

    val accountStore: AccountStore = AccountStore(context, firebaseAuthGateway)

    val settingsRepository: SettingsRepository =
        SettingsRepositoryImpl(context)

    val profilePhotoRepository: ProfilePhotoRepository =
        ProfilePhotoRepository(context.applicationContext, accountStore)

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS health_measurements (
                        id TEXT NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL,
                        value REAL NOT NULL,
                        secondaryValue REAL,
                        recordedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** Offline sync: timestamps and pending flags on every table, plus refill fields. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN interactionNotes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medications ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE medications SET pendingSync = 1")
                db.execSQL("ALTER TABLE dose_logs ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE dose_logs ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE health_measurements ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE health_measurements ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE health_measurements SET updatedAt = recordedAtMillis")
            }
        }
    }
}

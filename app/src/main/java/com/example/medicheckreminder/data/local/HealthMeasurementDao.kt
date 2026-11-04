package com.example.medicheckreminder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthMeasurementDao {
    @Query("SELECT * FROM health_measurements ORDER BY recordedAtMillis DESC")
    fun observeAll(): Flow<List<HealthMeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HealthMeasurementEntity)

    @Query("SELECT * FROM health_measurements")
    suspend fun all(): List<HealthMeasurementEntity>

    @Query("SELECT COUNT(*) FROM health_measurements WHERE pendingSync = 1")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE health_measurements SET pendingSync = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("DELETE FROM health_measurements WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM health_measurements")
    suspend fun deleteAll()
}

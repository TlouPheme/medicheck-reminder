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
}

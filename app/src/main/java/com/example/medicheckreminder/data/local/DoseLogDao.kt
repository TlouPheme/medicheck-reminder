package com.example.medicheckreminder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs")
    fun observeAll(): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE id = :id")
    suspend fun getById(id: String): DoseLogEntity?

    @Query("SELECT * FROM dose_logs")
    suspend fun all(): List<DoseLogEntity>

    @Query("SELECT COUNT(*) FROM dose_logs WHERE pendingSync = 1")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DoseLogEntity)

    @Query("UPDATE dose_logs SET pendingSync = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("DELETE FROM dose_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM dose_logs")
    suspend fun deleteAll()
}

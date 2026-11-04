package com.example.medicheckreminder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE deleted = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications")
    suspend fun allIncludingDeleted(): List<MedicationEntity>

    @Query("SELECT COUNT(*) FROM medications WHERE pendingSync = 1")
    fun observePendingCount(): Flow<Int>

    /** Soft delete, so the delete can be sent to Firestore later (offline mode). */
    @Query("UPDATE medications SET deleted = 1, pendingSync = 1, updatedAt = :now WHERE id = :id")
    suspend fun markDeleted(id: String, now: Long)

    /** Clears the flag only if the row was not changed again while it was being sent. */
    @Query("UPDATE medications SET pendingSync = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("DELETE FROM medications")
    suspend fun deleteAll()

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: String): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MedicationEntity)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteById(id: String)
}

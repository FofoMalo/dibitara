package com.dibitara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dibitara.app.data.local.entity.PatrimoineSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatrimoineSnapshotDao {

    @Query("SELECT * FROM patrimoine_snapshots ORDER BY snapshotEpochDay DESC")
    fun getAll(): Flow<List<PatrimoineSnapshotEntity>>

    // Vérifie si un snapshot existe déjà pour un jour donné (évite les doublons intra-journaliers)
    @Query("SELECT COUNT(*) FROM patrimoine_snapshots WHERE snapshotEpochDay = :epochDay")
    suspend fun countForDay(epochDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PatrimoineSnapshotEntity)
}

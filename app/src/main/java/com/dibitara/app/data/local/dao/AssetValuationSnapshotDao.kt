package com.dibitara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dibitara.app.data.local.entity.AssetValuationSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetValuationSnapshotDao {

    @Query("SELECT * FROM asset_valuation_snapshots WHERE assetType = :assetType AND assetId = :assetId ORDER BY snapshotEpochDay ASC")
    fun getForAsset(assetType: String, assetId: Long): Flow<List<AssetValuationSnapshotEntity>>

    // Vérifie si un snapshot existe déjà pour ce jour et cet actif (évite les doublons intra-journaliers)
    @Query("SELECT COUNT(*) FROM asset_valuation_snapshots WHERE assetType = :assetType AND assetId = :assetId AND snapshotEpochDay = :epochDay")
    suspend fun countForDay(assetType: String, assetId: Long, epochDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AssetValuationSnapshotEntity)

    @Query("DELETE FROM asset_valuation_snapshots WHERE assetType = :assetType AND assetId = :assetId")
    suspend fun deleteForAsset(assetType: String, assetId: Long)
}

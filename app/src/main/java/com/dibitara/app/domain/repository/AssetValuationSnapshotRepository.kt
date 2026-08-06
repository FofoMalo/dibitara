package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.AssetValuationSnapshot
import com.dibitara.app.domain.model.AssetValuationType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AssetValuationSnapshotRepository {
    fun getForAsset(assetType: AssetValuationType, assetId: Long): Flow<List<AssetValuationSnapshot>>
    suspend fun existsForDay(assetType: AssetValuationType, assetId: Long, date: LocalDate): Boolean
    suspend fun save(snapshot: AssetValuationSnapshot)
    suspend fun deleteForAsset(assetType: AssetValuationType, assetId: Long)
}

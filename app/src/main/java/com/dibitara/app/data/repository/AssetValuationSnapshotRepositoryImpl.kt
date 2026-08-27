package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.AssetValuationSnapshotDao
import com.dibitara.app.data.local.entity.AssetValuationSnapshotEntity
import com.dibitara.app.domain.model.AssetValuationSnapshot
import com.dibitara.app.domain.model.AssetValuationType
import com.dibitara.app.domain.repository.AssetValuationSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class AssetValuationSnapshotRepositoryImpl @Inject constructor(
    private val dao: AssetValuationSnapshotDao
) : AssetValuationSnapshotRepository {

    override fun getForAsset(assetType: AssetValuationType, assetId: Long): Flow<List<AssetValuationSnapshot>> =
        dao.getForAsset(assetType.name, assetId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun existsForDay(assetType: AssetValuationType, assetId: Long, date: LocalDate): Boolean =
        dao.countForDay(assetType.name, assetId, date.toEpochDay()) > 0

    override suspend fun save(snapshot: AssetValuationSnapshot) {
        dao.insert(AssetValuationSnapshotEntity.fromDomain(snapshot))
    }

    override suspend fun deleteForAsset(assetType: AssetValuationType, assetId: Long) {
        dao.deleteForAsset(assetType.name, assetId)
    }
}

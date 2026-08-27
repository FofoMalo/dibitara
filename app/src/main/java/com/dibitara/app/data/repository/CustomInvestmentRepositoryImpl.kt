package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.CustomAssetDao
import com.dibitara.app.data.local.dao.EmployeeSavingsDao
import com.dibitara.app.data.local.entity.CustomAssetEntity
import com.dibitara.app.data.local.entity.EmployeeSavingsEntity
import com.dibitara.app.domain.model.CustomAsset
import com.dibitara.app.domain.model.EmployeeSavings
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CustomInvestmentRepositoryImpl @Inject constructor(
    private val customAssetDao    : CustomAssetDao,
    private val employeeSavingsDao: EmployeeSavingsDao
) : CustomInvestmentRepository {

    // ─── Actifs libres ─────────────────────────────────────────────────────────

    override fun getAllCustomAssets(): Flow<List<CustomAsset>> =
        customAssetDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun saveCustomAsset(asset: CustomAsset): Result<Long> =
        runCatching { customAssetDao.insert(CustomAssetEntity.fromDomain(asset)) }

    override suspend fun updateCustomAsset(asset: CustomAsset) =
        customAssetDao.update(CustomAssetEntity.fromDomain(asset))

    override suspend fun deleteCustomAsset(asset: CustomAsset) =
        customAssetDao.delete(CustomAssetEntity.fromDomain(asset))

    // ─── Épargne salariale ─────────────────────────────────────────────────────

    override fun getAllEmployeeSavings(): Flow<List<EmployeeSavings>> =
        employeeSavingsDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun saveEmployeeSavings(savings: EmployeeSavings): Result<Long> =
        runCatching { employeeSavingsDao.insert(EmployeeSavingsEntity.fromDomain(savings)) }

    override suspend fun updateEmployeeSavings(savings: EmployeeSavings) =
        employeeSavingsDao.update(EmployeeSavingsEntity.fromDomain(savings))

    override suspend fun deleteEmployeeSavings(savings: EmployeeSavings) =
        employeeSavingsDao.delete(EmployeeSavingsEntity.fromDomain(savings))
}

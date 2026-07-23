package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.CustomAsset
import com.dibitara.app.domain.model.EmployeeSavings
import kotlinx.coroutines.flow.Flow

/**
 * Contrat unique pour les investissements personnalisés : actifs libres et épargne salariale.
 */
interface CustomInvestmentRepository {

    // ─── Actifs libres ─────────────────────────────────────────────────────────
    fun getAllCustomAssets(): Flow<List<CustomAsset>>
    suspend fun saveCustomAsset(asset: CustomAsset): Result<Long>
    suspend fun updateCustomAsset(asset: CustomAsset)
    suspend fun deleteCustomAsset(asset: CustomAsset)

    // ─── Épargne salariale ─────────────────────────────────────────────────────
    fun getAllEmployeeSavings(): Flow<List<EmployeeSavings>>
    suspend fun saveEmployeeSavings(savings: EmployeeSavings): Result<Long>
    suspend fun updateEmployeeSavings(savings: EmployeeSavings)
    suspend fun deleteEmployeeSavings(savings: EmployeeSavings)
}

package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.CustomAsset
import com.dibitara.app.domain.model.EmployeeSavings
import com.dibitara.app.domain.model.PreciousMetalAsset
import kotlinx.coroutines.flow.Flow

/**
 * Contrat unique pour les trois types d'investissements personnalisés :
 * métaux précieux, actifs libres, et épargne salariale.
 */
interface CustomInvestmentRepository {

    // ─── Métaux précieux ───────────────────────────────────────────────────────
    fun getAllPreciousMetals(): Flow<List<PreciousMetalAsset>>
    suspend fun savePreciousMetal(asset: PreciousMetalAsset): Result<Long>
    suspend fun updatePreciousMetal(asset: PreciousMetalAsset)
    suspend fun deletePreciousMetal(asset: PreciousMetalAsset)

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

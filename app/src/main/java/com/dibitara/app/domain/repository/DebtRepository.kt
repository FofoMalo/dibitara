package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.Debt
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    fun getAll(): Flow<List<Debt>>
    suspend fun save(debt: Debt): Result<Long>
    suspend fun delete(debt: Debt)
}

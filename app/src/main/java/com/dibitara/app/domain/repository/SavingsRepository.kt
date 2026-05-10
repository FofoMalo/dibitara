package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.SavingsAccount
import kotlinx.coroutines.flow.Flow

interface SavingsRepository {
    fun getAll(): Flow<List<SavingsAccount>>
    fun getByChild(childId: Long): Flow<List<SavingsAccount>>
    suspend fun save(account: SavingsAccount): Result<Long>
    suspend fun update(account: SavingsAccount)
    suspend fun delete(account: SavingsAccount)
}

package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.BankProvider
import kotlinx.coroutines.flow.Flow

interface BankAccountRepository {
    fun getAll(): Flow<List<BankAccount>>
    suspend fun findByProvider(provider: BankProvider): BankAccount?
    suspend fun upsert(account: BankAccount): Long
    suspend fun delete(account: BankAccount)
}

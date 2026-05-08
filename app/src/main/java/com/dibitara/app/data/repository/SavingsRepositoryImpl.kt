package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.SavingsAccountDao
import com.dibitara.app.data.local.entity.SavingsAccountEntity
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.repository.SavingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SavingsRepositoryImpl @Inject constructor(
    private val dao: SavingsAccountDao
) : SavingsRepository {

    override fun getAll(): Flow<List<SavingsAccount>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getByChild(childId: Long): Flow<List<SavingsAccount>> =
        dao.getByChild(childId).map { list -> list.map { it.toDomain() } }

    override suspend fun save(account: SavingsAccount): Result<Long> = runCatching {
        dao.insert(SavingsAccountEntity.fromDomain(account))
    }

    override suspend fun delete(account: SavingsAccount) {
        dao.delete(SavingsAccountEntity.fromDomain(account))
    }
}

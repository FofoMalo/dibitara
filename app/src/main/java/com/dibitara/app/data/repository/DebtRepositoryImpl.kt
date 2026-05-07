package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.DebtDao
import com.dibitara.app.data.local.entity.DebtEntity
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DebtRepositoryImpl @Inject constructor(
    private val dao: DebtDao
) : DebtRepository {

    override fun getAll(): Flow<List<Debt>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(debt: Debt): Result<Long> = runCatching {
        dao.insert(DebtEntity.fromDomain(debt))
    }

    override suspend fun delete(debt: Debt) {
        dao.delete(DebtEntity.fromDomain(debt))
    }
}

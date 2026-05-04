package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.BudgetDao
import com.dibitara.app.data.local.entity.BudgetEntity
import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val dao: BudgetDao
) : BudgetRepository {

    override fun getBudget(month: Int, year: Int): Flow<Budget?> =
        dao.getBudget(month, year).map { it?.toDomain() }

    override suspend fun upsert(budget: Budget) =
        dao.upsert(BudgetEntity.fromDomain(budget))
}

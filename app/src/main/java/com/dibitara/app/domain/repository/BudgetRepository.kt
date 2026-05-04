package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudget(month: Int, year: Int): Flow<Budget?>
    suspend fun upsert(budget: Budget)
}

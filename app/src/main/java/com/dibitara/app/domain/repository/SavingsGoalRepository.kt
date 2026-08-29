package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {
    fun getAll(): Flow<List<SavingsGoal>>
    suspend fun upsert(goal: SavingsGoal)
    suspend fun delete(goal: SavingsGoal)
}

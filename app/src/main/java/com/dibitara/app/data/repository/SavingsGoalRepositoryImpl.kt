package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.SavingsGoalDao
import com.dibitara.app.data.local.entity.SavingsGoalEntity
import com.dibitara.app.domain.model.SavingsGoal
import com.dibitara.app.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SavingsGoalRepositoryImpl @Inject constructor(
    private val dao: SavingsGoalDao
) : SavingsGoalRepository {

    override fun getAll(): Flow<List<SavingsGoal>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsert(goal: SavingsGoal) =
        dao.upsert(goal.toEntity())

    override suspend fun delete(goal: SavingsGoal) =
        dao.delete(goal.toEntity())

    // ─── Mapping entité ↔ domaine ─────────────────────────────────────────────
    // La conversion vit sur SavingsGoalEntity (réutilisée par la restauration JSON).

    private fun SavingsGoal.toEntity() = SavingsGoalEntity.fromDomain(this)
}

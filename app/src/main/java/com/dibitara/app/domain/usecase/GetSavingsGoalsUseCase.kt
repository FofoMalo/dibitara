package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.SavingsGoal
import com.dibitara.app.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Retourne tous les objectifs d'épargne définis par l'utilisateur, en temps réel. */
class GetSavingsGoalsUseCase @Inject constructor(
    private val repository: SavingsGoalRepository
) {
    operator fun invoke(): Flow<List<SavingsGoal>> = repository.getAll()
}

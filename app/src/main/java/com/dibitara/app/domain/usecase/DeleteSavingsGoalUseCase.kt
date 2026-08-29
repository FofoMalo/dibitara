package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.SavingsGoal
import com.dibitara.app.domain.repository.SavingsGoalRepository
import javax.inject.Inject

/** Supprime un objectif d'épargne. */
class DeleteSavingsGoalUseCase @Inject constructor(
    private val repository: SavingsGoalRepository
) {
    suspend operator fun invoke(goal: SavingsGoal) = repository.delete(goal)
}

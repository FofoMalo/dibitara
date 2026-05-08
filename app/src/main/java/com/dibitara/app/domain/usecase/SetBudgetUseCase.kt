package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.repository.BudgetRepository
import javax.inject.Inject

/**
 * Crée ou met à jour le budget d'un mois donné.
 * On valide que le montant est positif avant de persister.
 */
class SetBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget): Result<Unit> {
        if (budget.allocatedCents <= 0) {
            return Result.failure(IllegalArgumentException("Le budget doit être supérieur à 0"))
        }
        return runCatching { budgetRepository.upsert(budget) }
    }
}

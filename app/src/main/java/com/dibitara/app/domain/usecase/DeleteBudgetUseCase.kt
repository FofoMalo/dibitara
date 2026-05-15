package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.repository.BudgetRepository
import javax.inject.Inject

/**
 * Supprime le budget d'un mois donné de la base de données.
 * Après suppression, l'écran budget repasse à l'état "Aucun budget défini".
 */
class DeleteBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget) = repository.delete(budget)
}

package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Budget
import com.dibitara.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Règle : un UseCase = une seule responsabilité.
 * Ici : récupérer le budget d'un mois donné.
 * Le ViewModel appelle cet UseCase, pas le Repository directement.
 */
class GetMonthlyBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(month: Int, year: Int): Flow<Budget?> =
        budgetRepository.getBudget(month, year)
}

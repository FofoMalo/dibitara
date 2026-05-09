package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Budget
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Vérifie si le budget du mois courant est dépassé.
 *
 * Retourne le [Budget] si dépassé, null sinon (aucun budget configuré ou
 * budget respecté). L'appelant (AppViewModel) utilise ce résultat pour
 * décider d'envoyer ou non une notification.
 *
 * Le paramètre [today] permet de tester sans mocker LocalDate.now().
 */
class CheckBudgetNotificationUseCase @Inject constructor(
    private val getMonthlyBudget: GetMonthlyBudgetUseCase
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): Budget? {
        val budget = getMonthlyBudget(today.monthValue, today.year).first()
        return if (budget?.isOverBudget == true) budget else null
    }
}

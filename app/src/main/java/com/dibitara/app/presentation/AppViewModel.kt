package com.dibitara.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.usecase.CheckAvailableFundsUseCase
import com.dibitara.app.domain.usecase.CheckBudgetNotificationUseCase
import com.dibitara.app.domain.usecase.CheckDebtRemindersUseCase
import com.dibitara.app.domain.usecase.GenerateMonthlyRecurringUseCase
import com.dibitara.app.presentation.common.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel attaché à MainActivity.
 * Déclenche au démarrage :
 *  1. La génération des transactions récurrentes du mois.
 *  2. Les vérifications de notification (budget, dettes, fonds).
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val generateMonthlyRecurring: GenerateMonthlyRecurringUseCase,
    private val checkBudget: CheckBudgetNotificationUseCase,
    private val checkDebtReminders: CheckDebtRemindersUseCase,
    private val checkAvailableFunds: CheckAvailableFundsUseCase,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    companion object {
        // Seuil en-dessous duquel on avertit l'utilisateur (500€ par défaut)
        private const val SEUIL_FONDS_CENTS = 50_000L
    }

    init {
        viewModelScope.launch {
            generateMonthlyRecurring()
            verifierNotifications()
        }
    }

    private suspend fun verifierNotifications() {
        // 1. Budget dépassé ?
        val budgetDepasse = checkBudget()
        if (budgetDepasse != null) {
            notificationHelper.envoyerAlerteBudget(
                depenseCents = budgetDepasse.spentCents,
                alloueCents  = budgetDepasse.allocatedCents
            )
        }

        // 2. Dettes à rembourser aujourd'hui ?
        val dettesAujourdhui = checkDebtReminders()
        dettesAujourdhui.forEach { dette ->
            notificationHelper.envoyerRappelDette(
                idDette     = dette.id,
                labelDette  = dette.label,
                montantCents = dette.monthlyPaymentCents
            )
        }

        // 3. Solde du mois trop bas ?
        val soldeCents = checkAvailableFunds()
        if (soldeCents < SEUIL_FONDS_CENTS) {
            notificationHelper.envoyerAvertissementFonds(
                soldeCents = soldeCents,
                seuilCents = SEUIL_FONDS_CENTS
            )
        }
    }
}

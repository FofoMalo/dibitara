package com.dibitara.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.usecase.CheckAvailableFundsUseCase
import com.dibitara.app.domain.usecase.CheckBudgetNotificationUseCase
import com.dibitara.app.domain.usecase.CheckDebtRemindersUseCase
import com.dibitara.app.domain.usecase.GenerateMonthlyRecurringUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.presentation.common.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel attaché à MainActivity.
 * Déclenche au démarrage :
 *  1. La génération des transactions récurrentes du mois.
 *  2. Les vérifications de notification (budget, dettes, liquidités).
 * Le seuil d'alerte est lu depuis les préférences utilisateur — pas de valeur codée en dur.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val generateMonthlyRecurring: GenerateMonthlyRecurringUseCase,
    private val checkBudget: CheckBudgetNotificationUseCase,
    private val checkDebtReminders: CheckDebtRemindersUseCase,
    private val checkAvailableFunds: CheckAvailableFundsUseCase,
    private val getPreferences: GetUserPreferencesUseCase,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    init {
        viewModelScope.launch {
            generateMonthlyRecurring()
            verifierNotifications()
        }
    }

    /**
     * Appelé par MainActivity après que l'utilisateur accorde POST_NOTIFICATIONS.
     * Permet d'envoyer les notifications sans attendre le prochain démarrage.
     */
    fun relancerNotifications() {
        viewModelScope.launch { verifierNotifications() }
    }

    private suspend fun verifierNotifications() {
        // Snapshot unique des préférences — le seuil peut avoir été changé par l'utilisateur
        val prefs = getPreferences().first()

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
                idDette      = dette.id,
                labelDette   = dette.label,
                montantCents = dette.monthlyPaymentCents
            )
        }

        // 3. Liquidités insuffisantes ? (seuil depuis les préférences)
        val soldeCents = checkAvailableFunds()
        if (soldeCents < prefs.seuilFondsCents) {
            notificationHelper.envoyerAvertissementFonds(
                soldeCents = soldeCents,
                seuilCents = prefs.seuilFondsCents
            )
        }
    }
}

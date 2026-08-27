package com.dibitara.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dibitara.app.data.worker.PatrimoineSnapshotWorker
import com.dibitara.app.domain.usecase.CheckAvailableFundsUseCase
import com.dibitara.app.domain.usecase.CheckBudgetNotificationUseCase
import com.dibitara.app.domain.usecase.CheckDebtRemindersUseCase
import com.dibitara.app.domain.usecase.CheckEnveloppeDepassementUseCase
import com.dibitara.app.domain.usecase.CheckPendingContributionsUseCase
import com.dibitara.app.domain.usecase.GenerateRecurringUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.model.ThemeMode
import com.dibitara.app.domain.usecase.MigrerTabacVersCategorieUseCase
import com.dibitara.app.domain.repository.UserPreferencesRepository
import com.dibitara.app.presentation.common.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.content.Context

/**
 * ViewModel attaché à MainActivity.
 * Déclenche au démarrage :
 *  1. La migration ponctuelle de la sous-catégorie "Tabac" vers Category.TABAC (idempotente).
 *  2. La génération des transactions récurrentes du mois.
 *  3. Les vérifications de notification (budget, dettes, liquidités).
 *  4. La planification du snapshot mensuel du patrimoine (inconditionnelle, pas liée à une préférence).
 * Le seuil d'alerte est lu depuis les préférences utilisateur - pas de valeur codée en dur.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val generateRecurring          : GenerateRecurringUseCase,
    private val checkBudget                : CheckBudgetNotificationUseCase,
    private val checkDebtReminders         : CheckDebtRemindersUseCase,
    private val checkAvailableFunds        : CheckAvailableFundsUseCase,
    private val checkPendingContributions  : CheckPendingContributionsUseCase,
    private val checkEnveloppes            : CheckEnveloppeDepassementUseCase,
    private val migrerTabac                : MigrerTabacVersCategorieUseCase,
    private val getPreferences             : GetUserPreferencesUseCase,
    private val userPreferencesRepository  : UserPreferencesRepository,
    private val notificationHelper         : NotificationHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /**
     * Lu par MainActivity pour fournir [com.dibitara.app.presentation.common.LocalMontantsMasques]
     * à la racine de la composition - s'applique donc à tous les écrans sans les modifier.
     */
    val masquerMontants: StateFlow<Boolean> = getPreferences()
        .map { it.masquerMontants }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Lu par MainActivity pour choisir le colorScheme (clair/sombre/système) de [com.dibitara.app.presentation.common.theme.DibitaraTheme]. */
    val themeMode: StateFlow<ThemeMode> = getPreferences()
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEME)

    init {
        viewModelScope.launch {
            migrerTabac()
            generateRecurring()
            verifierNotifications()
        }
        planifierSnapshotPatrimoine()
    }

    /**
     * Garantit au moins un snapshot du patrimoine tous les 30 jours (voir [PatrimoineSnapshotWorker]),
     * indépendamment du fait que l'utilisateur ouvre ou non l'écran "Détail du patrimoine".
     * KEEP plutôt que UPDATE : ne redémarre pas le décompte à chaque lancement de l'app.
     */
    private fun planifierSnapshotPatrimoine() {
        val request = PeriodicWorkRequestBuilder<PatrimoineSnapshotWorker>(30, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PatrimoineSnapshotWorker.NOM_TRAVAIL_UNIQUE,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Appelé par MainActivity après que l'utilisateur accorde POST_NOTIFICATIONS.
     * Permet d'envoyer les notifications sans attendre le prochain démarrage.
     */
    fun relancerNotifications() {
        viewModelScope.launch { verifierNotifications() }
    }

    private suspend fun verifierNotifications() {
        // Snapshot unique des préférences - le seuil peut avoir été changé par l'utilisateur
        val prefs = getPreferences().first()
        val today = LocalDate.now()
        // Une seule alerte par jour et par type de notification : sans ce garde-fou,
        // verifierNotifications() (appelée à chaque ouverture de l'app) renotifiait à
        // l'identique tant que la condition restait vraie, même plusieurs fois par jour.
        val aujourdhui = today.toEpochDay()

        // 1. Budget dépassé ?
        val budgetDepasse = checkBudget()
        if (budgetDepasse != null && prefs.derniereAlerteBudgetEpochDay != aujourdhui) {
            notificationHelper.envoyerAlerteBudget(
                depenseCents = budgetDepasse.spentCents,
                alloueCents  = budgetDepasse.allocatedCents
            )
            userPreferencesRepository.updateDerniereAlerteBudget(aujourdhui)
        }

        // 2. Dettes à rembourser aujourd'hui ?
        //    Garde-fou sur le lot entier plutôt que par dette : CheckDebtRemindersUseCase ne
        //    retourne de toute façon une dette que le jour de son échéance, donc geler l'envoi
        //    du jour suffit à éviter les doublons sans suivre un état par dette.
        val dettesAujourdhui = checkDebtReminders()
        if (dettesAujourdhui.isNotEmpty() && prefs.derniereAlerteDettesEpochDay != aujourdhui) {
            dettesAujourdhui.forEach { dette ->
                notificationHelper.envoyerRappelDette(
                    idDette      = dette.id,
                    labelDette   = dette.label,
                    montantCents = dette.monthlyPaymentCents
                )
            }
            userPreferencesRepository.updateDerniereAlerteDettes(aujourdhui)
        }

        // 3. Liquidités insuffisantes ? (seuil depuis les préférences)
        val soldeCents = checkAvailableFunds()
        if (soldeCents < prefs.seuilFondsCents && prefs.derniereAlerteFondsEpochDay != aujourdhui) {
            notificationHelper.envoyerAvertissementFonds(
                soldeCents = soldeCents,
                seuilCents = prefs.seuilFondsCents
            )
            userPreferencesRepository.updateDerniereAlerteFonds(aujourdhui)
        }

        // 4. Contributions en attente en fin de mois ?
        //    Déclenchée uniquement dans les 5 derniers jours du mois
        val lastDayOfMonth = today.month.length(today.isLeapYear)
        if (today.dayOfMonth >= lastDayOfMonth - 4) {
            val pending = checkPendingContributions(today.monthValue, today.year)
            if (pending.count > 0) {
                notificationHelper.envoyerRappelContributions(pending.count, pending.totalCents)
            }
        }

        // 5. Enveloppes budgétaires dépassant 80 % ce mois ?
        val enveloppesEnAlerte = checkEnveloppes(seuilTaux = 0.8f)
        enveloppesEnAlerte.forEach { statut ->
            notificationHelper.envoyerAlerteEnveloppe(
                category     = statut.envelope.category,
                depenseCents = statut.depenseCents,
                plafondCents = statut.envelope.plafondCents
            )
        }
    }
}

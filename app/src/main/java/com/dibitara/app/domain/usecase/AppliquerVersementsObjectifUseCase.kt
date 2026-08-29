package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.SavingsGoal
import com.dibitara.app.domain.repository.SavingsGoalRepository
import com.dibitara.app.domain.repository.VersementRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Enregistre un ou plusieurs versements mensuels sur un objectif d'épargne, avec
 * **rattrapage des mois manqués**.
 *
 * « Rattraper N mensualités » = s'assurer que les N derniers mois (mois courant
 * inclus) ont chacun un versement enregistré. On parcourt la fenêtre
 * `[mois courant − N + 1 … mois courant]` et on écrit une ligne
 * `monthly_versements` (type [CompteType.OBJECTIF]) pour chaque mois de cette
 * fenêtre qui n'en a pas encore. Chaque versement vaut la mensualité **actuelle**
 * de l'objectif (si elle a changé entre-temps, les rattrapages sont enregistrés au
 * taux courant - acceptable, on ne reconstitue pas l'historique du plan).
 *
 * `currentAmountCents` de l'objectif est ensuite avancé du total réellement
 * enregistré (0 si tous les mois de la fenêtre étaient déjà couverts).
 *
 * Atomicité : comme [com.dibitara.app.presentation.savings.SavingsViewModel]
 * `appliquerVersement` pour les comptes, ce n'est pas une transaction unique. En
 * cas d'échec après quelques insertions, l'objectif garde une progression
 * cohérente avec ce qui a été écrit (on n'avance le solde qu'à la fin, du total
 * effectivement inséré). Appli mono-utilisateur, échec rare et rattrapable via
 * « Modifier ».
 */
class AppliquerVersementsObjectifUseCase @Inject constructor(
    private val versementRepository: VersementRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) {
    suspend operator fun invoke(
        goal: SavingsGoal,
        nbMensualites: Int,
        aujourdhui: LocalDate = LocalDate.now()
    ): Result<VersementsObjectifResult> = runCatching {
        require(nbMensualites >= 1) { "Le nombre de mensualités doit être au moins 1" }
        val mensualite = goal.monthlyContributionCents
        require(mensualite > 0) { "L'objectif n'a pas de versement mensuel défini" }

        val moisCourant = YearMonth.from(aujourdhui)
        var enregistrees = 0
        for (i in 0 until nbMensualites) {
            val mois = moisCourant.minusMonths(i.toLong())
            val existe = versementRepository.existsPourMois(
                goal.id, CompteType.OBJECTIF, mois.year, mois.monthValue
            )
            if (!existe) {
                versementRepository.save(
                    MonthlyVersement(
                        accountId    = goal.id,
                        compteType   = CompteType.OBJECTIF,
                        year         = mois.year,
                        month        = mois.monthValue,
                        montantCents = mensualite,
                        currency     = goal.currency
                    )
                ).getOrThrow()
                enregistrees++
            }
        }

        val credite = enregistrees * mensualite
        if (enregistrees > 0) {
            savingsGoalRepository.upsert(
                goal.copy(currentAmountCents = goal.currentAmountCents + credite)
            )
        }
        VersementsObjectifResult(mensualitesEnregistrees = enregistrees, centimesCredites = credite)
    }
}

/**
 * @param mensualitesEnregistrees nombre de mois pour lesquels un versement a été créé
 *   (peut être < demandé si des mois de la fenêtre étaient déjà couverts, 0 si à jour).
 * @param centimesCredites total ajouté à `currentAmountCents`.
 */
data class VersementsObjectifResult(
    val mensualitesEnregistrees: Int,
    val centimesCredites: Long
)

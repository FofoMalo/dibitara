package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.SavingsGoal
import javax.inject.Inject

/**
 * Objectifs d'épargne dont le versement mensuel est prévu mais **pas encore
 * enregistré pour le mois courant** - sert au rappel visuel in-app de l'`ObjectifCard`.
 *
 * Pendant de [GetVersementsEnAttenteUseCase] (comptes épargne). UseCase PUR : le
 * ViewModel lui passe les objectifs et la liste des versements `OBJECTIF` déjà
 * enregistrés ce mois-ci.
 *
 * NB : « en attente » ne concerne que le mois courant. Le rattrapage des mois
 * antérieurs manqués est un choix explicite de l'utilisateur dans la feuille de
 * versement (on n'a pas de date de début de plan pour les détecter tout seuls).
 */
class GetObjectifsVersementEnAttenteUseCase @Inject constructor() {

    /**
     * @param objectifs tous les objectifs d'épargne.
     * @param versementsObjectifDuMois les versements `OBJECTIF` déjà enregistrés
     *   pour le mois courant (déjà filtrés par le ViewModel via `getAllPourMois`).
     * @return les identifiants des objectifs **non atteints**, avec un versement
     *   mensuel prévu (`monthlyContributionCents > 0`) et non encore versé ce mois-ci.
     */
    operator fun invoke(
        objectifs: List<SavingsGoal>,
        versementsObjectifDuMois: List<MonthlyVersement>
    ): List<Long> {
        val dejaVerses = versementsObjectifDuMois.map { it.accountId }.toSet()
        return objectifs
            .filter { !it.estAtteint && it.monthlyContributionCents > 0 && it.id !in dejaVerses }
            .map { it.id }
    }
}

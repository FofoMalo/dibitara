package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.SavingsAccount
import javax.inject.Inject

/**
 * Comptes épargne dont le versement mensuel est prévu mais **pas encore enregistré
 * pour le mois courant** - sert au rappel visuel in-app de l'écran Épargne.
 *
 * UseCase PUR (`@Inject constructor()` sans dépendance), même forme que
 * [ProjeterObjectifUseCase] : le ViewModel lui passe les comptes et la liste des
 * versements EPARGNE déjà enregistrés ce mois-ci (qu'il a déjà en main pour calculer
 * `totalVerseMoisCents`).
 *
 * Aucune date « dernière alerte » n'est persistée : contrairement à une notification
 * push (cf. bug des alertes répétées), un rappel visuel peut se réafficher à chaque
 * ouverture de l'écran sans être intrusif, et il disparaît de lui-même dès que le
 * versement est enregistré.
 */
class GetVersementsEnAttenteUseCase @Inject constructor() {

    /**
     * @param comptes tous les comptes épargne.
     * @param versementsEpargneDuMois les versements de type EPARGNE déjà enregistrés
     *   pour le mois courant (déjà filtrés par le ViewModel via `getAllPourMois`).
     * @return les identifiants des comptes avec un versement mensuel prévu
     *   (`monthlyContributionCents > 0`) et non encore versé ce mois-ci.
     */
    operator fun invoke(
        comptes: List<SavingsAccount>,
        versementsEpargneDuMois: List<MonthlyVersement>
    ): List<Long> {
        val dejaVerses = versementsEpargneDuMois.map { it.accountId }.toSet()
        return comptes
            .filter { it.monthlyContributionCents > 0 && it.id !in dejaVerses }
            .map { it.id }
    }
}

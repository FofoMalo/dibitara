package com.dibitara.app.domain.usecase

import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Résultat retourné quand le revenu du mois le plus récent analysé est anormalement bas
 * par rapport à la moyenne des 3 mois - symptôme possible d'un revenu sous-capté (import
 * manquant) plutôt qu'une vraie baisse (CADRAGE_INDEPENDANCE_FINANCIERE.md §1/§5, cas réel :
 * 2213,19€ un mois donné contre 13544,04€ de moyenne 3 mois).
 */
data class RevenuIncompletAlerte(
    val mois             : Int,
    val annee            : Int,
    val revenuMoisCents  : Long,
    val revenuMoyenCents : Long
)

/**
 * Détecte un mois de référence dont le revenu est anormalement bas par rapport à la moyenne
 * des 3 mois analysés par [GetSpendingRecommendationsUseCase], plutôt que d'afficher
 * silencieusement un taux d'épargne dégradé (F5 du cadrage indépendance financière).
 *
 * Retourne l'alerte si le revenu du mois le plus récent est sous [SEUIL_PCT] % de la moyenne,
 * null sinon (y compris si la moyenne est nulle - rien à comparer).
 */
class CheckRevenuIncompletUseCase @Inject constructor(
    private val getSpendingRecommendations: GetSpendingRecommendationsUseCase
) {
    companion object {
        const val SEUIL_PCT = 30
    }

    suspend operator fun invoke(today: LocalDate = LocalDate.now()): RevenuIncompletAlerte? {
        val recommandation = getSpendingRecommendations(today.monthValue, today.year).first()
        val revenuMoyen = recommandation.revenuMoyenCents
        if (revenuMoyen <= 0) return null

        val revenuDernierMois = recommandation.revenuParMoisCents.firstOrNull() ?: return null
        if (revenuDernierMois >= revenuMoyen * SEUIL_PCT / 100) return null

        val (mois, annee) = recommandation.moisDeReference.first()
        return RevenuIncompletAlerte(
            mois             = mois,
            annee            = annee,
            revenuMoisCents  = revenuDernierMois,
            revenuMoyenCents = revenuMoyen
        )
    }
}

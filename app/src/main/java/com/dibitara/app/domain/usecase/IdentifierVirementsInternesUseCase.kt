package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val FENETRE_JOURS = 3L
private const val PREFIXE_IMPORT_BRED = "bred"
private const val IMPORT_TRADE_REPUBLIC = "trade_republic"

/**
 * Identifie, parmi une liste de transactions, les virements internes entre le compte BRED
 * et le compte TradeRepublic de Florent : un virement sortant côté BRED et un virement entrant
 * côté TradeRepublic de même montant, même devise, à quelques jours d'intervalle.
 *
 * Ce ne sont pas de nouvelles entrées/sorties d'argent réelles, juste un déplacement entre deux
 * comptes déjà suivis par l'app - les compter comme dépense ET revenu gonflerait artificiellement
 * les totaux (revenu moyen, taux d'épargne, projection de trésorerie...).
 *
 * Appariement glouton : chaque transaction n'est utilisée que dans au plus une paire.
 * Retourne les ids des transactions appariées, à exclure des calculs de revenus/dépenses -
 * elles restent visibles telles quelles dans la liste des transactions et les catégories.
 */
class IdentifierVirementsInternesUseCase @Inject constructor() {

    operator fun invoke(transactions: List<Transaction>): Set<Long> {
        val sortants = transactions.filter {
            it.type == TransactionType.EXPENSE &&
                it.category == Category.TRANSFERTS &&
                it.importSource?.startsWith(PREFIXE_IMPORT_BRED) == true
        }
        val entrantsDisponibles = transactions.filter {
            it.type == TransactionType.INCOME &&
                it.category == Category.TRANSFERTS &&
                it.importSource == IMPORT_TRADE_REPUBLIC
        }.toMutableList()

        val idsAppaires = mutableSetOf<Long>()
        for (sortant in sortants) {
            val correspondance = entrantsDisponibles.firstOrNull { entrant ->
                entrant.amountCents == sortant.amountCents &&
                    entrant.currency == sortant.currency &&
                    ChronoUnit.DAYS.between(sortant.date, entrant.date) in -FENETRE_JOURS..FENETRE_JOURS
            } ?: continue

            idsAppaires += sortant.id
            idsAppaires += correspondance.id
            entrantsDisponibles -= correspondance
        }
        return idsAppaires
    }
}

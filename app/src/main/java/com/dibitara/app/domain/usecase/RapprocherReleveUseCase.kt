package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import java.time.YearMonth
import javax.inject.Inject

data class RapprochementResult(
    val soldeCalculeCents: Long,
    val ecartCents: Long,
    val nombreOperations: Int,
    val devisesNonComparees: Int,
    val investissementsAVerifier: Int
)

/** Comparaison d'un seul compte : ses virements internes sont de vrais mouvements de solde. */
class RapprocherReleveUseCase @Inject constructor() {
    operator fun invoke(compte: BankAccount, mois: YearMonth, ouverture: Long, cloture: Long,
        transactions: List<Transaction>): RapprochementResult {
        val operations = transactions.filter { it.bankAccountId == compte.id && YearMonth.from(it.date) == mois }
        val comparees = operations.filter { it.currency == compte.currency }
        val solde = comparees.fold(ouverture) { total, t ->
            if (t.type == TransactionType.INCOME) Math.addExact(total, t.amountCents)
            else Math.subtractExact(total, t.amountCents)
        }
        return RapprochementResult(solde, Math.subtractExact(cloture, solde), comparees.size,
            operations.size - comparees.size, comparees.count { it.type == TransactionType.INVESTMENT })
    }
}

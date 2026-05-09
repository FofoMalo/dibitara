package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Calcule le solde estimé du mois courant.
 *
 * Formule : Σ revenus (INCOME) − Σ dépenses (EXPENSE) pour le mois en cours.
 * Les investissements ne sont pas comptés dans ce solde courant.
 *
 * Retourne le solde en centimes. L'appelant compare ce solde à son seuil
 * d'alerte pour décider d'envoyer ou non une notification.
 */
class CheckAvailableFundsUseCase @Inject constructor(
    private val getMonthlyTransactions: GetMonthlyTransactionsUseCase
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): Long {
        val transactions = getMonthlyTransactions(today.monthValue, today.year).first()

        val revenus  = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amountCents }

        val depenses = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amountCents }

        return revenus - depenses
    }
}

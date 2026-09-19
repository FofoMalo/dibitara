package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import javax.inject.Inject

data class BilanMensuel(val transactions: List<Transaction>, val revenusCents: Long, val depensesCents: Long)

/** Une même base de calcul pour le résumé et les détails : exclusion des transferts puis conversion. */
class CalculerBilanMensuelUseCase @Inject constructor() {
    operator fun invoke(transactions: List<Transaction>, devise: Currency, taux: ExchangeRates): BilanMensuel {
        val internes = IdentifierVirementsInternesUseCase()(transactions)
        val normalisees = transactions.filterNot { it.id in internes }.map {
            it.copy(amountCents = CurrencyConverter.convertCents(it.amountCents, it.currency, devise, taux), currency = devise)
        }
        return BilanMensuel(normalisees,
            normalisees.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents },
            normalisees.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents })
    }
}

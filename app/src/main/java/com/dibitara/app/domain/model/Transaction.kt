package com.dibitara.app.domain.model

import java.time.LocalDate

/**
 * Entité métier centrale : représente une transaction financière.
 *
 * IMPORTANT : [amountCents] est toujours en centimes de la devise [currency].
 * Ne jamais stocker de Double pour les montants financiers (risque d'arrondi).
 * Exemple : 12,50 € → amountCents = 1250, currency = Currency.EUR
 */
data class Transaction(
    val id: Long = 0,
    val amountCents: Long,
    val currency: Currency,
    val category: Category,
    val type: TransactionType,
    val date: LocalDate,
    val note: String = ""
)

enum class TransactionType { EXPENSE, INCOME, INVESTMENT }

enum class Currency(val symbol: String, val isoCode: String) {
    EUR("€", "EUR"),
    USD("$", "USD"),
    XOF("FCFA", "XOF"),
    XAF("FCFA", "XAF")
}

enum class Category {
    ALIMENTATION, LOGEMENT, TRANSPORT, SANTE,
    LOISIRS, INVESTISSEMENT, EPARGNE, AUTRE
}

package com.dibitara.app.domain.model

import java.time.LocalDate

/**
 * Représente un compte bancaire suivi (BRED, TradeRepublic...), auquel les transactions
 * importées sont rattachées via [Transaction.bankAccountId].
 *
 * [currentBalanceCents] est saisi/mis à jour manuellement par l'utilisateur (comme
 * [SavingsAccount.currentBalanceCents]), pas dérivé de l'historique des transactions -
 * l'app ne fait pas de comptabilité en partie double.
 */
data class BankAccount(
    val id: Long = 0,
    val provider: BankProvider,
    val label: String,
    val currentBalanceCents: Long,
    val currency: Currency,
    val updatedAt: LocalDate
)

/** Comptes + somme de leurs soldes convertie dans [currency] (devise par défaut de l'utilisateur). */
data class BankAccountsSummary(
    val comptes: List<BankAccount>,
    val totalCents: Long,
    val currency: Currency
)

enum class BankProvider(val displayName: String) {
    BRED           ("BRED"),
    TRADE_REPUBLIC ("TradeRepublic"),
    QONTO          ("Qonto"),
    ESPECES        ("Espèces"),
    AUTRE          ("Autre");        // toujours en dernier - fallback de safeValueOf

    companion object
}

/**
 * Déduit le [BankProvider] d'un compte à partir de la chaîne [Transaction.importSource]
 * posée par les parseurs d'import (voir BredCsvParser, BredPdfParser, BredNotificationParser,
 * TradeRepublicCsvParser). Retourne null si la source n'est pas reconnue ou absente
 * (ex. saisie manuelle) - dans ce cas, aucun compte n'est rattaché automatiquement.
 */
fun BankProvider.Companion.fromImportSource(importSource: String?): BankProvider? = when {
    importSource == null -> null
    importSource.startsWith("bred") -> BankProvider.BRED
    importSource == "trade_republic" -> BankProvider.TRADE_REPUBLIC
    else -> null
}

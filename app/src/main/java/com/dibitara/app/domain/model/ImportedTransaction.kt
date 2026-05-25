package com.dibitara.app.domain.model

import java.time.LocalDate

/**
 * Transaction parsée depuis un export CSV externe, avant confirmation d'import.
 * Une fois validée par l'utilisateur, elle est convertie en [Transaction] et persistée en base.
 *
 * [alreadyImported] est renseigné par l'use case après vérification de l'[externalId] en base —
 * les doublons sont signalés dans le preview mais jamais insérés deux fois.
 *
 * [importSource] identifie la banque d'origine ("trade_republic", "bred"…) — stocké sur la
 * [Transaction] pour permettre un filtre ou une traçabilité ultérieure.
 */
data class ImportedTransaction(
    val date: LocalDate,
    val amountCents: Long,                      // toujours positif, [type] donne la direction
    val currency: Currency,
    val category: Category,                     // catégorie suggérée automatiquement (modifiable dans le preview)
    val type: TransactionType,
    val note: String,
    val externalId: String,                     // clé de déduplication — UUID (TR) ou hash calculé (BRED)
    val rawType: String,                        // type brut de l'opération selon la banque source
    val importSource: String = "trade_republic",
    val alreadyImported: Boolean = false
) {
    /** Convertit vers [Transaction] prêt à être sauvegardé en base. */
    fun toTransaction() = Transaction(
        amountCents  = amountCents,
        currency     = currency,
        category     = category,
        type         = type,
        date         = date,
        note         = note,
        importSource = importSource,
        externalId   = externalId
    )
}

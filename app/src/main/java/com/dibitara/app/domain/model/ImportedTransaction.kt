package com.dibitara.app.domain.model

import java.time.LocalDate

/**
 * Transaction parsée depuis un export CSV externe (TradeRepublic), avant confirmation d'import.
 * Une fois validée par l'utilisateur, elle est convertie en [Transaction] et persistée en base.
 *
 * [alreadyImported] est renseigné par l'use case après vérification de l'[externalId] en base —
 * les doublons sont signalés dans le preview mais jamais insérés deux fois.
 */
data class ImportedTransaction(
    val date: LocalDate,
    val amountCents: Long,                 // toujours positif, [type] donne la direction
    val currency: Currency,
    val category: Category,                // catégorie suggérée automatiquement (modifiable dans le preview)
    val type: TransactionType,
    val note: String,
    val externalId: String,                // UUID TradeRepublic — clé de déduplication
    val trRawType: String,                 // type TR brut ("CARD_TRANSACTION", "BUY"…) pour l'affichage
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
        importSource = "trade_republic",
        externalId   = externalId
    )
}

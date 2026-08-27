package com.dibitara.app.domain.model

import java.time.LocalDate

/**
 * Transaction extraite d'un fichier CSV importé, **avant** confirmation.
 * Une fois validée par l'utilisateur dans l'écran d'aperçu, elle est convertie
 * en [Transaction] et persistée.
 *
 * [amountCents] est toujours positif : [type] porte le sens (dépense / revenu).
 *
 * [alreadyImported] est positionné par `ImporterTransactionsCsvUseCase` après
 * comparaison de l'[externalId] avec ceux déjà en base — les doublons sont
 * signalés dans l'aperçu mais jamais réinsérés.
 *
 * [inclure] permet à l'utilisateur de décocher une ligne dans l'aperçu.
 *
 * [ligneIndex] est la position de la ligne dans le fichier source. C'est la clé
 * d'identité côté UI (liste et édition de l'aperçu) : deux transactions vraiment
 * identiques le même jour partagent le même [externalId] (dédup voulue à
 * l'insertion) mais restent deux lignes distinctes à l'écran.
 */
data class ImportedTransaction(
    val date: LocalDate,
    val amountCents: Long,
    val currency: Currency,
    val type: TransactionType,
    val note: String,
    val category: Category,
    val externalId: String,
    val ligneIndex: Int,
    val alreadyImported: Boolean = false,
    val inclure: Boolean = true,
) {
    /**
     * Convertit vers une [Transaction] prête à être sauvegardée.
     * [bankAccountId] est résolu par l'appelant (choix de l'utilisateur dans
     * l'aperçu) — un parseur pur n'a pas accès à la base des comptes.
     */
    fun toTransaction(bankAccountId: Long? = null) = Transaction(
        amountCents = amountCents,
        currency = currency,
        category = category,
        type = type,
        date = date,
        note = note,
        importSource = SOURCE_CSV,
        externalId = externalId,
        bankAccountId = bankAccountId,
    )

    companion object {
        /** Valeur de [Transaction.importSource] pour toute transaction issue d'un import CSV. */
        const val SOURCE_CSV = "csv"
    }
}

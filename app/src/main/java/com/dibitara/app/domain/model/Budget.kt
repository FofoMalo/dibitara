package com.dibitara.app.domain.model

/**
 * Budget mensuel défini par l'utilisateur pour une période donnée.
 * [allocatedCents] = montant alloué, [spentCents] = montant déjà dépensé.
 * La couche domain calcule le solde : remainingCents = allocatedCents - spentCents.
 */
data class Budget(
    val id: Long = 0,
    val month: Int,
    val year: Int,
    val allocatedCents: Long,
    val spentCents: Long,
    val currency: Currency
) {
    val remainingCents: Long get() = allocatedCents - spentCents
    val isOverBudget: Boolean get() = spentCents > allocatedCents
}

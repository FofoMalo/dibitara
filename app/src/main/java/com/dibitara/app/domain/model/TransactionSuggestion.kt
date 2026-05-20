package com.dibitara.app.domain.model

/**
 * Représente une suggestion de saisie rapide.
 *
 * Construite à partir des transactions récentes : si le même groupe
 * (libellé + montant + catégorie + devise + type) apparaît au moins 2 fois
 * dans les 30 derniers jours, il devient une suggestion.
 *
 * [frequence] permet de trier les suggestions les plus courantes en premier.
 */
data class TransactionSuggestion(
    val label: String,
    val amountCents: Long,
    val currency: Currency,
    val category: Category,
    val type: TransactionType,
    val subCategory: SubCategory? = null,
    val customSubCategoryId: Long? = null,
    val frequence: Int
)

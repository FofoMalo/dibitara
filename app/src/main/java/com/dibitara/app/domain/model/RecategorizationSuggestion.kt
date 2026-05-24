package com.dibitara.app.domain.model

/**
 * Suggestion de recatégorisation pour une transaction actuellement classée dans [Category.AUTRE].
 *
 * [transaction]       — la transaction concernée
 * [suggestedCategory] — catégorie proposée par correspondance de mot-clé
 * [matchedKeyword]    — le mot-clé du libellé qui a déclenché la suggestion (utile pour l'UI)
 */
data class RecategorizationSuggestion(
    val transaction: Transaction,
    val suggestedCategory: Category,
    val matchedKeyword: String
)

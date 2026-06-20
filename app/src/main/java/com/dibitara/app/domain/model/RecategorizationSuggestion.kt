package com.dibitara.app.domain.model

/**
 * Suggestion de recatégorisation pour une transaction actuellement classée dans [Category.AUTRE].
 *
 * [transaction]          - la transaction concernée
 * [suggestedCategory]    - catégorie proposée (Category.AUTRE si seule la sous-catégorie change)
 * [matchedKeyword]       - le mot-clé du libellé qui a déclenché la suggestion (utile pour l'UI)
 * [suggestedSubCategory] - non-null quand le moteur recommande une sous-catégorie d'AUTRE
 *                          plutôt que de changer de catégorie principale
 */
data class RecategorizationSuggestion(
    val transaction: Transaction,
    val suggestedCategory: Category,
    val matchedKeyword: String,
    val suggestedSubCategory: SubCategory? = null
)

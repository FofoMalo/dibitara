package com.dibitara.app.domain.model

/**
 * Règle de catégorisation apprise depuis les choix manuels de l'utilisateur.
 *
 * Quand l'utilisateur catégorise une transaction (libellé → catégorie),
 * une règle est sauvegardée. À l'import suivant, si un libellé correspond
 * exactement (casse ignorée, espaces tronqués), la catégorie est appliquée
 * automatiquement sans intervention.
 *
 * [noteExact] est toujours stocké en minuscules + trimé pour simplifier les comparaisons.
 */
data class CategorizationRule(
    val id: Long = 0,
    val noteExact: String,
    val category: Category,
    val subCategory: SubCategory? = null,
    val customSubCategoryId: Long? = null
)

package com.dibitara.app.domain.model

/**
 * Sous-catégorie personnalisée créée par l'utilisateur.
 *
 * Contrairement à [SubCategory] (enum fixe pour AUTRE),
 * une CustomSubCategory est liée à n'importe quelle [Category]
 * et peut être créée, renommée ou supprimée librement.
 *
 * [parentCategory] détermine dans quel groupe la sous-catégorie apparaît
 * dans le formulaire de transaction.
 */
data class CustomSubCategory(
    val id: Long = 0,
    val name: String,
    val parentCategory: Category
)

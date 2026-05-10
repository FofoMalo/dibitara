package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CustomSubCategory
import kotlinx.coroutines.flow.Flow

interface CustomSubCategoryRepository {
    /** Émet la liste de toutes les sous-catégories personnalisées, triées par catégorie puis nom. */
    fun getAll(): Flow<List<CustomSubCategory>>

    /** Émet uniquement les sous-catégories pour une [Category] donnée. */
    fun getByCategory(category: Category): Flow<List<CustomSubCategory>>

    /** Insère ou remplace (UPSERT) — l'id=0 crée, id>0 met à jour. */
    suspend fun upsert(subCategory: CustomSubCategory)

    /** Supprime la sous-catégorie. Les transactions liées conservent leur [customSubCategoryId]
     *  — le ViewModel affichera "Sous-catégorie supprimée" pour ces transactions. */
    suspend fun delete(subCategory: CustomSubCategory)
}

package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.CustomSubCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomSubCategoryDao {

    /** Toutes les sous-catégories, triées par parentCategory puis name. */
    @Query("SELECT * FROM custom_sub_categories ORDER BY parentCategory ASC, name ASC")
    fun getAll(): Flow<List<CustomSubCategoryEntity>>

    /** Sous-catégories d'une catégorie donnée, triées par nom. */
    @Query("SELECT * FROM custom_sub_categories WHERE parentCategory = :category ORDER BY name ASC")
    fun getByCategory(category: String): Flow<List<CustomSubCategoryEntity>>

    /** Insère ou remplace si l'id est déjà présent. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CustomSubCategoryEntity)

    @Delete
    suspend fun delete(entity: CustomSubCategoryEntity)
}

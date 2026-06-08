package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.CategoryEnvelopeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryEnvelopeDao {

    @Query("SELECT * FROM category_envelopes ORDER BY category ASC")
    fun getAll(): Flow<List<CategoryEnvelopeEntity>>

    /** INSERT ou REPLACE si la catégorie existe déjà (contrainte unique sur category). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CategoryEnvelopeEntity)

    @Delete
    suspend fun delete(entity: CategoryEnvelopeEntity)
}

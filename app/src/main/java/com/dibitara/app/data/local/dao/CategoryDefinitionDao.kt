package com.dibitara.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.dibitara.app.data.local.entity.CategoryDefinitionEntity

@Dao interface CategoryDefinitionDao {
    @Query("SELECT * FROM category_definitions") fun observe(): Flow<List<CategoryDefinitionEntity>>
    @Query("SELECT * FROM category_definitions") suspend fun all(): List<CategoryDefinitionEntity>
    @Upsert suspend fun save(value: CategoryDefinitionEntity)
    @Query("DELETE FROM category_definitions WHERE `key` = :key") suspend fun delete(key: String)
}

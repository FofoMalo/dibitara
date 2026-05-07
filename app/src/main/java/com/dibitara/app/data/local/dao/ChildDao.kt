package com.dibitara.app.data.local.dao

import androidx.room.*
import com.dibitara.app.data.local.entity.ChildEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Query("SELECT * FROM children ORDER BY name ASC")
    fun getAll(): Flow<List<ChildEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(child: ChildEntity): Long

    @Update
    suspend fun update(child: ChildEntity)

    @Delete
    suspend fun delete(child: ChildEntity)
}

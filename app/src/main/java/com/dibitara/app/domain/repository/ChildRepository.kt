package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.Child
import kotlinx.coroutines.flow.Flow

interface ChildRepository {
    fun getAll(): Flow<List<Child>>
    suspend fun save(child: Child): Result<Long>
    suspend fun delete(child: Child)
}

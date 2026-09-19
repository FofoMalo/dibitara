package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface CategoryCatalogRepository {
    fun observe(): Flow<CategoryCatalog>
    suspend fun save(node: CategoryNode)
    suspend fun create(name: String, parentKey: String?): String
    suspend fun impact(key: String): CategoryImpact
    suspend fun deleteUnused(key: String)
    suspend fun merge(source: String, target: String, expected: CategoryImpact)
    suspend fun move(source: String, parent: String, expected: CategoryImpact)
    suspend fun classify(ids: Set<Long>, choice: CategoryChoice): Int
    suspend fun rememberRule(note: String, choice: CategoryChoice)
    suspend fun matching(note: String, choice: CategoryChoice): List<Transaction>
}

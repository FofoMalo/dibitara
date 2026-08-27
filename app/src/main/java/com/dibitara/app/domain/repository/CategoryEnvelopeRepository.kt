package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.CategoryEnvelope
import kotlinx.coroutines.flow.Flow

interface CategoryEnvelopeRepository {
    fun getAll(): Flow<List<CategoryEnvelope>>
    suspend fun upsert(envelope: CategoryEnvelope)
    suspend fun delete(envelope: CategoryEnvelope)
}

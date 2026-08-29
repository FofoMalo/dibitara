package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.CategoryEnvelopeDao
import com.dibitara.app.data.local.entity.CategoryEnvelopeEntity
import com.dibitara.app.domain.model.CategoryEnvelope
import com.dibitara.app.domain.repository.CategoryEnvelopeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryEnvelopeRepositoryImpl @Inject constructor(
    private val dao: CategoryEnvelopeDao
) : CategoryEnvelopeRepository {

    override fun getAll(): Flow<List<CategoryEnvelope>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsert(envelope: CategoryEnvelope) =
        dao.upsert(envelope.toEntity())

    override suspend fun delete(envelope: CategoryEnvelope) =
        dao.delete(envelope.toEntity())

    // ─── Mapping entité ↔ domaine ─────────────────────────────────────────────
    // La conversion vit sur CategoryEnvelopeEntity (réutilisée par la restauration JSON).

    private fun CategoryEnvelope.toEntity() = CategoryEnvelopeEntity.fromDomain(this)
}

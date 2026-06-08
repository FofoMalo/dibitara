package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CategoryEnvelope
import com.dibitara.app.domain.repository.CategoryEnvelopeRepository
import javax.inject.Inject

/** Crée ou met à jour une enveloppe budgétaire pour une catégorie. */
class UpsertCategoryEnvelopeUseCase @Inject constructor(
    private val repository: CategoryEnvelopeRepository
) {
    suspend operator fun invoke(envelope: CategoryEnvelope) = repository.upsert(envelope)
}

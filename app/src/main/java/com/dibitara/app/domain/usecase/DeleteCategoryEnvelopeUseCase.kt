package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CategoryEnvelope
import com.dibitara.app.domain.repository.CategoryEnvelopeRepository
import javax.inject.Inject

/** Supprime une enveloppe budgétaire. */
class DeleteCategoryEnvelopeUseCase @Inject constructor(
    private val repository: CategoryEnvelopeRepository
) {
    suspend operator fun invoke(envelope: CategoryEnvelope) = repository.delete(envelope)
}

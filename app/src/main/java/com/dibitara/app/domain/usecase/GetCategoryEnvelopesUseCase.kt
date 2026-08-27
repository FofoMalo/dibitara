package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CategoryEnvelope
import com.dibitara.app.domain.repository.CategoryEnvelopeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Retourne toutes les enveloppes définies par l'utilisateur, en temps réel. */
class GetCategoryEnvelopesUseCase @Inject constructor(
    private val repository: CategoryEnvelopeRepository
) {
    operator fun invoke(): Flow<List<CategoryEnvelope>> = repository.getAll()
}

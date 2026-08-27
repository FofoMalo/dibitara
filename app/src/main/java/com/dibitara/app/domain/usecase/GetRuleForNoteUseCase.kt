package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CategorizationRule
import com.dibitara.app.domain.repository.CategorizationRuleRepository
import javax.inject.Inject

/**
 * Cherche une règle apprise pour un libellé donné (exact match, insensible à la casse).
 * Retourne null si aucune règle n'existe pour ce libellé.
 */
class GetRuleForNoteUseCase @Inject constructor(
    private val repository: CategorizationRuleRepository
) {
    suspend operator fun invoke(note: String): CategorizationRule? =
        repository.getRuleForNote(note)
}

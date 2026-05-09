package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Met à jour le seuil d'alerte "liquidités insuffisantes" (valeur en centimes).
class UpdateSeuilFondsUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(seuilCents: Long) = repository.updateSeuil(seuilCents)
}

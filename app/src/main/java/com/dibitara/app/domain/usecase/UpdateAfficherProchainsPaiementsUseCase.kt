package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Active ou désactive la carte "Prochains paiements" dans le tableau de bord.
class UpdateAfficherProchainsPaiementsUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(afficher: Boolean) = repository.updateAfficherProchainsPaiements(afficher)
}

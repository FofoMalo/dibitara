package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Active ou masque l'onglet "Placements" dans la barre de navigation.
class UpdateAfficherInvestissementsUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(afficher: Boolean) = repository.updateAfficherInvestissements(afficher)
}

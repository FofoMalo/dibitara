package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Active ou désactive le bouton "Recommandations" dans l'écran Budget.
class UpdateAfficherRecommandationsUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(afficher: Boolean) = repository.updateAfficherRecommandations(afficher)
}

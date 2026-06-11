package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Met à jour l'objectif d'épargne cible en pourcentage du revenu mensuel.
class UpdateTauxEpargneCibleUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(pct: Int) = repository.updateTauxEpargneCible(pct)
}

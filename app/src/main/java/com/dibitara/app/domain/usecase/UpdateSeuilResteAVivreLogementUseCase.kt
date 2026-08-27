package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Met à jour le seuil de reste à vivre mensuel minimum du Scénario logement (valeur en centimes).
class UpdateSeuilResteAVivreLogementUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(seuilCents: Long) = repository.updateSeuilResteAVivreLogement(seuilCents)
}

package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Met à jour le multiple de dépense annuelle lissée utilisé pour le capital cible FI.
class UpdateMultipleFICibleUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(multiple: Int) = repository.updateMultipleFICible(multiple)
}

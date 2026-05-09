package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Met à jour la devise par défaut utilisée à l'affichage et à la saisie.
class UpdateDeviseParDefautUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(currency: Currency) = repository.updateDevise(currency)
}

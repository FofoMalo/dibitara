package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Met à jour l'hypothèse de rendement annuel (%) utilisée pour la projection FI.
class UpdateRendementFIEspereUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(pct: Int) = repository.updateRendementFIEspere(pct)
}

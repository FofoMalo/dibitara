package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Active ou désactive le masquage global des montants à l'écran (confidentialité).
class UpdateMasquerMontantsUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(masquer: Boolean) = repository.updateMasquerMontants(masquer)
}

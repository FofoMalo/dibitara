package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Active ou désactive l'obligation de saisir un code TOTP après le PIN ou le mot de passe.
class UpdateTwoFactorEnabledUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean) = repository.updateTwoFactorEnabled(enabled)
}

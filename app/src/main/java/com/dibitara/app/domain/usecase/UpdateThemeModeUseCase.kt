package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.ThemeMode
import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Met à jour l'apparence choisie (système/clair/sombre).
class UpdateThemeModeUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.updateThemeMode(mode)
}

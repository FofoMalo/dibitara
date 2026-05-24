package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Active ou désactive les notifications mensuelles de résumé.
class UpdateNotificationsMensuellesUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean) =
        repository.updateNotificationsMensuelles(enabled)
}

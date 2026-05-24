package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.DashboardCard
import com.dibitara.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

// Persiste le nouvel ordre des cartes du tableau de bord choisi par l'utilisateur.
class UpdateDashboardCardOrderUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(order: List<DashboardCard>) =
        repository.updateDashboardCardOrder(order)
}

package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.repository.VersementRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Somme des versements enregistrés pour un compte (SCPI ou épargne salariale) à partir
 * d'un mois donné inclus - utilisé pour neutraliser les versements dans le calcul de
 * performance depuis acquisition (voir [CalculerPerformanceActifUseCase]) : un versement
 * est de l'argent apporté, pas de la performance.
 */
class SommeVersementsDepuisUseCase @Inject constructor(
    private val repository: VersementRepository
) {
    suspend operator fun invoke(accountId: Long, type: CompteType, depuis: LocalDate): Long =
        repository.getForAccount(accountId, type).first()
            .filter { it.year > depuis.year || (it.year == depuis.year && it.month >= depuis.monthValue) }
            .sumOf { it.montantCents }
}

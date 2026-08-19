package com.dibitara.app.domain.usecase

import javax.inject.Inject

/**
 * Évolution de valeur d'un actif depuis son acquisition, nette des versements/abondements
 * effectués entre-temps (ceux-ci sont de l'argent apporté, pas de la performance).
 */
data class PerformanceActif(val deltaCents: Long, val deltaPct: Float)

/**
 * Delta "Acquisition → Aujourd'hui" (écran Placements), par opposition à
 * [CalculerTendanceActifUseCase] qui raisonne sur les 12 derniers mois de snapshots.
 * Retourne null si la valeur d'acquisition est nulle ou nulle en base (pourcentage non défini).
 */
class CalculerPerformanceActifUseCase @Inject constructor() {
    operator fun invoke(
        acquisitionValueCents: Long,
        currentValueCents: Long,
        versementsCumulesCents: Long = 0L
    ): PerformanceActif? {
        if (acquisitionValueCents == 0L) return null
        val deltaCents = currentValueCents - acquisitionValueCents - versementsCumulesCents
        val deltaPct = (deltaCents.toFloat() / acquisitionValueCents.toFloat()) * 100f
        return PerformanceActif(deltaCents, deltaPct)
    }
}

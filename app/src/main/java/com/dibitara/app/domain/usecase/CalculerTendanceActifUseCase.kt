package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.AssetValuationSnapshot
import javax.inject.Inject

/**
 * Variation de la valeur d'un actif (%) entre le plus ancien et le plus récent
 * snapshot disponibles - badge de tendance par actif (Immobilier/SCPI). Même
 * formule que [CalculerTendancePatrimoineUseCase], mais gardée séparée : ce
 * UseCase raisonne sur un actif individuel, pas sur le patrimoine global, et
 * les deux UseCases doivent pouvoir diverger librement plus tard (ex. règle de
 * profondeur d'historique minimale différente).
 *
 * Retourne null tant qu'il n'y a pas au moins 2 points, pour ne jamais afficher
 * un pourcentage trompeur.
 */
class CalculerTendanceActifUseCase @Inject constructor() {
    operator fun invoke(history: List<AssetValuationSnapshot>): Float? {
        if (history.size < 2) return null
        val premier = history.first().valueCents
        val dernier = history.last().valueCents
        if (premier == 0L) return null
        return ((dernier - premier).toFloat() / premier.toFloat()) * 100f
    }
}

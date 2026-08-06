package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.PatrimoineSnapshot
import javax.inject.Inject

/**
 * Variation du patrimoine net (%) entre le plus ancien et le plus récent snapshot
 * disponibles - badge de tendance global réutilisé par le Dashboard et l'écran
 * Placements (refonte UX/UI 2026-08).
 *
 * Un snapshot n'est enregistré que quand l'utilisateur visite l'écran Patrimoine :
 * l'historique peut donc être court ou absent. Retourne null tant qu'il n'y a pas
 * au moins 2 points, pour ne jamais afficher un pourcentage trompeur.
 */
class CalculerTendancePatrimoineUseCase @Inject constructor() {
    operator fun invoke(history: List<PatrimoineSnapshot>): Float? {
        if (history.size < 2) return null
        val premier = history.first().patrimoineNetCents
        val dernier = history.last().patrimoineNetCents
        if (premier == 0L) return null
        return ((dernier - premier).toFloat() / premier.toFloat()) * 100f
    }
}

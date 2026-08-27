package com.dibitara.app.domain.model

import java.time.LocalDate

/**
 * Résultat de la projection de trésorerie sur 30 jours.
 *
 * [soldeActuelCents]         - solde du budget restant aujourd'hui (point de départ)
 * [soldeProjecte30jCents]    - solde estimé à J+30 après tous les engagements
 * [jourPassageSeuilNegatif]  - premier jour où le solde passe sous [seuilFondsCents], null = aucun risque
 * [pointsTimeline]           - courbe jour par jour pour l'affichage graphique
 */
data class CashflowProjection(
    val soldeActuelCents: Long,
    val soldeProjecte30jCents: Long,
    val jourPassageSeuilNegatif: LocalDate?,
    val pointsTimeline: List<CashflowPoint>,
    val currency: Currency,
    val evenementsAVenir: List<EventProjecte> = emptyList()
)

/**
 * Un point sur la courbe de trésorerie.
 * [date]      - le jour concerné
 * [soldeCents] - solde estimé à la fin de ce jour après déduction des paiements du jour
 */
data class CashflowPoint(
    val date: LocalDate,
    val soldeCents: Long
)

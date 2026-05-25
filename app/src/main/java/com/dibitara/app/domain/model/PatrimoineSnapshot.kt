package com.dibitara.app.domain.model

import java.time.LocalDate

/**
 * Instantané du patrimoine à une date donnée.
 * Un snapshot est enregistré automatiquement une fois par jour quand l'utilisateur
 * ouvre PatrimoineDetail, ce qui permet de tracer l'évolution mensuelle du patrimoine net.
 */
data class PatrimoineSnapshot(
    val id: Long = 0,
    val snapshotDate: LocalDate,
    val patrimoineBrutCents: Long,
    val patrimoineNetCents: Long,
    val currency: Currency
)

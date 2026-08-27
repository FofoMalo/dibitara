package com.dibitara.app.domain.model

import java.time.LocalDate

data class EventProjecte(
    val date: LocalDate,
    val label: String,
    val montantCents: Long,   // toujours positif, dans la devise d'origine (non converti - voir currency)
    val currency: Currency,  // devise d'origine de la transaction/dette/compte source, pas la devise agrégée de la projection
    val sens: SensFlux
)

enum class SensFlux { ENTREE, SORTIE }

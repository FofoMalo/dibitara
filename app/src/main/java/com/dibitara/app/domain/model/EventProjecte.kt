package com.dibitara.app.domain.model

import java.time.LocalDate

data class EventProjecte(
    val date: LocalDate,
    val label: String,
    val montantCents: Long,   // toujours positif
    val sens: SensFlux
)

enum class SensFlux { ENTREE, SORTIE }

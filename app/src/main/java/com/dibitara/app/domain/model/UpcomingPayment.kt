package com.dibitara.app.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Prochaine échéance calculée à partir d'un modèle récurrent.
 *
 * [template] — la transaction modèle (isRecurring = true)
 * [nextDate] — date de la prochaine occurrence après aujourd'hui
 * [daysUntil] — nombre de jours jusqu'à [nextDate] (0 = aujourd'hui, négatif = passé)
 */
data class UpcomingPayment(
    val template: Transaction,
    val nextDate: LocalDate
) {
    val daysUntil: Long get() = ChronoUnit.DAYS.between(LocalDate.now(), nextDate)
}

package com.dibitara.app.domain.model

/**
 * Fréquence de répétition d'une transaction récurrente.
 *
 * WEEKLY  — même jour de la semaine que [Transaction.firstPaymentDate]
 * MONTHLY — même jour du mois que [Transaction.recurrenceDay] (1-28)
 * YEARLY  — même jour/mois que [Transaction.firstPaymentDate]
 */
enum class RecurrenceFrequency(val displayName: String) {
    WEEKLY("Hebdomadaire"),
    MONTHLY("Mensuelle"),
    YEARLY("Annuelle")
}

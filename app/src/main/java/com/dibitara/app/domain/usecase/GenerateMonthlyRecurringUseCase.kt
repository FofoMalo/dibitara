package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Génère les occurrences mensuelles des transactions récurrentes.
 *
 * Règles :
 * - Ne génère rien le mois où le modèle a été créé (le modèle lui-même compte comme première occurrence).
 * - Ne génère pas de doublon si une occurrence existe déjà ce mois-ci.
 * - Le jour est limité à 28 pour éviter les problèmes avec février.
 *
 * À appeler au démarrage de l'application (via AppViewModel).
 */
class GenerateMonthlyRecurringUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke() {
        val today = LocalDate.now()

        // Snapshot unique des modèles — first() termine le flow immédiatement
        val templates = repository.getRecurring().first()

        for (template in templates) {
            val day = template.recurrenceDay ?: continue

            // Le modèle lui-même représente la première occurrence : on saute le mois de création
            val createdThisMonth = template.date.monthValue == today.monthValue
                    && template.date.year == today.year
            if (createdThisMonth) continue

            // Évite de générer un doublon si l'occurrence de ce mois existe déjà
            val alreadyGenerated = repository.hasRecurringOccurrenceThisMonth(
                recurringId = template.id,
                month = today.monthValue,
                year = today.year
            )
            if (alreadyGenerated) continue

            // Adapte le jour au nombre de jours réels du mois (ex. 30 → 28 en février)
            val safeDay = day.coerceAtMost(today.month.length(today.isLeapYear))

            repository.insert(
                template.copy(
                    id = 0,                             // Room génère un nouvel ID
                    date = LocalDate.of(today.year, today.monthValue, safeDay),
                    isRecurring = false,                // L'occurrence n'est pas un modèle
                    sourceRecurringId = template.id     // Lien vers le modèle d'origine
                )
            )
        }
    }
}

package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Debt
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Identifie les dettes dont l'échéance mensuelle tombe aujourd'hui.
 *
 * Convention : le jour de l'échéance est déduit de [Debt.updatedAt].
 * Par exemple, une dette enregistrée le 15 du mois déclenchera un rappel
 * chaque 15 du mois.
 *
 * Retourne la liste des dettes concernées (vide si aucune échéance aujourd'hui).
 */
class CheckDebtRemindersUseCase @Inject constructor(
    private val getDebts: GetDebtsUseCase
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): List<Debt> {
        return getDebts().first()
            .filter { it.updatedAt.dayOfMonth == today.dayOfMonth }
    }
}

package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.repository.VersementRepository
import javax.inject.Inject

/** Enregistre un versement mensuel en base. Échoue si un versement existe déjà pour ce mois. */
class SaveVersementUseCase @Inject constructor(private val repository: VersementRepository) {
    suspend operator fun invoke(versement: MonthlyVersement): Result<Long> =
        repository.save(versement)
}

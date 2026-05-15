package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.repository.VersementRepository
import javax.inject.Inject

/** Vérifie si un versement a déjà été enregistré pour un compte donné sur un mois donné. */
class ExisteVersementMoisUseCase @Inject constructor(private val repository: VersementRepository) {
    suspend operator fun invoke(accountId: Long, type: CompteType, year: Int, month: Int): Boolean =
        repository.existsPourMois(accountId, type, year, month)
}

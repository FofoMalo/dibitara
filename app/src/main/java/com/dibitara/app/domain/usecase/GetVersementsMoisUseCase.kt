package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.repository.VersementRepository
import javax.inject.Inject

/**
 * Versements déjà enregistrés pour un type de compte donné, sur un mois donné.
 * Retourne la liste brute (montants dans leur devise d'origine) plutôt qu'une somme :
 * la conversion vers la devise d'affichage se fait côté ViewModel, comme pour les autres totaux.
 */
class GetVersementsMoisUseCase @Inject constructor(private val repository: VersementRepository) {
    suspend operator fun invoke(type: CompteType, year: Int, month: Int): List<MonthlyVersement> =
        repository.getAllPourMois(type, year, month)
}

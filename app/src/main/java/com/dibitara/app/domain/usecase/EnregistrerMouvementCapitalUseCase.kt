package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.repository.VersementRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Enregistre un mouvement de capital ponctuel (apport ou retrait, [montantDeltaCents] signé)
 * sur un compte sans contribution mensuelle fixe - actif libre ([CompteType.CUSTOM_ASSET]) ou
 * bien immobilier ([CompteType.REAL_ESTATE], ex. travaux qui font remonter la valeur affichée
 * sans que ce soit une plus-value de marché). Contrairement à [SaveVersementUseCase] (un seul
 * versement mensuel régulier, échoue au second appel du mois), ce cas cumule avec un éventuel
 * mouvement déjà enregistré ce mois-ci : rien n'empêche Florent de faire un retrait puis un
 * apport sur le même CTO dans le même mois. Neutralisé ensuite dans
 * [CalculerPerformanceActifUseCase] via [SommeVersementsDepuisUseCase] : un mouvement de
 * capital n'est pas de la performance.
 *
 * Réservé aux types d'actif sans versement mensuel récurrent existant (voir [CompteType]) :
 * réutiliser ce UseCase pour SCPI/EMPLOYEE_SAVINGS collisionnerait avec la contrainte UNIQUE
 * (compte, type, mois) déjà occupée par `appliquerVersementScpi`/`appliquerVersementEmployeeSavings`.
 */
class EnregistrerMouvementCapitalUseCase @Inject constructor(
    private val repository: VersementRepository
) {
    suspend operator fun invoke(
        accountId: Long,
        type: CompteType,
        montantDeltaCents: Long,
        currency: Currency,
        date: LocalDate = LocalDate.now()
    ): Result<Unit> {
        val existant = repository.getPourMoisEtCompte(accountId, type, date.year, date.monthValue)
        return if (existant != null) {
            repository.update(existant.copy(montantCents = existant.montantCents + montantDeltaCents))
        } else {
            repository.save(
                MonthlyVersement(
                    accountId = accountId,
                    compteType = type,
                    year = date.year,
                    month = date.monthValue,
                    montantCents = montantDeltaCents,
                    currency = currency
                )
            ).fold(onSuccess = { Result.success(Unit) }, onFailure = { Result.failure(it) })
        }
    }
}

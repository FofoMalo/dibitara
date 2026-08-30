package com.dibitara.app.domain.model

/**
 * Enregistrement d'un versement mensuel effectué sur un compte épargne ou une SCPI.
 * Un seul versement est autorisé par compte et par mois (contrainte unique en base).
 * La non-rétroactivité est assurée par le fait qu'on enregistre [montantCents] au
 * moment du versement - modifier le montant prévu sur le compte ne change pas les
 * enregistrements passés.
 */
data class MonthlyVersement(
    val id: Long = 0,
    val accountId: Long,
    val compteType: CompteType,
    val year: Int,
    val month: Int,
    val montantCents: Long,
    val currency: Currency
)

/**
 * Distingue les versements épargne (SavingsAccount), SCPI (ScpiInvestment), épargne
 * salariale (EmployeeSavings), objectif d'épargne (SavingsGoal), actif libre (CustomAsset)
 * et bien immobilier (RealEstateAsset).
 *
 * Stockée en base comme `String` dans `monthly_versements.account_type` (lue en
 * `safeValueOf`) : ajouter une valeur ne nécessite **aucune migration Room**.
 * Pour un versement `OBJECTIF`, [MonthlyVersement.accountId] porte l'id du
 * `SavingsGoal` (comme pour SCPI/EMPLOYEE_SAVINGS qui pointent leurs propres tables).
 *
 * `CUSTOM_ASSET` et `REAL_ESTATE` s'écartent du sens habituel « versement mensuel régulier » :
 * ils portent un mouvement de capital ponctuel (apport ou retrait, signé), cumulé au sein du
 * même mois plutôt que rejeté par la contrainte UNIQUE - voir
 * [com.dibitara.app.domain.usecase.EnregistrerMouvementCapitalUseCase]. Ce sont les deux seuls
 * types d'actif sans versement mensuel récurrent existant au moment de leur introduction, donc
 * sans collision possible avec la contrainte « un seul versement par compte et par mois ».
 *
 * `SCPI_MOUVEMENT` et `EMPLOYEE_SAVINGS_MOUVEMENT` portent le même genre de mouvement ponctuel
 * que `CUSTOM_ASSET`/`REAL_ESTATE`, mais pour SCPI et épargne salariale qui, elles, ONT déjà un
 * versement mensuel récurrent (`CompteType.SCPI`/`EMPLOYEE_SAVINGS`, un seul par compte et par
 * mois - voir `appliquerVersementScpi`/`appliquerVersementEmployeeSavings` dans
 * `InvestmentsViewModel`). Réutiliser directement `SCPI`/`EMPLOYEE_SAVINGS` pour un mouvement
 * ad hoc écrirait dans la même ligne `monthly_versements` que ce versement mensuel et le
 * ferait échouer - d'où une voie séparée pour le même compte. `SommeVersementsDepuisUseCase`
 * est appelé sur les deux voies et les totaux additionnés (voir
 * `InvestmentsViewModel.performanceDepuisAcquisition`).
 */
enum class CompteType { EPARGNE, SCPI, EMPLOYEE_SAVINGS, OBJECTIF, CUSTOM_ASSET, REAL_ESTATE, SCPI_MOUVEMENT, EMPLOYEE_SAVINGS_MOUVEMENT }

package com.dibitara.app.domain.model

/** Format de fichier choisi par l'utilisateur pour l'export. */
enum class ExportFormat { CSV, JSON }

/**
 * Agrégat de toutes les données exportables de l'application.
 * Construit par [com.dibitara.app.domain.usecase.ExporterDonneesUseCase]
 * à partir des différents repositories.
 */
data class ExportData(
    val enfants        : List<Child>,           // en premier - clé étrangère childId dans transactions
    val transactions   : List<Transaction>,
    val budgets        : List<Budget>,
    val epargne        : List<SavingsAccount>,
    val immobilier     : List<RealEstateAsset>,
    val scpi           : List<ScpiInvestment>,
    val airbnb         : List<AirbnbRental>,
    val dettes         : List<Debt>,
    val metaux         : List<PreciousMetalAsset>,
    val actifsLibres   : List<CustomAsset>,
    val epargneSalariale: List<EmployeeSavings>
)

package com.dibitara.app.domain.model

/** Format de fichier choisi par l'utilisateur pour l'export. */
enum class ExportFormat { CSV, JSON }

/**
 * Agrégat de toutes les données exportables de l'application.
 * Construit par [com.dibitara.app.domain.usecase.ExporterDonneesUseCase]
 * à partir des différents repositories.
 */
data class ExportData(
    val transactions : List<Transaction>,
    val budgets      : List<Budget>,
    val epargne      : List<SavingsAccount>,
    val immobilier   : List<RealEstateAsset>,
    val scpi         : List<ScpiInvestment>,
    val airbnb       : List<AirbnbRental>,
    val dettes       : List<Debt>
)

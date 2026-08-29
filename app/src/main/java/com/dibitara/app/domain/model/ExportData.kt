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
    val vehiculeLocatif: List<VehicleRentalEntry>,
    val dettes         : List<Debt>,
    val actifsLibres   : List<CustomAsset>,
    val epargneSalariale: List<EmployeeSavings>,
    // Ajoutées 2026-08 : ces collections manquaient à la sauvegarde JSON et étaient donc
    // perdues à la restauration (versements du mois recompté, sous-catégories orphelines...).
    val sousCategoriesPerso : List<CustomSubCategory>,
    val comptesBancaires    : List<BankAccount>,
    val enveloppesBudget    : List<CategoryEnvelope>,
    val reglesCategorisation: List<CategorizationRule>,
    val versementsMensuels  : List<MonthlyVersement>
)

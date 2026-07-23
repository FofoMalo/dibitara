package com.dibitara.app.domain.usecase

import android.net.Uri
import com.dibitara.app.domain.model.ExportData
import com.dibitara.app.domain.model.ExportFormat
import com.dibitara.app.domain.repository.BudgetRepository
import com.dibitara.app.domain.repository.ChildRepository
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import com.dibitara.app.domain.repository.DebtRepository
import com.dibitara.app.domain.repository.ExportRepository
import com.dibitara.app.domain.repository.InvestmentRepository
import com.dibitara.app.domain.repository.SavingsRepository
import com.dibitara.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Collecte toutes les données de l'application, puis délègue l'écriture
 * du fichier à [ExportRepository].
 *
 * Retourne l'Uri du fichier généré, prêt à être partagé via un Intent Android.
 */
class ExporterDonneesUseCase @Inject constructor(
    private val transactionRepository      : TransactionRepository,
    private val budgetRepository           : BudgetRepository,
    private val savingsRepository          : SavingsRepository,
    private val investmentRepository       : InvestmentRepository,
    private val debtRepository             : DebtRepository,
    private val customInvestmentRepository : CustomInvestmentRepository,
    private val childRepository            : ChildRepository,
    private val exportRepository           : ExportRepository
) {
    suspend operator fun invoke(format: ExportFormat): Uri {
        // On prend la première émission de chaque Flow - capture instantanée des données
        val data = ExportData(
            enfants         = childRepository.getAll().first(),
            transactions    = transactionRepository.getAll().first(),
            budgets         = budgetRepository.getAll().first(),
            epargne         = savingsRepository.getAll().first(),
            immobilier      = investmentRepository.getAllRealEstate().first(),
            scpi            = investmentRepository.getAllScpi().first(),
            airbnb          = investmentRepository.getAllAirbnbRentals().first(),
            vehiculeLocatif = investmentRepository.getAllVehicleRentalEntries().first(),
            dettes          = debtRepository.getAll().first(),
            actifsLibres    = customInvestmentRepository.getAllCustomAssets().first(),
            epargneSalariale = customInvestmentRepository.getAllEmployeeSavings().first()
        )
        return exportRepository.exporter(data, format)
    }
}

package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.repository.InvestmentRepository
import com.dibitara.app.domain.repository.SavingsRepository
import com.dibitara.app.domain.repository.VersementRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Résultat du calcul des contributions en attente pour un mois donné.
 */
data class PendingContributions(
    val count: Int,
    val totalCents: Long
)

/**
 * Calcule le nombre et le montant total des versements mensuels non encore effectués
 * pour un mois/année donnés, en parcourant les comptes épargne et les SCPI actifs.
 *
 * Un versement est considéré « en attente » si :
 *  - le compte a une contribution mensuelle > 0 (il est configuré pour recevoir un versement)
 *  - aucun enregistrement [MonthlyVersement] n'existe pour ce compte / ce mois-ci
 */
class CheckPendingContributionsUseCase @Inject constructor(
    private val savingsRepository    : SavingsRepository,
    private val investmentRepository : InvestmentRepository,
    private val versementRepository  : VersementRepository
) {
    suspend operator fun invoke(month: Int, year: Int): PendingContributions {
        var count      = 0
        var totalCents = 0L

        // Comptes épargne avec contribution mensuelle configurée
        val savingsAccounts = savingsRepository.getAll().first()
        for (account in savingsAccounts) {
            if (account.monthlyContributionCents > 0) {
                val existe = versementRepository.existsPourMois(
                    account.id, CompteType.EPARGNE, year, month
                )
                if (!existe) {
                    count++
                    totalCents += account.monthlyContributionCents
                }
            }
        }

        // SCPI avec contribution mensuelle configurée
        val scpiList = investmentRepository.getAllScpi().first()
        for (scpi in scpiList) {
            if (scpi.monthlyContributionCents > 0) {
                val existe = versementRepository.existsPourMois(
                    scpi.id, CompteType.SCPI, year, month
                )
                if (!existe) {
                    count++
                    totalCents += scpi.monthlyContributionCents
                }
            }
        }

        return PendingContributions(count = count, totalCents = totalCents)
    }
}

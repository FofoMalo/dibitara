package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BudgetBucket
import com.dibitara.app.domain.model.CapaciteLogementResult
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.model.PocheReduction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.model.bucket
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Simule la capacité à conserver un bien immobilier (typiquement la résidence principale)
 * avec un revenu hypothétique, à partir des dépenses et engagements réels des 3 derniers mois.
 *
 * Voir [CapaciteLogementResult] pour le détail des champs, notamment pourquoi la mensualité
 * du crédit n'est volontairement PAS additionnée dans le revenu plancher (déjà comptée via
 * les dépenses réelles catégorisées LOGEMENT).
 */
class SimulerCapaciteLogementUseCase @Inject constructor(
    private val getRealEstate               : GetRealEstateUseCase,
    private val getDebts                    : GetDebtsUseCase,
    private val getSavings                  : GetSavingsUseCase,
    private val getMonthlyTransactions      : GetMonthlyTransactionsUseCase,
    private val userPreferencesRepo         : UserPreferencesRepository,
    private val exchangeRateRepo            : ExchangeRateRepository,
    private val identifierVirementsInternes : IdentifierVirementsInternesUseCase
) {
    operator fun invoke(
        realEstateAssetId: Long,
        revenuSimuleCents: Long,
        refMonth: Int = LocalDate.now().monthValue,
        refYear: Int = LocalDate.now().year
    ): Flow<CapaciteLogementResult?> {
        val moisAnalyses = (1..3).map { offset ->
            LocalDate.of(refYear, refMonth, 1).minusMonths(offset.toLong())
                .let { it.monthValue to it.year }
        }

        val transactionsFlow = combine(
            getMonthlyTransactions(moisAnalyses[0].first, moisAnalyses[0].second),
            getMonthlyTransactions(moisAnalyses[1].first, moisAnalyses[1].second),
            getMonthlyTransactions(moisAnalyses[2].first, moisAnalyses[2].second)
        ) { m1, m2, m3 -> listOf(m1, m2, m3) }

        val conversionFlow = combine(
            userPreferencesRepo.get(),
            exchangeRateRepo.getRatesFlow()
        ) { prefs, rates -> prefs to rates }

        return combine(
            getRealEstate(),
            getDebts(),
            getSavings(),
            transactionsFlow,
            conversionFlow
        ) { biens, dettes, comptes, txParMoisBrut, (prefs, rates) ->

            val bien = biens.firstOrNull { it.id == realEstateAssetId } ?: return@combine null
            val devise = prefs.deviseParDefaut
            fun Long.cvt(from: Currency) = CurrencyConverter.convertCents(this, from, devise, rates)

            val idsExclus = mutableSetOf<Long>()
            val txParMois = txParMoisBrut.map { transactions ->
                idsExclus += identifierVirementsInternes(transactions)
                transactions.filterNot { it.id in idsExclus }
            }

            // ─── Revenu réel moyen sur 3 mois (situation actuelle, pour comparaison) ────
            val revenuReelMoyenCents = txParMois.map { transactions ->
                transactions.filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amountCents.cvt(it.currency) }
            }.average().toLong()

            // ─── Mensualité du crédit lié au bien - informative uniquement ──────────────
            val detteMaison = bien.debtId?.let { debtId -> dettes.firstOrNull { it.id == debtId } }
            val mensualiteMaisonCents = detteMaison?.let { it.monthlyPaymentCents.cvt(it.currency) }

            // ─── Autres engagements incompressibles (hors crédit du bien) ───────────────
            val autresEngagementsCents =
                dettes.filter { it.id != bien.debtId }.sumOf { it.monthlyPaymentCents.cvt(it.currency) } +
                comptes.sumOf { it.monthlyContributionCents.cvt(it.currency) }

            // ─── Dépenses réelles par catégorie, moyenne sur 3 mois ─────────────────────
            val depensesParCategorie = txParMois.flatten()
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { (_, txs) -> txs.sumOf { it.amountCents.cvt(it.currency) } / 3 }

            val besoinsIncompressiblesCents = depensesParCategorie
                .filterKeys { it.bucket == BudgetBucket.BESOINS }
                .values.sum()

            val enviesTrieesDesc = depensesParCategorie
                .filterKeys { it.bucket == BudgetBucket.ENVIES }
                .toList()
                .sortedByDescending { (_, moyenne) -> moyenne }

            // ─── Plancher et viabilité du scénario simulé ───────────────────────────────
            val revenuPlancherCents    = autresEngagementsCents + besoinsIncompressiblesCents
            val resteAVivreReelCents   = revenuReelMoyenCents - revenuPlancherCents
            val resteAVivreSimuleCents = revenuSimuleCents - revenuPlancherCents
            val seuilResteAVivreCents  = prefs.seuilFondsCents
            val estTenable             = resteAVivreSimuleCents >= seuilResteAVivreCents

            // ─── Suggestions de réduction sur les poches Envies si le scénario est tendu ─
            var manquant = (seuilResteAVivreCents - resteAVivreSimuleCents).coerceAtLeast(0L)
            val reductions = mutableListOf<PocheReduction>()
            for ((categorie, moyenne) in enviesTrieesDesc) {
                if (manquant <= 0) break
                val capMax = moyenne / 2
                val coupe = minOf(manquant, capMax)
                if (coupe > 0) {
                    reductions += PocheReduction(categorie, moyenne, coupe)
                    manquant -= coupe
                }
            }

            CapaciteLogementResult(
                currency                    = devise,
                bienLabel                   = bien.label,
                mensualiteMaisonCents       = mensualiteMaisonCents,
                autresEngagementsCents      = autresEngagementsCents,
                besoinsIncompressiblesCents = besoinsIncompressiblesCents,
                revenuPlancherCents         = revenuPlancherCents,
                revenuReelMoyenCents        = revenuReelMoyenCents,
                revenuSimuleCents           = revenuSimuleCents,
                resteAVivreReelCents        = resteAVivreReelCents,
                resteAVivreSimuleCents      = resteAVivreSimuleCents,
                seuilResteAVivreCents       = seuilResteAVivreCents,
                estTenable                  = estTenable,
                pochesEnviesReduction       = reductions,
                ecartNonCouvertCents        = manquant.coerceAtLeast(0L)
            )
        }
    }
}

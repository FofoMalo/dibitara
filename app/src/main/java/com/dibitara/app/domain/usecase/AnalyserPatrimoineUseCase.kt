package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BudgetBucket
import com.dibitara.app.domain.model.CategoriePatrimoine
import com.dibitara.app.domain.model.ConseilPatrimoineResult
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.model.PocheAvecMarge
import com.dibitara.app.domain.model.RepartitionCategorie
import com.dibitara.app.domain.model.SEUIL_CONCENTRATION_PCT
import com.dibitara.app.domain.model.SavingsType
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.model.bucket
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/** Types d'épargne considérés immédiatement sûrs et liquides (voir [ConseilPatrimoineResult]). */
private val TYPES_LIQUIDES_SURS = setOf(SavingsType.LIVRET_A, SavingsType.LDDS, SavingsType.COMPTE_COURANT)

/**
 * Analyse "Conseiller patrimoine" : où en sont les poches d'épargne et d'investissement,
 * à partir des seules données réelles. Voir [ConseilPatrimoineResult] pour le détail des
 * 4 axes et pourquoi la mensualité d'un crédit immobilier n'est pas comptée deux fois.
 */
class AnalyserPatrimoineUseCase @Inject constructor(
    private val getRealEstate               : GetRealEstateUseCase,
    private val getScpi                     : GetScpiUseCase,
    private val getCustomAssets             : GetCustomAssetsUseCase,
    private val getEmployeeSavings          : GetEmployeeSavingsUseCase,
    private val getDebts                    : GetDebtsUseCase,
    private val getSavings                  : GetSavingsUseCase,
    private val getBankAccounts             : GetBankAccountsUseCase,
    private val getMonthlyTransactions      : GetMonthlyTransactionsUseCase,
    private val userPreferencesRepo         : UserPreferencesRepository,
    private val exchangeRateRepo            : ExchangeRateRepository,
    private val identifierVirementsInternes : IdentifierVirementsInternesUseCase
) {
    operator fun invoke(
        refMonth: Int = LocalDate.now().monthValue,
        refYear: Int = LocalDate.now().year
    ): Flow<ConseilPatrimoineResult> {
        val moisAnalyses = (1..3).map { offset ->
            LocalDate.of(refYear, refMonth, 1).minusMonths(offset.toLong())
                .let { it.monthValue to it.year }
        }

        val transactionsFlow = combine(
            getMonthlyTransactions(moisAnalyses[0].first, moisAnalyses[0].second),
            getMonthlyTransactions(moisAnalyses[1].first, moisAnalyses[1].second),
            getMonthlyTransactions(moisAnalyses[2].first, moisAnalyses[2].second)
        ) { m1, m2, m3 -> listOf(m1, m2, m3) }

        val investissementsFlow = combine(
            getRealEstate(), getScpi(), getCustomAssets(), getEmployeeSavings()
        ) { immo, scpi, actifsLibres, empSavings -> Investissements(immo, scpi, actifsLibres, empSavings) }

        val patrimoineFlow = combine(
            getDebts(), getSavings(), getBankAccounts()
        ) { dettes, epargne, comptes -> Patrimoine(dettes, epargne, comptes) }

        val conversionFlow = combine(
            userPreferencesRepo.get(),
            exchangeRateRepo.getRatesFlow()
        ) { prefs, rates -> prefs to rates }

        return combine(
            transactionsFlow, investissementsFlow, patrimoineFlow, conversionFlow
        ) { txParMoisBrut, investissements, patrimoine, (prefs, rates) ->

            val devise = prefs.deviseParDefaut
            fun Long.cvt(from: Currency) = CurrencyConverter.convertCents(this, from, devise, rates)

            val idsExclus = mutableSetOf<Long>()
            val txParMois = txParMoisBrut.map { transactions ->
                idsExclus += identifierVirementsInternes(transactions)
                transactions.filterNot { it.id in idsExclus }
            }

            // ─── Revenu et besoins réels moyens sur 3 mois ──────────────────────────
            val revenuMoyenCents = txParMois.map { transactions ->
                transactions.filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amountCents.cvt(it.currency) }
            }.average().toLong()

            val besoinsIncompressiblesCents = txParMois.flatten()
                .filter { it.type == TransactionType.EXPENSE && it.category.bucket == BudgetBucket.BESOINS }
                .sumOf { it.amountCents.cvt(it.currency) } / 3

            // ─── Dettes hors crédit d'un bien immobilier (déjà comptées via LOGEMENT) ───
            val idsDettesLieesImmo = investissements.immo.mapNotNull { it.debtId }.toSet()
            val autresDettesCents = patrimoine.dettes
                .filter { it.id !in idsDettesLieesImmo }
                .sumOf { it.monthlyPaymentCents.cvt(it.currency) }

            val resteAVivreReelCents = revenuMoyenCents - besoinsIncompressiblesCents - autresDettesCents

            // ─── Axe 1 : épargne de précaution ───────────────────────────────────────
            val objectifPrecautionCents = 6 * (besoinsIncompressiblesCents + autresDettesCents)
            val liquiditesSuresCents =
                patrimoine.comptes.sumOf { it.currentBalanceCents.cvt(it.currency) } +
                patrimoine.epargne.filter { it.type in TYPES_LIQUIDES_SURS }
                    .sumOf { it.currentBalanceCents.cvt(it.currency) }
            val precautionSuffisante = liquiditesSuresCents >= objectifPrecautionCents

            // ─── Axe 2 : poches avec marge avant plafond ─────────────────────────────
            val pochesAvecMarge = patrimoine.epargne
                .filter { it.plafondCents != null && it.currentBalanceCents < it.plafondCents }
                .map { compte ->
                    val solde   = compte.currentBalanceCents.cvt(compte.currency)
                    val plafond = compte.plafondCents!!.cvt(compte.currency)
                    PocheAvecMarge(compte.label, solde, plafond, plafond - solde)
                }
                .sortedByDescending { it.margeCents }

            // ─── Axe 3 : capacité d'épargne mensuelle non affectée ───────────────────
            val objectifEpargneMensuelCents = revenuMoyenCents * prefs.tauxEpargneCiblePct / 100
            val versementsProgrammesCents =
                patrimoine.epargne.sumOf { it.monthlyContributionCents.cvt(it.currency) } +
                investissements.scpi.sumOf { it.monthlyContributionCents.cvt(it.currency) }
            val capaciteCibleCents = minOf(objectifEpargneMensuelCents, resteAVivreReelCents.coerceAtLeast(0L))
            val capaciteNonAffecteeCents = (capaciteCibleCents - versementsProgrammesCents).coerceAtLeast(0L)
            val objectifPlafonneParResteAVivre = resteAVivreReelCents < objectifEpargneMensuelCents

            // ─── Axe 4 : concentration par catégorie (immobilier net du crédit lié) ──
            val immobilierNetCents = investissements.immo.sumOf { bien ->
                val detteRestante = bien.debtId
                    ?.let { id -> patrimoine.dettes.firstOrNull { it.id == id } }
                    ?.let { it.totalCents.cvt(it.currency) }
                    ?: 0L
                (bien.currentValueCents.cvt(bien.currency) - detteRestante).coerceAtLeast(0L)
            }
            val scpiCents          = investissements.scpi.sumOf { it.totalValueCents.cvt(it.currency) }
            val actifsLibresCents  = investissements.actifsLibres.sumOf { it.totalValueCents.cvt(it.currency) }
            val empSavingsCents    = investissements.empSavings.sumOf { it.currentBalanceCents.cvt(it.currency) }
            val epargneCents       = patrimoine.epargne.sumOf { it.currentBalanceCents.cvt(it.currency) }

            val montantsParCategorie = mapOf(
                CategoriePatrimoine.EPARGNE           to epargneCents,
                CategoriePatrimoine.IMMOBILIER        to immobilierNetCents,
                CategoriePatrimoine.SCPI              to scpiCents,
                CategoriePatrimoine.ACTIFS_LIBRES     to actifsLibresCents,
                CategoriePatrimoine.EPARGNE_SALARIALE to empSavingsCents
            )
            val totalPatrimoineCents = montantsParCategorie.values.sum()
            val repartition = montantsParCategorie.map { (categorie, montant) ->
                RepartitionCategorie(
                    categorie    = categorie,
                    montantCents = montant,
                    pourcentage  = if (totalPatrimoineCents > 0) montant * 100f / totalPatrimoineCents else 0f
                )
            }

            ConseilPatrimoineResult(
                currency                       = devise,
                liquiditesSuresCents           = liquiditesSuresCents,
                objectifPrecautionCents        = objectifPrecautionCents,
                precautionSuffisante           = precautionSuffisante,
                pochesAvecMarge                = pochesAvecMarge,
                objectifEpargneMensuelCents    = objectifEpargneMensuelCents,
                resteAVivreReelCents           = resteAVivreReelCents,
                versementsProgrammesCents      = versementsProgrammesCents,
                capaciteNonAffecteeCents       = capaciteNonAffecteeCents,
                objectifPlafonneParResteAVivre = objectifPlafonneParResteAVivre,
                repartitionParCategorie        = repartition,
                categorieSurConcentree         = repartition.firstOrNull { it.pourcentage > SEUIL_CONCENTRATION_PCT }
            )
        }
    }

    private data class Investissements(
        val immo         : List<com.dibitara.app.domain.model.RealEstateAsset>,
        val scpi          : List<com.dibitara.app.domain.model.ScpiInvestment>,
        val actifsLibres  : List<com.dibitara.app.domain.model.CustomAsset>,
        val empSavings    : List<com.dibitara.app.domain.model.EmployeeSavings>
    )

    private data class Patrimoine(
        val dettes  : List<com.dibitara.app.domain.model.Debt>,
        val epargne  : List<com.dibitara.app.domain.model.SavingsAccount>,
        val comptes  : List<com.dibitara.app.domain.model.BankAccount>
    )
}

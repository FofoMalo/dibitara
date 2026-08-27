package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CategoryEnvelope
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.model.PocheRecommandee
import com.dibitara.app.domain.model.SpendingRecommendation
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.model.bucket
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Calcule les recommandations budgétaires mensuelles à partir des 3 derniers mois de données réelles.
 *
 * Algorithme en 5 étapes :
 *  1. Revenu de référence = moyenne des revenus sur les 3 mois précédant [refMonth]/[refYear].
 *  2. Engagements incompressibles = mensualités dettes + contributions épargne programmées.
 *  3. Poches suggérées = moyenne des dépenses par catégorie, arrondie au 10 € supérieur.
 *  4. Classification 50/30/20 : chaque catégorie rangée dans Besoins, Envies ou Épargne/Dettes.
 *  5. Viabilité = revenuMoyen - engagements - sum(poches) ≥ 0.
 *
 * Les catégories AUTRE et TRANSFERTS sont exclues - trop vagues pour être recommandées.
 * Retourne un Flow actif : la recommandation se recalcule si les données sources changent.
 */
class GetSpendingRecommendationsUseCase @Inject constructor(
    private val getMonthlyTransactions      : GetMonthlyTransactionsUseCase,
    private val getDebts                    : GetDebtsUseCase,
    private val getSavings                  : GetSavingsUseCase,
    private val getCategoryEnvelopes        : GetCategoryEnvelopesUseCase,
    private val userPreferencesRepo         : UserPreferencesRepository,
    private val exchangeRateRepo            : ExchangeRateRepository,
    private val identifierVirementsInternes : IdentifierVirementsInternesUseCase
) {
    operator fun invoke(refMonth: Int, refYear: Int): Flow<SpendingRecommendation> {
        // Les 3 mois précédant le mois de référence (du plus récent au plus ancien)
        val moisAnalyses = (1..3).map { offset ->
            LocalDate.of(refYear, refMonth, 1).minusMonths(offset.toLong())
                .let { it.monthValue to it.year }
        }

        val txFlow1 = getMonthlyTransactions(moisAnalyses[0].first, moisAnalyses[0].second)
        val txFlow2 = getMonthlyTransactions(moisAnalyses[1].first, moisAnalyses[1].second)
        val txFlow3 = getMonthlyTransactions(moisAnalyses[2].first, moisAnalyses[2].second)

        // Combine les 3 mois de transactions en une seule liste de listes
        val transactionsFlow = combine(txFlow1, txFlow2, txFlow3) { m1, m2, m3 ->
            listOf(m1, m2, m3)
        }

        // Combine devise cible + taux de change
        val conversionFlow = combine(
            userPreferencesRepo.get(),
            exchangeRateRepo.getRatesFlow()
        ) { prefs, rates -> prefs to rates }

        return combine(
            transactionsFlow,
            getDebts(),
            getSavings(),
            getCategoryEnvelopes(),
            conversionFlow
        ) { txParMoisBrut, dettes, comptes, enveloppes, (prefs, rates) ->

            val devise = prefs.deviseParDefaut
            fun Long.cvt(from: Currency) = CurrencyConverter.convertCents(this, from, devise, rates)

            // Exclut les virements internes BRED↔TradeRepublic appariés de chaque mois avant tout
            // calcul (voir IdentifierVirementsInternesUseCase) : un déplacement entre comptes
            // suivis n'est ni un revenu ni une dépense réelle.
            val txParMois = txParMoisBrut.map { transactions ->
                val idsExclus = identifierVirementsInternes(transactions)
                transactions.filterNot { it.id in idsExclus }
            }

            // ─── Étape 1 : Revenu moyen sur 3 mois ──────────────────────────────
            val revenuParMois = txParMois.map { transactions ->
                transactions.filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amountCents.cvt(it.currency) }
            }
            val revenuMoyen = if (revenuParMois.isNotEmpty()) revenuParMois.average().toLong() else 0L

            // Taux d'épargne moyen réel observé sur les 3 mois (pour affichage comparatif)
            val tauxEpargneActuelPct = if (revenuMoyen > 0) {
                val depensesMoyennes = txParMois.map { transactions ->
                    transactions.filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amountCents.cvt(it.currency) }
                }.average().toLong()
                ((revenuMoyen - depensesMoyennes) * 100 / revenuMoyen).toInt().coerceIn(0, 100)
            } else null

            // ─── Étape 2 : Engagements incompressibles ──────────────────────────
            // Ces montants sont déjà "prélevés" avant toute recommandation de dépense
            val mensualitesDettes    = dettes.sumOf { it.monthlyPaymentCents.cvt(it.currency) }
            val contributionsEpargne = comptes.sumOf { it.monthlyContributionCents.cvt(it.currency) }
            val engagements = mensualitesDettes + contributionsEpargne

            // ─── Étape 3 : Poches suggérées par catégorie ───────────────────────
            val depensesParCategorie = txParMois.flatten()
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { (_, txs) -> txs.sumOf { it.amountCents.cvt(it.currency) } }

            // Index des enveloppes existantes (Sprint 40) pour les afficher en comparaison
            val enveloppeParCategorie: Map<Category, CategoryEnvelope> =
                enveloppes.associateBy { it.category }

            val poches = depensesParCategorie
                .mapNotNull { (categorie, totalSur3Mois) ->
                    // Les catégories sans bucket (AUTRE, TRANSFERTS) sont exclues
                    val bucket = categorie.bucket ?: return@mapNotNull null

                    val moyenneCents   = totalSur3Mois / 3
                    val recommandeCents = arrondiAu10EurosSuperieur(moyenneCents)

                    PocheRecommandee(
                        category           = categorie,
                        moyenneCents       = moyenneCents,
                        recommandeCents    = recommandeCents,
                        bucket             = bucket,
                        enveloppeExistante = enveloppeParCategorie[categorie]
                    )
                }
                // Tri : d'abord par bucket (Besoins → Envies → Épargne), puis par montant décroissant
                .sortedWith(compareBy({ it.bucket.ordinal }, { -it.recommandeCents }))

            // ─── Étape 4 : Objectif d'épargne cible ─────────────────────────────
            val tauxCible        = prefs.tauxEpargneCiblePct
            val objectifEpargne  = revenuMoyen * tauxCible / 100

            // ─── Étape 5 : Viabilité du plan ─────────────────────────────────────
            // Si soldePrevisionnel est négatif, les poches dépassent ce que le revenu permet
            val totalPoches       = poches.sumOf { it.recommandeCents }
            val soldePrevisionnel = revenuMoyen - engagements - totalPoches

            SpendingRecommendation(
                currency              = devise,
                revenuMoyenCents      = revenuMoyen,
                engagementsMensuels   = engagements,
                tauxEpargneActuelPct  = tauxEpargneActuelPct,
                tauxEpargneCiblePct   = tauxCible,
                objectifEpargneCents  = objectifEpargne,
                pouchesRecommandees   = poches,
                soldePrevisionelCents = soldePrevisionnel,
                estEquilibre          = soldePrevisionnel >= 0,
                moisDeReference       = moisAnalyses
            )
        }
    }

    /**
     * Arrondit un montant en centimes au 10 € supérieur le plus proche.
     * Exemple : 4 730 centimes (47,30 €) → 5 000 centimes (50 €).
     * Cette marge évite une recommandation trop serrée par rapport aux dépenses réelles.
     */
    private fun arrondiAu10EurosSuperieur(cents: Long): Long {
        val cran = 1_000L // 10 € en centimes
        return if (cents % cran == 0L) cents else (cents / cran + 1) * cran
    }
}

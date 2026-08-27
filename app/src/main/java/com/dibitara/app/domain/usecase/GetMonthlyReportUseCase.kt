package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CategoryExpense
import com.dibitara.app.domain.model.CategoryVariation
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.model.MonthlyReport
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Construit le rapport mensuel en combinant 4 sources de données :
 *  - Transactions du mois courant (revenus, dépenses, catégories)
 *  - Transactions du mois précédent (pour calculer la variation)
 *  - Budget du mois courant
 *  - Sous-catégories personnalisées (pour résoudre les noms par id)
 *
 * Retourne un Flow actif : le rapport se recalcule automatiquement
 * si l'une des sources est modifiée.
 */
class GetMonthlyReportUseCase @Inject constructor(
    private val getMonthlyTransactions      : GetMonthlyTransactionsUseCase,
    private val getMonthlyBudget            : GetMonthlyBudgetUseCase,
    private val getCustomSubCategories      : GetCustomSubCategoriesUseCase,
    private val userPreferencesRepository   : UserPreferencesRepository,
    private val exchangeRateRepository      : ExchangeRateRepository,
    private val identifierVirementsInternes : IdentifierVirementsInternesUseCase
) {
    operator fun invoke(month: Int, year: Int): Flow<MonthlyReport> {
        val datePrecedente = LocalDate.of(year, month, 1).minusMonths(1)

        // Taux + préférences combinés pour la conversion
        val conversionFlow = combine(
            userPreferencesRepository.get(),
            exchangeRateRepository.getRatesFlow()
        ) { prefs, rates -> prefs.deviseParDefaut to rates }

        return combine(
            getMonthlyTransactions(month, year),
            getMonthlyTransactions(datePrecedente.monthValue, datePrecedente.year),
            getMonthlyBudget(month, year),
            getCustomSubCategories(),
            conversionFlow
        ) { currentBrut, previousBrut, budget, customSubCats, (targetCurrency, rates) ->

            // Exclut les virements internes BRED↔TradeRepublic appariés : un déplacement entre
            // comptes suivis n'est ni un revenu ni une dépense réelle (voir IdentifierVirementsInternesUseCase).
            val idsExclusCurrent  = identifierVirementsInternes(currentBrut)
            val idsExclusPrevious = identifierVirementsInternes(previousBrut)
            val current  = currentBrut.filterNot { it.id in idsExclusCurrent }
            val previous = previousBrut.filterNot { it.id in idsExclusPrevious }

            fun Long.cvt(from: Currency) =
                CurrencyConverter.convertCents(this, from, targetCurrency, rates)

            val revenus  = current.filter { it.type == TransactionType.INCOME  }.sumOf { it.amountCents.cvt(it.currency) }
            val depenses = current.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents.cvt(it.currency) }
            val depensesPrecedent = previous.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents.cvt(it.currency) }

            // Pour AUTRE : on éclate par sous-catégorie pour éviter un bloc "Autre" dominant.
            // Clé de regroupement : catégorie normale, ou sous-catégorie si category == AUTRE.
            val topCategories = current
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { tx ->
                    when {
                        tx.category == Category.AUTRE && tx.subCategory != null ->
                            "subcat_${tx.subCategory.name}"
                        tx.category == Category.AUTRE && tx.customSubCategoryId != null ->
                            "custom_${tx.customSubCategoryId}"
                        else -> "cat_${tx.category.name}"
                    }
                }
                .map { (key, transactions) ->
                    val total      = transactions.sumOf { it.amountCents.cvt(it.currency) }
                    val firstTx    = transactions.first()
                    val label = when {
                        key.startsWith("subcat_") ->
                            firstTx.subCategory!!.displayName
                        key.startsWith("custom_") ->
                            customSubCats.find { it.id == firstTx.customSubCategoryId }?.name
                                ?: Category.AUTRE.displayName
                        else -> firstTx.category.displayName
                    }
                    CategoryExpense(
                        category           = firstTx.category,
                        totalCents         = total,
                        pourcentage        = if (depenses > 0) total.toFloat() / depenses * 100f else 0f,
                        displayLabel       = label,
                        nombreTransactions = transactions.size
                    )
                }
                .sortedByDescending { it.totalCents }
                .take(5)

            // Taux d'épargne = (revenus - dépenses) * 100 / revenus (null si aucun revenu)
            val tauxEpargnePct = if (revenus > 0)
                ((revenus - depenses) * 100 / revenus).toInt()
            else null

            // Dépenses du mois courant par catégorie (clé = nom de catégorie)
            val depensesCourantesParCat = current
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { (_, txs) -> txs.sumOf { it.amountCents.cvt(it.currency) } }

            // Dépenses du mois précédent par catégorie
            val depensesPrecedentesParCat = previous
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { (_, txs) -> txs.sumOf { it.amountCents.cvt(it.currency) } }

            // Variation par catégorie - top 5 par variation absolue, catégories du mois courant uniquement
            val variationParCategorie = depensesCourantesParCat
                .map { (cat, currentCents) ->
                    val previousCents = depensesPrecedentesParCat[cat] ?: 0L
                    CategoryVariation(
                        category      = cat,
                        displayLabel  = cat.displayName,
                        currentCents  = currentCents,
                        variationCents = currentCents - previousCents
                    )
                }
                .filter { it.variationCents != 0L }
                .sortedByDescending { kotlin.math.abs(it.variationCents) }
                .take(5)

            MonthlyReport(
                month                   = month,
                year                    = year,
                currency                = targetCurrency,
                revenusCents            = revenus,
                depensesCents           = depenses,
                soldeCents              = revenus - depenses,
                budget                  = budget,
                topCategories           = topCategories,
                variationDepensesCents  = depenses - depensesPrecedent,
                tauxEpargnePct          = tauxEpargnePct,
                variationParCategorie   = variationParCategorie
            )
        }
    }
}

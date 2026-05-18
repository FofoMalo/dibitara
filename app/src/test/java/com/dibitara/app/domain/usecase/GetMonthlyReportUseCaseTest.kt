package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetMonthlyReportUseCaseTest {

    private val getMonthlyTransactions: GetMonthlyTransactionsUseCase = mockk()
    private val getMonthlyBudget: GetMonthlyBudgetUseCase = mockk()
    private val getCustomSubCategories: GetCustomSubCategoriesUseCase = mockk()
    private val useCase = GetMonthlyReportUseCase(getMonthlyTransactions, getMonthlyBudget, getCustomSubCategories)

    @BeforeEach
    fun setUp() {
        every { getCustomSubCategories() } returns flowOf(emptyList())
    }

    private val mois = 5
    private val annee = 2026

    private fun buildTransaction(type: TransactionType, amountCents: Long, category: Category = Category.ALIMENTATION) =
        Transaction(
            amountCents = amountCents, currency = Currency.EUR,
            category = category, type = type,
            date = LocalDate.of(annee, mois, 10)
        )

    @Test
    fun `calcule revenus et dépenses correctement`() = runTest {
        every { getMonthlyTransactions(mois, annee) } returns flowOf(listOf(
            buildTransaction(TransactionType.INCOME,  200_000L),
            buildTransaction(TransactionType.EXPENSE,  80_000L)
        ))
        every { getMonthlyTransactions(4, annee) } returns flowOf(emptyList())
        every { getMonthlyBudget(mois, annee) } returns flowOf(null)

        val rapport = useCase(mois, annee).first()

        assertEquals(200_000L, rapport.revenusCents)
        assertEquals(80_000L,  rapport.depensesCents)
        assertEquals(120_000L, rapport.soldeCents)
    }

    @Test
    fun `les investissements ne sont pas comptés dans les dépenses`() = runTest {
        every { getMonthlyTransactions(mois, annee) } returns flowOf(listOf(
            buildTransaction(TransactionType.EXPENSE,    50_000L),
            buildTransaction(TransactionType.INVESTMENT, 30_000L)
        ))
        every { getMonthlyTransactions(4, annee) } returns flowOf(emptyList())
        every { getMonthlyBudget(mois, annee) } returns flowOf(null)

        val rapport = useCase(mois, annee).first()

        assertEquals(50_000L, rapport.depensesCents)
    }

    @Test
    fun `top catégories triées par montant décroissant et limitées à 5`() = runTest {
        every { getMonthlyTransactions(mois, annee) } returns flowOf(listOf(
            buildTransaction(TransactionType.EXPENSE, 10_000L, Category.TRANSPORT),
            buildTransaction(TransactionType.EXPENSE, 50_000L, Category.LOGEMENT),
            buildTransaction(TransactionType.EXPENSE, 20_000L, Category.ALIMENTATION),
            buildTransaction(TransactionType.EXPENSE,  5_000L, Category.LOISIRS)
        ))
        every { getMonthlyTransactions(4, annee) } returns flowOf(emptyList())
        every { getMonthlyBudget(mois, annee) } returns flowOf(null)

        val rapport = useCase(mois, annee).first()

        // Le UseCase limite à take(5) — les 4 catégories doivent toutes apparaître, triées par montant décroissant
        assertEquals(4, rapport.topCategories.size)
        assertEquals(Category.LOGEMENT,      rapport.topCategories[0].category)
        assertEquals(Category.ALIMENTATION,  rapport.topCategories[1].category)
        assertEquals(Category.TRANSPORT,     rapport.topCategories[2].category)
        assertEquals(Category.LOISIRS,       rapport.topCategories[3].category)
    }

    @Test
    fun `variation est positive quand les dépenses augmentent`() = runTest {
        every { getMonthlyTransactions(mois, annee) } returns flowOf(listOf(
            buildTransaction(TransactionType.EXPENSE, 80_000L)
        ))
        every { getMonthlyTransactions(4, annee) } returns flowOf(listOf(
            buildTransaction(TransactionType.EXPENSE, 50_000L)
        ))
        every { getMonthlyBudget(mois, annee) } returns flowOf(null)

        val rapport = useCase(mois, annee).first()

        assertEquals(30_000L, rapport.variationDepensesCents)
    }

    @Test
    fun `variation est négative quand les dépenses diminuent`() = runTest {
        every { getMonthlyTransactions(mois, annee) } returns flowOf(listOf(
            buildTransaction(TransactionType.EXPENSE, 40_000L)
        ))
        every { getMonthlyTransactions(4, annee) } returns flowOf(listOf(
            buildTransaction(TransactionType.EXPENSE, 60_000L)
        ))
        every { getMonthlyBudget(mois, annee) } returns flowOf(null)

        val rapport = useCase(mois, annee).first()

        assertEquals(-20_000L, rapport.variationDepensesCents)
    }

    @Test
    fun `le budget est inclus dans le rapport quand il existe`() = runTest {
        val budget = Budget(month = mois, year = annee,
            allocatedCents = 100_000L, spentCents = 70_000L, currency = Currency.EUR)
        every { getMonthlyTransactions(mois, annee) } returns flowOf(emptyList())
        every { getMonthlyTransactions(4, annee) } returns flowOf(emptyList())
        every { getMonthlyBudget(mois, annee) } returns flowOf(budget)

        val rapport = useCase(mois, annee).first()

        assertNotNull(rapport.budget)
        assertEquals(100_000L, rapport.budget!!.allocatedCents)
    }

    @Test
    fun `pourcentage catégorie calculé correctement`() = runTest {
        every { getMonthlyTransactions(mois, annee) } returns flowOf(listOf(
            buildTransaction(TransactionType.EXPENSE, 25_000L, Category.ALIMENTATION),
            buildTransaction(TransactionType.EXPENSE, 75_000L, Category.LOGEMENT)
        ))
        every { getMonthlyTransactions(4, annee) } returns flowOf(emptyList())
        every { getMonthlyBudget(mois, annee) } returns flowOf(null)

        val rapport = useCase(mois, annee).first()
        val logement = rapport.topCategories.first { it.category == Category.LOGEMENT }

        assertEquals(75f, logement.pourcentage, 0.1f) // 75 000 / 100 000 * 100 = 75%
    }
}

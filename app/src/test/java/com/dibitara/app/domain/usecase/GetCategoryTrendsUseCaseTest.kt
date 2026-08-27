package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.repository.TransactionRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tests unitaires de GetCategoryTrendsUseCase.
 * Vérifie en particulier la conversion de devise (bug corrigé le 2026-08-05 :
 * l'écran Tendances affichait un symbole EUR codé en dur, sans conversion réelle).
 */
class GetCategoryTrendsUseCaseTest {

    private val transactionRepo : TransactionRepository     = mockk()
    private val prefsRepo       : UserPreferencesRepository = mockk()
    private val exchangeRateRepo: ExchangeRateRepository    = mockk()

    private val useCase = GetCategoryTrendsUseCase(transactionRepo, prefsRepo, exchangeRateRepo)

    private val rates = ExchangeRates(usdParEur = 1.09, xofParEur = 655.957, horodatage = 0L)

    @BeforeEach
    fun setUp() {
        every { prefsRepo.get() }               returns flowOf(UserPreferences())
        every { exchangeRateRepo.getRatesFlow() } returns flowOf(rates)
    }

    @Test
    fun `retourne liste vide sans transactions`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(emptyList())

        val result = useCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `exclut les catégories sans dépense sur les 6 mois`() = runTest {
        val today = LocalDate.now()
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(listOf(
            Transaction(amountCents = 10000L, currency = Currency.EUR,
                category = Category.AUTRE, type = TransactionType.INCOME, date = today)
        ))

        val result = useCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `somme les dépenses EXPENSE d une catégorie sur les 6 mois`() = runTest {
        val today = LocalDate.now()
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(listOf(
            Transaction(amountCents = 5000L, currency = Currency.EUR,
                category = Category.ALIMENTATION, type = TransactionType.EXPENSE, date = today),
            Transaction(amountCents = 3000L, currency = Currency.EUR,
                category = Category.ALIMENTATION, type = TransactionType.EXPENSE, date = today)
        ))

        val result = useCase().first()

        val trend = result.first { it.category == Category.ALIMENTATION }
        assertEquals(8000L, trend.totalSixMoisCents)
        assertEquals(Currency.EUR, trend.currency)
    }

    @Test
    fun `convertit les montants vers la devise par défaut avant sommation`() = runTest {
        // Bug corrigé (2026-08-05) : le total était sommé en centimes bruts, quelle que
        // soit la devise d'origine, et l'écran affichait toujours un symbole EUR figé.
        every { prefsRepo.get() } returns flowOf(UserPreferences(deviseParDefaut = Currency.USD))
        val today = LocalDate.now()
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(listOf(
            Transaction(amountCents = 10000L, currency = Currency.EUR,
                category = Category.TRANSPORT, type = TransactionType.EXPENSE, date = today)
        ))

        val result = useCase().first()

        val trend = result.first { it.category == Category.TRANSPORT }
        assertEquals(Currency.USD, trend.currency)
        assertEquals(
            CurrencyConverter.convertCents(10000L, Currency.EUR, Currency.USD, rates),
            trend.totalSixMoisCents
        )
    }

    @Test
    fun `garde au maximum 8 catégories triées par total décroissant`() = runTest {
        val today = LocalDate.now()
        val categories = Category.entries.take(9)
        val transactions = categories.mapIndexed { index, cat ->
            Transaction(
                amountCents = (index + 1) * 1000L, currency = Currency.EUR,
                category = cat, type = TransactionType.EXPENSE, date = today
            )
        }
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(transactions)

        val result = useCase().first()

        assertEquals(8, result.size)
        assertTrue(result.zipWithNext().all { (a, b) -> a.totalSixMoisCents >= b.totalSixMoisCents })
    }
}

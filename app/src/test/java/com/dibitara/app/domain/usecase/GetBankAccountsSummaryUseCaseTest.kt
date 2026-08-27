package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.repository.BankAccountRepository
import com.dibitara.app.domain.repository.ExchangeRateRepository
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

class GetBankAccountsSummaryUseCaseTest {

    private val bankAccountRepo : BankAccountRepository     = mockk()
    private val prefsRepo       : UserPreferencesRepository = mockk()
    private val exchangeRateRepo: ExchangeRateRepository    = mockk()
    private val useCase = GetBankAccountsSummaryUseCase(bankAccountRepo, prefsRepo, exchangeRateRepo)

    private val rates = ExchangeRates(usdParEur = 1.1, xofParEur = 655.957, horodatage = 0L)

    @BeforeEach
    fun setUp() {
        every { prefsRepo.get() } returns flowOf(UserPreferences())
        every { exchangeRateRepo.getRatesFlow() } returns flowOf(rates)
    }

    private fun compte(cents: Long, currency: Currency) = BankAccount(
        provider = BankProvider.BRED, label = "BRED", currentBalanceCents = cents,
        currency = currency, updatedAt = LocalDate.of(2026, 1, 1)
    )

    @Test
    fun `somme les soldes de même devise sans conversion`() = runTest {
        every { bankAccountRepo.getAll() } returns flowOf(listOf(compte(50_000L, Currency.EUR), compte(30_000L, Currency.EUR)))

        val result = useCase().first()

        assertEquals(80_000L, result.totalCents)
        assertEquals(Currency.EUR, result.currency)
    }

    @Test
    fun `convertit les comptes en devise étrangère avant de sommer`() = runTest {
        every { bankAccountRepo.getAll() } returns flowOf(listOf(
            compte(100_000L, Currency.EUR),
            compte(110_000L, Currency.USD) // 1100 USD / 1.1 = 1000 EUR = 100 000 centimes
        ))

        val result = useCase().first()

        assertEquals(200_000L, result.totalCents)
    }

    @Test
    fun `liste vide retourne un total de zéro`() = runTest {
        every { bankAccountRepo.getAll() } returns flowOf(emptyList())

        val result = useCase().first()

        assertEquals(0L, result.totalCents)
        assertTrue(result.comptes.isEmpty())
    }
}

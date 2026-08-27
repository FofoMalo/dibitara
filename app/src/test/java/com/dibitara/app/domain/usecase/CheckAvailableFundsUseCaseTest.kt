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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CheckAvailableFundsUseCaseTest {

    private val bankAccountRepo : BankAccountRepository     = mockk()
    private val prefsRepo       : UserPreferencesRepository = mockk()
    private val exchangeRateRepo: ExchangeRateRepository    = mockk()

    private val useCase = CheckAvailableFundsUseCase(bankAccountRepo, prefsRepo, exchangeRateRepo)

    private val rates = ExchangeRates(usdParEur = 1.09, xofParEur = 655.957, horodatage = 0L)

    @BeforeEach
    fun setUp() {
        every { prefsRepo.get() }             returns flowOf(UserPreferences())
        every { exchangeRateRepo.getRatesFlow() } returns flowOf(rates)
    }

    private fun buildBankAccount(provider: BankProvider = BankProvider.BRED, balanceCents: Long, currency: Currency = Currency.EUR) =
        BankAccount(
            provider            = provider,
            label               = provider.displayName,
            currentBalanceCents = balanceCents,
            currency            = currency,
            updatedAt           = LocalDate.of(2026, 8, 1)
        )

    @Test
    fun `retourne la somme des soldes des comptes bancaires`() = runTest {
        every { bankAccountRepo.getAll() } returns flowOf(listOf(
            buildBankAccount(balanceCents = 150_000L),
            buildBankAccount(balanceCents = 30_000L)
        ))

        val solde = useCase()

        assertEquals(180_000L, solde)
    }

    @Test
    fun `retourne 0 sans compte bancaire suivi`() = runTest {
        every { bankAccountRepo.getAll() } returns flowOf(emptyList())

        val solde = useCase()

        assertEquals(0L, solde)
    }

    @Test
    fun `exclut le compte pro Qonto`() = runTest {
        every { bankAccountRepo.getAll() } returns flowOf(listOf(
            buildBankAccount(provider = BankProvider.BRED,  balanceCents = 150_000L),
            buildBankAccount(provider = BankProvider.QONTO, balanceCents = 999_000L)
        ))

        val solde = useCase()

        assertEquals(150_000L, solde)
    }

    @Test
    fun `convertit les soldes dans la devise par défaut`() = runTest {
        every { prefsRepo.get() } returns flowOf(UserPreferences(deviseParDefaut = Currency.XOF))
        every { bankAccountRepo.getAll() } returns flowOf(listOf(
            buildBankAccount(balanceCents = 1_000L, currency = Currency.EUR)
        ))

        val solde = useCase()

        // 1 000 centimes EUR (10€) * 655.957 = 655 957 centimes XOF
        assertEquals(655_957L, solde)
    }
}

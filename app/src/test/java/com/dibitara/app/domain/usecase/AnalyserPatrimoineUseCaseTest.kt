package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.BankAccountRepository
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import com.dibitara.app.domain.repository.DebtRepository
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.repository.InvestmentRepository
import com.dibitara.app.domain.repository.SavingsRepository
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

class AnalyserPatrimoineUseCaseTest {

    private val investmentRepo      : InvestmentRepository       = mockk()
    private val customInvestmentRepo: CustomInvestmentRepository = mockk()
    private val debtRepo            : DebtRepository              = mockk()
    private val savingsRepo         : SavingsRepository           = mockk()
    private val bankAccountRepo     : BankAccountRepository       = mockk()
    private val transactionRepo     : TransactionRepository       = mockk()
    private val prefsRepo           : UserPreferencesRepository   = mockk()
    private val exchangeRateRepo    : ExchangeRateRepository      = mockk()

    private val useCase = AnalyserPatrimoineUseCase(
        getRealEstate               = GetRealEstateUseCase(investmentRepo),
        getScpi                     = GetScpiUseCase(investmentRepo),
        getCustomAssets             = GetCustomAssetsUseCase(customInvestmentRepo),
        getEmployeeSavings          = GetEmployeeSavingsUseCase(customInvestmentRepo),
        getDebts                    = GetDebtsUseCase(debtRepo),
        getSavings                  = GetSavingsUseCase(savingsRepo),
        getBankAccounts             = GetBankAccountsUseCase(bankAccountRepo),
        getMonthlyTransactions      = GetMonthlyTransactionsUseCase(transactionRepo),
        userPreferencesRepo         = prefsRepo,
        exchangeRateRepo            = exchangeRateRepo,
        identifierVirementsInternes = IdentifierVirementsInternesUseCase()
    )

    private val rates = ExchangeRates(usdParEur = 1.1, xofParEur = 655.957, horodatage = 0L)

    @BeforeEach
    fun setUp() {
        every { investmentRepo.getAllRealEstate() }        returns flowOf(emptyList())
        every { investmentRepo.getAllScpi() }               returns flowOf(emptyList())
        every { customInvestmentRepo.getAllCustomAssets() } returns flowOf(emptyList())
        every { customInvestmentRepo.getAllEmployeeSavings() } returns flowOf(emptyList())
        every { debtRepo.getAll() }                         returns flowOf(emptyList())
        every { savingsRepo.getAll() }                       returns flowOf(emptyList())
        every { bankAccountRepo.getAll() }                  returns flowOf(emptyList())
        every { transactionRepo.getByMonth(any(), any()) }  returns flowOf(emptyList())
        every { prefsRepo.get() }                            returns flowOf(UserPreferences())
        every { exchangeRateRepo.getRatesFlow() }            returns flowOf(rates)
    }

    private fun income(amountCents: Long, date: LocalDate = LocalDate.of(2026, 7, 1)) = Transaction(
        amountCents = amountCents, currency = Currency.EUR,
        category = Category.AUTRE, type = TransactionType.INCOME, date = date
    )

    private fun expense(amountCents: Long, category: Category, date: LocalDate = LocalDate.of(2026, 7, 1)) = Transaction(
        amountCents = amountCents, currency = Currency.EUR,
        category = category, type = TransactionType.EXPENSE, date = date
    )

    // refMonth=8, refYear=2026 → 3 mois analysés : juillet, juin, mai 2026

    @Test
    fun `la mensualite du credit d'un bien immobilier n'est pas comptee deux fois dans l'objectif de precaution`() = runTest {
        val maison = RealEstateAsset(
            id = 1L, label = "Maison", currentValueCents = 300_000_00L,
            currency = Currency.EUR, updatedAt = LocalDate.of(2026, 8, 1), debtId = 10L
        )
        every { investmentRepo.getAllRealEstate() } returns flowOf(listOf(maison))
        every { debtRepo.getAll() } returns flowOf(listOf(
            Debt(id = 10L, label = "Crédit maison", totalCents = 200_000_00L, monthlyPaymentCents = 1_200_00L,
                currency = Currency.EUR, type = DebtType.CREDIT_IMMO, updatedAt = LocalDate.of(2026, 8, 1))
        ))
        listOf(7, 6, 5).forEach { mois ->
            every { transactionRepo.getByMonth(mois, 2026) } returns flowOf(listOf(
                expense(1_200_00L, Category.LOGEMENT)
            ))
        }

        val result = useCase(refMonth = 8, refYear = 2026).first()

        // 6 mois × 1200€ (besoins réels, qui incluent déjà la mensualité) = 7200€, pas 13200€
        assertEquals(7_200_00L, result.objectifPrecautionCents)
    }

    @Test
    fun `une dette hors credit immobilier est comptee dans l'objectif de precaution`() = runTest {
        every { debtRepo.getAll() } returns flowOf(listOf(
            Debt(id = 20L, label = "Crédit conso", totalCents = 5_000_00L, monthlyPaymentCents = 300_00L,
                currency = Currency.EUR, type = DebtType.CREDIT_CONSO, updatedAt = LocalDate.of(2026, 8, 1))
        ))
        listOf(7, 6, 5).forEach { mois ->
            every { transactionRepo.getByMonth(mois, 2026) } returns flowOf(listOf(expense(500_00L, Category.ALIMENTATION)))
        }

        val result = useCase(refMonth = 8, refYear = 2026).first()

        // 6 mois × (500€ besoins + 300€ dette conso) = 4800€
        assertEquals(4_800_00L, result.objectifPrecautionCents)
    }

    @Test
    fun `l'immobilier est compte net du credit lie dans la repartition par categorie`() = runTest {
        val maison = RealEstateAsset(
            id = 1L, label = "Maison", currentValueCents = 300_000_00L,
            currency = Currency.EUR, updatedAt = LocalDate.of(2026, 8, 1), debtId = 10L
        )
        every { investmentRepo.getAllRealEstate() } returns flowOf(listOf(maison))
        every { debtRepo.getAll() } returns flowOf(listOf(
            Debt(id = 10L, label = "Crédit maison", totalCents = 200_000_00L, monthlyPaymentCents = 1_200_00L,
                currency = Currency.EUR, type = DebtType.CREDIT_IMMO, updatedAt = LocalDate.of(2026, 8, 1))
        ))

        val result = useCase(refMonth = 8, refYear = 2026).first()

        val immobilier = result.repartitionParCategorie.first { it.categorie == CategoriePatrimoine.IMMOBILIER }
        // 300 000 € valeur - 200 000 € restant dû = 100 000 € net, seule catégorie donc 100%
        assertEquals(100_000_00L, immobilier.montantCents)
        assertEquals(100f, immobilier.pourcentage)
    }

    @Test
    fun `liquidites sures cumule comptes bancaires et savings sûrs, hors comptes non liquides`() = runTest {
        every { bankAccountRepo.getAll() } returns flowOf(listOf(
            BankAccount(id = 1L, provider = BankProvider.BRED, label = "BRED", currentBalanceCents = 50_000L, currency = Currency.EUR, updatedAt = LocalDate.of(2026, 8, 1))
        ))
        every { savingsRepo.getAll() } returns flowOf(listOf(
            SavingsAccount(id = 1L, type = SavingsType.LIVRET_A, label = "Livret A", currentBalanceCents = 100_000L, monthlyContributionCents = 0L, currency = Currency.EUR, updatedAt = LocalDate.of(2026, 8, 1)),
            SavingsAccount(id = 2L, type = SavingsType.PEA, label = "PEA", currentBalanceCents = 500_000L, monthlyContributionCents = 0L, currency = Currency.EUR, updatedAt = LocalDate.of(2026, 8, 1))
        ))

        val result = useCase(refMonth = 8, refYear = 2026).first()

        // 50 000 (BRED) + 100 000 (Livret A) - le PEA n'est pas "sûr et liquide", exclu
        assertEquals(150_000L, result.liquiditesSuresCents)
    }

    @Test
    fun `capacite non affectee est plafonnee par le reste a vivre reel quand il est inferieur a l'objectif 20 pourcent`() = runTest {
        listOf(7, 6, 5).forEach { mois ->
            every { transactionRepo.getByMonth(mois, 2026) } returns flowOf(listOf(
                income(2_000_00L, LocalDate.of(2026, mois, 1)),
                expense(1_900_00L, Category.ALIMENTATION, LocalDate.of(2026, mois, 1))
            ))
        }

        val result = useCase(refMonth = 8, refYear = 2026).first()

        // Reste à vivre réel = 2000 - 1900 = 100€, objectif 20% = 400€ → capacité plafonnée à 100€
        assertEquals(100_00L, result.resteAVivreReelCents)
        assertEquals(400_00L, result.objectifEpargneMensuelCents)
        assertTrue(result.objectifPlafonneParResteAVivre)
        assertEquals(100_00L, result.capaciteNonAffecteeCents)
    }
}

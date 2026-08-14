package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetCashflowProjectionUseCaseTest {

    private val budgetRepo      : BudgetRepository          = mockk()
    private val transactionRepo : TransactionRepository     = mockk()
    private val savingsRepo     : SavingsRepository         = mockk()
    private val investmentRepo  : InvestmentRepository      = mockk()
    private val debtRepo        : DebtRepository            = mockk()
    private val versementRepo   : VersementRepository       = mockk()
    private val prefsRepo       : UserPreferencesRepository = mockk()
    private val exchangeRateRepo: ExchangeRateRepository    = mockk()

    private val useCase = GetCashflowProjectionUseCase(
        budgetRepo, transactionRepo, savingsRepo, investmentRepo, debtRepo, versementRepo, prefsRepo, exchangeRateRepo,
        IdentifierVirementsInternesUseCase()
    )

    private val today = LocalDate.of(2026, 5, 10)

    @BeforeEach
    fun setUp() {
        every { budgetRepo.getBudget(any(), any()) }           returns flowOf(null)
        every { transactionRepo.getRecurring() }               returns flowOf(emptyList())
        every { transactionRepo.getByMonth(any(), any()) }     returns flowOf(emptyList())
        every { savingsRepo.getAll() }                         returns flowOf(emptyList())
        every { investmentRepo.getAllScpi() }                   returns flowOf(emptyList())
        every { debtRepo.getAll() }                            returns flowOf(emptyList())
        every { prefsRepo.get() }                              returns flowOf(UserPreferences(seuilFondsCents = 20_000L))
        every { exchangeRateRepo.getRatesFlow() }              returns flowOf(ExchangeRates(usdParEur = 1.09, xofParEur = 655.957, horodatage = 0L))
        coEvery { versementRepo.existsPourMois(any(), any(), any(), any()) } returns false
    }

    // ─── Solde de départ - calculé depuis les transactions réelles ───────────

    @Test
    fun `solde de départ est zéro sans transactions dans le mois`() = runTest {
        // getByMonth retourne liste vide (setUp)
        val result = useCase(today).first()
        assertEquals(0L, result.soldeActuelCents)
    }

    @Test
    fun `solde de départ reflète les transactions réelles du mois`() = runTest {
        // Correction #1 : le solde vient des transactions, pas de Budget.spentCents (périmé en base)
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(listOf(
            buildTx(TransactionType.INCOME,  300_000L),  // 3 000€ de revenus
            buildTx(TransactionType.EXPENSE, 80_000L)   // 800€ de dépenses
        ))

        val result = useCase(today).first()

        assertEquals(220_000L, result.soldeActuelCents)  // 3 000 - 800 = 2 200€
    }

    @Test
    fun `solde de départ exclut un virement interne BRED vers TradeRepublic apparié`() = runTest {
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(listOf(
            buildTx(TransactionType.INCOME, 300_000L),
            Transaction(
                id = 1, amountCents = 80_000L, currency = Currency.EUR, category = Category.TRANSFERTS,
                type = TransactionType.EXPENSE, date = today, importSource = "bred_csv"
            ),
            Transaction(
                id = 2, amountCents = 80_000L, currency = Currency.EUR, category = Category.TRANSFERTS,
                type = TransactionType.INCOME, date = today.plusDays(1), importSource = "trade_republic"
            )
        ))

        val result = useCase(today).first()

        // Le virement interne appairé (80 000) ne doit pas venir en déduction du solde
        assertEquals(300_000L, result.soldeActuelCents)
    }

    @Test
    fun `solde de départ ignore les templates récurrents du mois`() = runTest {
        // Un template isRecurring=true tombant dans le mois ne doit pas fausser le solde
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(listOf(
            buildTx(TransactionType.INCOME,  200_000L, isRecurring = false),
            buildTx(TransactionType.EXPENSE, 999_000L, isRecurring = true)  // template - exclu
        ))

        val result = useCase(today).first()

        assertEquals(200_000L, result.soldeActuelCents)
    }

    @Test
    fun `spentCents périmé dans le budget n influence pas le solde initial`() = runTest {
        // Budget en base avec spentCents = 50 000 (périmé - dépenses réelles = 80 000)
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(
            Budget(month = 5, year = 2026, allocatedCents = 200_000L, spentCents = 50_000L, currency = Currency.EUR)
        )
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(listOf(
            buildTx(TransactionType.INCOME,  300_000L),
            buildTx(TransactionType.EXPENSE, 80_000L)
        ))

        val result = useCase(today).first()

        // 300 000 - 80 000 = 220 000, pas 200 000 - 50 000 = 150 000
        assertEquals(220_000L, result.soldeActuelCents)
    }

    @Test
    fun `devise suit la préférence par défaut quand aucun budget créé`() = runTest {
        val result = useCase(today).first()
        assertEquals(Currency.EUR, result.currency)
    }

    @Test
    fun `devise suit prefs deviseParDefaut et ignore la devise du budget`() = runTest {
        // Bug corrigé (2026-08-05) : la devise venait de budget.currency au lieu de prefs.deviseParDefaut -
        // un budget en XOF ne doit plus imposer la devise si la préférence globale reste EUR.
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(
            Budget(month = 5, year = 2026, allocatedCents = 0L, spentCents = 0L, currency = Currency.XOF)
        )

        val result = useCase(today).first()

        assertEquals(Currency.EUR, result.currency)
    }

    @Test
    fun `devise et montants suivent prefs deviseParDefaut quand la préférence change`() = runTest {
        every { prefsRepo.get() } returns flowOf(
            UserPreferences(seuilFondsCents = 20_000L, deviseParDefaut = Currency.XOF)
        )
        mockSoldeInitial(1_000L)  // 10€

        val result = useCase(today).first()

        assertEquals(Currency.XOF, result.currency)
        // 1 000 centimes EUR (10€) * 655.957 = 655 957 centimes XOF
        assertEquals(655_957L, result.soldeActuelCents)
    }

    @Test
    fun `un paiement récurrent dans une devise différente est converti avant d être sommé`() = runTest {
        // Bug corrigé (2026-08-05) : aucune conversion n'était appliquée, les montants étaient
        // additionnés en centimes bruts quelle que soit leur devise d'origine.
        mockSoldeInitial(0L)
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(buildRecurrent(amountCents = 10_000L, recurrenceDay = 15).copy(currency = Currency.USD))
        )

        val result = useCase(today).first()

        // 10 000 centimes USD converti en EUR (taux 1.09) = 9174 centimes environ
        val point15Mai = result.pointsTimeline.first { it.date == LocalDate.of(2026, 5, 15) }
        assertEquals(-CurrencyConverter.convertCents(10_000L, Currency.USD, Currency.EUR,
            ExchangeRates(usdParEur = 1.09, xofParEur = 655.957, horodatage = 0L)), point15Mai.soldeCents)

        // L'événement affiché garde lui son montant et sa devise d'origine (USD), non converti
        val evenement = result.evenementsAVenir.first()
        assertEquals(10_000L, evenement.montantCents)
        assertEquals(Currency.USD, evenement.currency)
    }

    // ─── Paiements récurrents EXPENSE ────────────────────────────────────────

    @Test
    fun `paiement mensuel déduit au bon jour dans la timeline`() = runTest {
        mockSoldeInitial(200_000L)
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(buildRecurrent(amountCents = 80_000L, recurrenceDay = 15))
        )

        val result = useCase(today).first()

        val pointsAvant = result.pointsTimeline.filter { it.date < LocalDate.of(2026, 5, 15) }
        assertTrue(pointsAvant.all { it.soldeCents == 200_000L })

        val pointsApres = result.pointsTimeline.filter { it.date >= LocalDate.of(2026, 5, 15) }
        assertTrue(pointsApres.all { it.soldeCents == 120_000L })
    }

    @Test
    fun `paiement récurrent hebdomadaire génère plusieurs occurrences sur 30 jours`() = runTest {
        mockSoldeInitial(100_000L)
        // Paiement hebdomadaire le samedi (today = 2026-05-10 = dimanche → samedis : 16, 23, 30 mai, 6 juin)
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(buildRecurrent(
                amountCents      = 5_000L,
                freq             = RecurrenceFrequency.WEEKLY,
                firstPaymentDate = LocalDate.of(2026, 5, 9)  // samedi
            ))
        )

        val result = useCase(today).first()

        assertEquals(100_000L - 4 * 5_000L, result.soldeProjecte30jCents)
    }

    @Test
    fun `paiement récurrent annuel hors fenêtre 30 jours n est pas déduit`() = runTest {
        mockSoldeInitial(50_000L)
        // Annuel le 1er janvier - hors des 30 prochains jours depuis le 10 mai
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(buildRecurrent(
                amountCents      = 10_000L,
                freq             = RecurrenceFrequency.YEARLY,
                firstPaymentDate = LocalDate.of(2026, 1, 1)
            ))
        )

        val result = useCase(today).first()

        assertEquals(50_000L, result.soldeProjecte30jCents)
    }

    @Test
    fun `paiement récurrent ignoré si sa date de fin est dépassée`() = runTest {
        mockSoldeInitial(50_000L)
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(buildRecurrent(
                amountCents   = 10_000L,
                recurrenceDay = 15,
                endDate       = LocalDate.of(2026, 5, 1)  // fin avant aujourd'hui
            ))
        )

        val result = useCase(today).first()

        assertEquals(50_000L, result.soldeProjecte30jCents)
    }

    // ─── Revenus récurrents INCOME (correction #2) ────────────────────────────

    @Test
    fun `revenu récurrent mensuel augmente le solde projeté`() = runTest {
        // Correction #2 : les INCOME récurrents sont désormais ajoutés (avant ils étaient ignorés)
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(buildRecurrent(amountCents = 200_000L, recurrenceDay = 25, type = TransactionType.INCOME))
        )

        val result = useCase(today).first()

        val point25Mai = result.pointsTimeline.first { it.date == LocalDate.of(2026, 5, 25) }
        assertEquals(200_000L, point25Mai.soldeCents)
    }

    @Test
    fun `revenu et dépense récurrents le même jour se compensent`() = runTest {
        every { transactionRepo.getRecurring() } returns flowOf(listOf(
            buildRecurrent(amountCents = 200_000L, recurrenceDay = 15, type = TransactionType.INCOME),
            buildRecurrent(amountCents =  80_000L, recurrenceDay = 15, type = TransactionType.EXPENSE)
        ))

        val result = useCase(today).first()

        // Solde de départ = 0, le 15 mai : +200 000 − 80 000 = +120 000
        val point15Mai = result.pointsTimeline.first { it.date == LocalDate.of(2026, 5, 15) }
        assertEquals(120_000L, point15Mai.soldeCents)
    }

    @Test
    fun `revenu récurrent hebdomadaire projeté plusieurs fois`() = runTest {
        // today = dimanche 10 mai, horizon = 9 juin
        // Lundis dans [10 mai, 9 juin] : 11, 18, 25 mai, 1 et 8 juin = 5 occurrences
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(buildRecurrent(
                amountCents      = 50_000L,
                freq             = RecurrenceFrequency.WEEKLY,
                firstPaymentDate = LocalDate.of(2026, 5, 11),  // lundi
                type             = TransactionType.INCOME
            ))
        )

        val result = useCase(today).first()

        assertEquals(5 * 50_000L, result.soldeProjecte30jCents)
    }

    @Test
    fun `solde projeté à J+30 intègre revenus et dépenses récurrents`() = runTest {
        // Salaire 2 000€ le 25, loyer 800€ le 15 → net +1 200€
        every { transactionRepo.getRecurring() } returns flowOf(listOf(
            buildRecurrent(amountCents = 200_000L, recurrenceDay = 25, type = TransactionType.INCOME),
            buildRecurrent(amountCents =  80_000L, recurrenceDay = 15, type = TransactionType.EXPENSE)
        ))

        val result = useCase(today).first()

        assertEquals(120_000L, result.soldeProjecte30jCents)
    }

    // ─── Contributions épargne ────────────────────────────────────────────────

    @Test
    fun `contribution épargne non versée est déduite en fin de mois`() = runTest {
        mockSoldeInitial(100_000L)
        every { savingsRepo.getAll() } returns flowOf(
            listOf(buildSavings(monthlyContributionCents = 20_000L))
        )
        coEvery { versementRepo.existsPourMois(any(), any(), 2026, 5) } returns false

        val result = useCase(today).first()

        assertEquals(80_000L, result.soldeProjecte30jCents)
    }

    @Test
    fun `contribution épargne déjà versée ce mois n est pas déduite`() = runTest {
        mockSoldeInitial(100_000L)
        every { savingsRepo.getAll() } returns flowOf(
            listOf(buildSavings(id = 42L, monthlyContributionCents = 20_000L))
        )
        coEvery { versementRepo.existsPourMois(42L, CompteType.EPARGNE, 2026, 5) } returns true

        val result = useCase(today).first()

        assertEquals(100_000L, result.soldeProjecte30jCents)
    }

    @Test
    fun `compte épargne sans contribution mensuelle n est pas pris en compte`() = runTest {
        mockSoldeInitial(100_000L)
        every { savingsRepo.getAll() } returns flowOf(
            listOf(buildSavings(monthlyContributionCents = 0L))
        )

        val result = useCase(today).first()

        assertEquals(100_000L, result.soldeProjecte30jCents)
    }

    // ─── Détection du passage sous seuil ─────────────────────────────────────

    @Test
    fun `jourPassageSeuilNegatif est null quand le solde reste au-dessus du seuil`() = runTest {
        every { prefsRepo.get() } returns flowOf(UserPreferences(seuilFondsCents = 10_000L))
        mockSoldeInitial(200_000L)

        val result = useCase(today).first()

        assertNull(result.jourPassageSeuilNegatif)
    }

    @Test
    fun `jourPassageSeuilNegatif pointe le bon jour quand le seuil est franchi`() = runTest {
        every { prefsRepo.get() } returns flowOf(UserPreferences(seuilFondsCents = 50_000L))
        mockSoldeInitial(80_000L)
        // Loyer de 50 001 le 15 → solde = 80 000 − 50 001 = 29 999, sous le seuil de 50 000
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(buildRecurrent(amountCents = 50_001L, recurrenceDay = 15))
        )

        val result = useCase(today).first()

        assertEquals(LocalDate.of(2026, 5, 15), result.jourPassageSeuilNegatif)
    }

    // ─── Timeline ─────────────────────────────────────────────────────────────

    @Test
    fun `la timeline contient exactement 31 points (J à J+30)`() = runTest {
        val result = useCase(today).first()

        assertEquals(31, result.pointsTimeline.size)
        assertEquals(today, result.pointsTimeline.first().date)
        assertEquals(today.plusDays(30), result.pointsTimeline.last().date)
    }

    // ─── Contributions SCPI (#3) ──────────────────────────────────────────────

    @Test
    fun `contribution SCPI non versée est déduite en fin de mois`() = runTest {
        mockSoldeInitial(100_000L)
        every { investmentRepo.getAllScpi() } returns flowOf(
            listOf(buildScpi(id = 10L, monthlyContributionCents = 30_000L))
        )
        coEvery { versementRepo.existsPourMois(10L, CompteType.SCPI, any(), any()) } returns false

        val result = useCase(today).first()

        assertEquals(70_000L, result.soldeProjecte30jCents)
    }

    @Test
    fun `contribution SCPI déjà versée ce mois n est pas déduite`() = runTest {
        mockSoldeInitial(100_000L)
        every { investmentRepo.getAllScpi() } returns flowOf(
            listOf(buildScpi(id = 10L, monthlyContributionCents = 30_000L))
        )
        coEvery { versementRepo.existsPourMois(10L, CompteType.SCPI, any(), any()) } returns true

        val result = useCase(today).first()

        assertEquals(100_000L, result.soldeProjecte30jCents)
    }

    @Test
    fun `épargne et SCPI non versées sont toutes les deux déduites`() = runTest {
        mockSoldeInitial(200_000L)
        every { savingsRepo.getAll() } returns flowOf(
            listOf(buildSavings(id = 1L, monthlyContributionCents = 20_000L))
        )
        every { investmentRepo.getAllScpi() } returns flowOf(
            listOf(buildScpi(id = 10L, monthlyContributionCents = 30_000L))
        )

        val result = useCase(today).first()

        // 200 000 − 20 000 (épargne) − 30 000 (SCPI) = 150 000
        assertEquals(150_000L, result.soldeProjecte30jCents)
    }

    // ─── Mensualités crédit (#4) ──────────────────────────────────────────────

    @Test
    fun `mensualité crédit déduite en fin du mois courant`() = runTest {
        // today = 10 mai → fin mai = 31 mai (dans la fenêtre 30 jours)
        mockSoldeInitial(150_000L)
        every { debtRepo.getAll() } returns flowOf(
            listOf(buildDebt(monthlyPaymentCents = 80_000L))
        )

        val result = useCase(today).first()

        val point31Mai = result.pointsTimeline.first { it.date == LocalDate.of(2026, 5, 31) }
        assertEquals(150_000L - 80_000L, point31Mai.soldeCents)
    }

    @Test
    fun `mensualité crédit projetée deux fois si la fenêtre enjambe deux mois`() = runTest {
        // today = 10 mai, horizon = 9 juin → fin mai (31/05) ET fin juin tronqué à 9/06
        every { debtRepo.getAll() } returns flowOf(
            listOf(buildDebt(monthlyPaymentCents = 80_000L))
        )

        val result = useCase(today).first()

        // Deux prélèvements : le 31 mai et le 9 juin (= min(30 juin, horizon))
        assertEquals(-2 * 80_000L, result.soldeProjecte30jCents)
    }

    @Test
    fun `dette sans mensualité n est pas prise en compte`() = runTest {
        mockSoldeInitial(50_000L)
        every { debtRepo.getAll() } returns flowOf(
            listOf(buildDebt(monthlyPaymentCents = 0L))
        )

        val result = useCase(today).first()

        assertEquals(50_000L, result.soldeProjecte30jCents)
    }

    // ─── Correction du jour mensuel - plus de plafond fixe 28 (#8) ───────────

    @Test
    fun `prélèvement le 30 reste au 30 dans les mois de 30 jours`() = runTest {
        // today = 10 mai (31 jours), prélèvement le 30 → doit tomber le 30 mai
        val template = buildRecurrent(amountCents = 10_000L, recurrenceDay = 30)
        val from = LocalDate.of(2026, 5, 10)
        val to   = from.plusDays(30)

        val dates = useCase.occurrencesInRange(template, from, to)

        assertTrue(LocalDate.of(2026, 5, 30) in dates)
    }

    @Test
    fun `prélèvement le 31 en février est ramené au 28`() = runTest {
        val template = buildRecurrent(amountCents = 10_000L, recurrenceDay = 31)
        val from = LocalDate.of(2026, 2, 1)
        val to   = from.plusDays(30)

        val dates = useCase.occurrencesInRange(template, from, to)

        // Février 2026 = 28 jours (pas bissextile) → le 31 devient le 28
        assertEquals(listOf(LocalDate.of(2026, 2, 28)), dates)
    }

    // ─── occurrencesInRange ───────────────────────────────────────────────────

    @Nested
    inner class OccurrencesInRangeTest {

        @Test
        fun `mensuel produit une seule occurrence sur 30 jours`() {
            val template = buildRecurrent(recurrenceDay = 20)
            val from = LocalDate.of(2026, 5, 10)
            val to = from.plusDays(30)

            val dates = useCase.occurrencesInRange(template, from, to)

            assertEquals(listOf(LocalDate.of(2026, 5, 20)), dates)
        }

        @Test
        fun `mensuel enjambe deux mois quand from est après le jour cible`() {
            val template = buildRecurrent(recurrenceDay = 5)
            val from = LocalDate.of(2026, 5, 10)   // le 5 est déjà passé
            val to = from.plusDays(30)              // jusqu'au 9 juin

            val dates = useCase.occurrencesInRange(template, from, to)

            assertEquals(listOf(LocalDate.of(2026, 6, 5)), dates)
        }

        @Test
        fun `hebdomadaire produit plusieurs occurrences`() {
            val template = buildRecurrent(
                freq             = RecurrenceFrequency.WEEKLY,
                firstPaymentDate = LocalDate.of(2026, 5, 9)  // samedi
            )
            val from = LocalDate.of(2026, 5, 10)
            val to = from.plusDays(30)

            val dates = useCase.occurrencesInRange(template, from, to)

            // Samedis : 16, 23, 30 mai et 6 juin
            assertEquals(4, dates.size)
            assertTrue(dates.all { it.dayOfWeek.value == 6 })
        }

        @Test
        fun `retourne liste vide quand endDate est avant from`() {
            val template = buildRecurrent(
                recurrenceDay = 15,
                endDate       = LocalDate.of(2026, 5, 1)
            )
            val from = LocalDate.of(2026, 5, 10)
            val to = from.plusDays(30)

            val dates = useCase.occurrencesInRange(template, from, to)

            assertTrue(dates.isEmpty())
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Configure le solde initial via un revenu fictif dans getByMonth.
     * Remplace l'ancien pattern "Budget(allocatedCents = x, spentCents = 0)".
     */
    private fun mockSoldeInitial(cents: Long) {
        every { transactionRepo.getByMonth(any(), any()) } returns flowOf(
            listOf(buildTx(TransactionType.INCOME, cents))
        )
    }

    private fun buildTx(
        type        : TransactionType,
        amountCents : Long,
        isRecurring : Boolean = false
    ) = Transaction(
        amountCents = amountCents,
        currency    = Currency.EUR,
        category    = Category.AUTRE,
        type        = type,
        date        = today,
        isRecurring = isRecurring
    )

    private fun buildRecurrent(
        amountCents      : Long                  = 10_000L,
        recurrenceDay    : Int?                  = null,
        freq             : RecurrenceFrequency   = RecurrenceFrequency.MONTHLY,
        firstPaymentDate : LocalDate?            = null,
        endDate          : LocalDate?            = null,
        type             : TransactionType       = TransactionType.EXPENSE
    ) = Transaction(
        amountCents          = amountCents,
        currency             = Currency.EUR,
        category             = Category.LOGEMENT,
        type                 = type,
        date                 = today.minusMonths(1),
        isRecurring          = true,
        recurrenceDay        = recurrenceDay,
        recurrenceFrequency  = freq,
        firstPaymentDate     = firstPaymentDate,
        endDate              = endDate
    )

    private fun buildScpi(
        id                       : Long = 1L,
        monthlyContributionCents : Long = 0L
    ) = ScpiInvestment(
        id                       = id,
        label                    = "SCPI test",
        sharesCount              = 1.0,
        shareValueCents          = 100_000L,
        monthlyContributionCents = monthlyContributionCents,
        currency                 = Currency.EUR,
        updatedAt                = today
    )

    private fun buildDebt(
        monthlyPaymentCents : Long = 0L
    ) = Debt(
        label               = "Crédit test",
        totalCents          = 10_000_000L,
        monthlyPaymentCents = monthlyPaymentCents,
        currency            = Currency.EUR,
        type                = DebtType.CREDIT_IMMO,
        updatedAt           = today
    )

    private fun buildSavings(
        id                       : Long = 1L,
        monthlyContributionCents : Long = 0L
    ) = SavingsAccount(
        id                       = id,
        type                     = SavingsType.LIVRET_A,
        label                    = "Livret A",
        currentBalanceCents      = 100_000L,
        monthlyContributionCents = monthlyContributionCents,
        currency                 = Currency.EUR,
        updatedAt                = today
    )
}

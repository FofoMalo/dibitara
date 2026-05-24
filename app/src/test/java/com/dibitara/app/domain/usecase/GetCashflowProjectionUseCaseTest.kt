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

    private val budgetRepo: BudgetRepository = mockk()
    private val transactionRepo: TransactionRepository = mockk()
    private val savingsRepo: SavingsRepository = mockk()
    private val versementRepo: VersementRepository = mockk()
    private val prefsRepo: UserPreferencesRepository = mockk()

    private val useCase = GetCashflowProjectionUseCase(
        budgetRepo, transactionRepo, savingsRepo, versementRepo, prefsRepo
    )

    // Date de référence fixe pour tous les tests
    private val today = LocalDate.of(2026, 5, 10)

    @BeforeEach
    fun setUp() {
        // Valeurs par défaut — chaque test peut surcharger
        every { transactionRepo.getRecurring() } returns flowOf(emptyList())
        every { savingsRepo.getAll() } returns flowOf(emptyList())
        every { prefsRepo.get() } returns flowOf(UserPreferences(seuilFondsCents = 20_000L))
        coEvery { versementRepo.existsPourMois(any(), any(), any(), any()) } returns false
    }

    // ─── Solde de départ ─────────────────────────────────────────────────────

    @Test
    fun `solde de départ est zéro quand aucun budget configuré`() = runTest {
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(null)

        val result = useCase(today).first()

        assertEquals(0L, result.soldeActuelCents)
    }

    @Test
    fun `solde de départ correspond au budget restant du mois`() = runTest {
        every { budgetRepo.getBudget(5, 2026) } returns flowOf(
            Budget(month = 5, year = 2026, allocatedCents = 300_000L, spentCents = 80_000L, currency = Currency.EUR)
        )

        val result = useCase(today).first()

        assertEquals(220_000L, result.soldeActuelCents)
    }

    @Test
    fun `devise par défaut EUR quand aucun budget`() = runTest {
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(null)

        val result = useCase(today).first()

        assertEquals(Currency.EUR, result.currency)
    }

    // ─── Paiements récurrents ─────────────────────────────────────────────────

    @Test
    fun `paiement mensuel déduit au bon jour dans la timeline`() = runTest {
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(
            Budget(month = 5, year = 2026, allocatedCents = 200_000L, spentCents = 0L, currency = Currency.EUR)
        )
        // Loyer le 15 de chaque mois
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(buildRecurrent(amountCents = 80_000L, recurrenceDay = 15))
        )

        val result = useCase(today).first()

        // Avant le 15 : solde intact
        val pointsAvant = result.pointsTimeline.filter { it.date < LocalDate.of(2026, 5, 15) }
        assertTrue(pointsAvant.all { it.soldeCents == 200_000L })

        // Dès le 15 : solde diminué de 80 000
        val pointsApres = result.pointsTimeline.filter { it.date >= LocalDate.of(2026, 5, 15) }
        assertTrue(pointsApres.all { it.soldeCents == 120_000L })
    }

    @Test
    fun `paiement récurrent hebdomadaire génère plusieurs occurrences sur 30 jours`() = runTest {
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(
            Budget(month = 5, year = 2026, allocatedCents = 100_000L, spentCents = 0L, currency = Currency.EUR)
        )
        // Paiement hebdomadaire le samedi (today = 2026-05-10 = dimanche → prochain samedi = 16/05)
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(
                buildRecurrent(
                    amountCents = 5_000L,
                    freq = RecurrenceFrequency.WEEKLY,
                    firstPaymentDate = LocalDate.of(2026, 5, 9) // samedi
                )
            )
        )

        val result = useCase(today).first()

        // Sur 30 jours à partir du 10 mai, les samedis sont : 16, 23, 30 mai et 6 juin = 4 occurrences
        val soldeFinal = result.soldeProjecte30jCents
        assertEquals(100_000L - 4 * 5_000L, soldeFinal)
    }

    @Test
    fun `paiement récurrent annuel hors fenêtre 30 jours n est pas déduit`() = runTest {
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(
            Budget(month = 5, year = 2026, allocatedCents = 50_000L, spentCents = 0L, currency = Currency.EUR)
        )
        // Paiement annuel le 1er janvier — hors des 30 prochains jours
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(
                buildRecurrent(
                    amountCents = 10_000L,
                    freq = RecurrenceFrequency.YEARLY,
                    firstPaymentDate = LocalDate.of(2026, 1, 1)
                )
            )
        )

        val result = useCase(today).first()

        assertEquals(50_000L, result.soldeProjecte30jCents)
    }

    @Test
    fun `paiement récurrent ignoré si sa date de fin est dépassée`() = runTest {
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(
            Budget(month = 5, year = 2026, allocatedCents = 50_000L, spentCents = 0L, currency = Currency.EUR)
        )
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(
                buildRecurrent(
                    amountCents = 10_000L,
                    recurrenceDay = 15,
                    endDate = LocalDate.of(2026, 5, 1)  // fin avant aujourd'hui
                )
            )
        )

        val result = useCase(today).first()

        assertEquals(50_000L, result.soldeProjecte30jCents)
    }

    @Test
    fun `seuls les paiements de type EXPENSE sont déduits`() = runTest {
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(
            Budget(month = 5, year = 2026, allocatedCents = 50_000L, spentCents = 0L, currency = Currency.EUR)
        )
        // Un revenu récurrent ne doit pas être déduit
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(
                buildRecurrent(amountCents = 10_000L, recurrenceDay = 15, type = TransactionType.INCOME)
            )
        )

        val result = useCase(today).first()

        assertEquals(50_000L, result.soldeProjecte30jCents)
    }

    // ─── Contributions épargne ────────────────────────────────────────────────

    @Test
    fun `contribution épargne non versée est déduite en fin de mois`() = runTest {
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(
            Budget(month = 5, year = 2026, allocatedCents = 100_000L, spentCents = 0L, currency = Currency.EUR)
        )
        every { savingsRepo.getAll() } returns flowOf(
            listOf(buildSavings(monthlyContributionCents = 20_000L))
        )
        coEvery { versementRepo.existsPourMois(any(), any(), 2026, 5) } returns false

        val result = useCase(today).first()

        // Solde final = 100 000 - 20 000 = 80 000
        assertEquals(80_000L, result.soldeProjecte30jCents)
    }

    @Test
    fun `contribution épargne déjà versée ce mois n est pas déduite`() = runTest {
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(
            Budget(month = 5, year = 2026, allocatedCents = 100_000L, spentCents = 0L, currency = Currency.EUR)
        )
        every { savingsRepo.getAll() } returns flowOf(
            listOf(buildSavings(id = 42L, monthlyContributionCents = 20_000L))
        )
        coEvery { versementRepo.existsPourMois(42L, CompteType.EPARGNE, 2026, 5) } returns true

        val result = useCase(today).first()

        assertEquals(100_000L, result.soldeProjecte30jCents)
    }

    @Test
    fun `compte épargne sans contribution mensuelle n est pas pris en compte`() = runTest {
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(
            Budget(month = 5, year = 2026, allocatedCents = 100_000L, spentCents = 0L, currency = Currency.EUR)
        )
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
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(
            Budget(month = 5, year = 2026, allocatedCents = 200_000L, spentCents = 0L, currency = Currency.EUR)
        )

        val result = useCase(today).first()

        assertNull(result.jourPassageSeuilNegatif)
    }

    @Test
    fun `jourPassageSeuilNegatif pointe le bon jour quand le seuil est franchi`() = runTest {
        every { prefsRepo.get() } returns flowOf(UserPreferences(seuilFondsCents = 50_000L))
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(
            Budget(month = 5, year = 2026, allocatedCents = 80_000L, spentCents = 0L, currency = Currency.EUR)
        )
        // Loyer de 50 001 le 15 → solde = 80 000 - 50 001 = 29 999, en dessous de 50 000
        every { transactionRepo.getRecurring() } returns flowOf(
            listOf(buildRecurrent(amountCents = 50_001L, recurrenceDay = 15))
        )

        val result = useCase(today).first()

        assertEquals(LocalDate.of(2026, 5, 15), result.jourPassageSeuilNegatif)
    }

    // ─── Timeline ─────────────────────────────────────────────────────────────

    @Test
    fun `la timeline contient exactement 31 points (J à J+30)`() = runTest {
        every { budgetRepo.getBudget(any(), any()) } returns flowOf(null)

        val result = useCase(today).first()

        assertEquals(31, result.pointsTimeline.size)
        assertEquals(today, result.pointsTimeline.first().date)
        assertEquals(today.plusDays(30), result.pointsTimeline.last().date)
    }

    // ─── occurrencesInRange (méthode interne exposée pour test) ──────────────

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
                freq = RecurrenceFrequency.WEEKLY,
                firstPaymentDate = LocalDate.of(2026, 5, 9)  // samedi
            )
            val from = LocalDate.of(2026, 5, 10)
            val to = from.plusDays(30)

            val dates = useCase.occurrencesInRange(template, from, to)

            // Samedis du 16/05 au 06/06 : 16, 23, 30 mai et 6 juin
            assertEquals(4, dates.size)
            assertTrue(dates.all { it.dayOfWeek.value == 6 })  // 6 = samedi
        }

        @Test
        fun `retourne liste vide quand endDate est avant from`() {
            val template = buildRecurrent(
                recurrenceDay = 15,
                endDate = LocalDate.of(2026, 5, 1)
            )
            val from = LocalDate.of(2026, 5, 10)
            val to = from.plusDays(30)

            val dates = useCase.occurrencesInRange(template, from, to)

            assertTrue(dates.isEmpty())
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun buildRecurrent(
        amountCents: Long = 10_000L,
        recurrenceDay: Int? = null,
        freq: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        firstPaymentDate: LocalDate? = null,
        endDate: LocalDate? = null,
        type: TransactionType = TransactionType.EXPENSE
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

    private fun buildSavings(
        id: Long = 1L,
        monthlyContributionCents: Long = 0L
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

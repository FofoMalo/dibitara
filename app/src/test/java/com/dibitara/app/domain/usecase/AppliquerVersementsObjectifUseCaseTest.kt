package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CompteType
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.GoalColor
import com.dibitara.app.domain.model.GoalIcon
import com.dibitara.app.domain.model.MonthlyVersement
import com.dibitara.app.domain.model.SavingsGoal
import com.dibitara.app.domain.repository.SavingsGoalRepository
import com.dibitara.app.domain.repository.VersementRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AppliquerVersementsObjectifUseCaseTest {

    private val versementRepo: VersementRepository = mockk()
    private val goalRepo: SavingsGoalRepository = mockk()
    private lateinit var useCase: AppliquerVersementsObjectifUseCase

    private val goal = SavingsGoal(
        id = 7L, name = "Voiture", targetAmountCents = 1_500_000L, currentAmountCents = 420_000L,
        targetDate = LocalDate.of(2027, 6, 1), monthlyContributionCents = 40_000L,
        currency = Currency.EUR, colorKey = GoalColor.TEAL, iconKey = GoalIcon.VOITURE
    )

    @BeforeEach
    fun setUp() {
        useCase = AppliquerVersementsObjectifUseCase(versementRepo, goalRepo)
        coEvery { versementRepo.save(any()) } returns Result.success(1L)
        coEvery { goalRepo.upsert(any()) } just Runs
    }

    @Test
    fun `1 mensualité, mois courant libre - enregistre 1 et avance currentAmountCents`() = runTest {
        coEvery { versementRepo.existsPourMois(7L, CompteType.OBJECTIF, 2026, 8) } returns false
        val goalSlot = slot<SavingsGoal>()
        coEvery { goalRepo.upsert(capture(goalSlot)) } just Runs

        val res = useCase(goal, nbMensualites = 1, aujourdhui = LocalDate.of(2026, 8, 15)).getOrThrow()

        assertEquals(1, res.mensualitesEnregistrees)
        assertEquals(40_000L, res.centimesCredites)
        assertEquals(420_000L + 40_000L, goalSlot.captured.currentAmountCents)
        coVerify(exactly = 1) { versementRepo.save(any()) }
    }

    @Test
    fun `3 mensualités toutes libres - enregistre 3 sur le mois courant et les 2 précédents`() = runTest {
        coEvery { versementRepo.existsPourMois(7L, CompteType.OBJECTIF, any(), any()) } returns false
        val moisSauves = mutableListOf<MonthlyVersement>()
        coEvery { versementRepo.save(capture(moisSauves)) } returns Result.success(1L)

        val res = useCase(goal, nbMensualites = 3, aujourdhui = LocalDate.of(2026, 10, 10)).getOrThrow()

        assertEquals(3, res.mensualitesEnregistrees)
        assertEquals(120_000L, res.centimesCredites)
        assertEquals(
            setOf(2026 to 10, 2026 to 9, 2026 to 8),
            moisSauves.map { it.year to it.month }.toSet()
        )
    }

    @Test
    fun `rattrapage saute les mois déjà couverts`() = runTest {
        // Septembre est déjà enregistré, octobre et août non → « rattraper 3 » en écrit 2
        coEvery { versementRepo.existsPourMois(7L, CompteType.OBJECTIF, 2026, 10) } returns false
        coEvery { versementRepo.existsPourMois(7L, CompteType.OBJECTIF, 2026, 9) } returns true
        coEvery { versementRepo.existsPourMois(7L, CompteType.OBJECTIF, 2026, 8) } returns false

        val res = useCase(goal, nbMensualites = 3, aujourdhui = LocalDate.of(2026, 10, 10)).getOrThrow()

        assertEquals(2, res.mensualitesEnregistrees)
        assertEquals(80_000L, res.centimesCredites)
        coVerify(exactly = 2) { versementRepo.save(any()) }
    }

    @Test
    fun `tous les mois déjà couverts - 0 enregistré, pas d'upsert`() = runTest {
        coEvery { versementRepo.existsPourMois(7L, CompteType.OBJECTIF, any(), any()) } returns true

        val res = useCase(goal, nbMensualites = 2, aujourdhui = LocalDate.of(2026, 10, 10)).getOrThrow()

        assertEquals(0, res.mensualitesEnregistrees)
        assertEquals(0L, res.centimesCredites)
        coVerify(exactly = 0) { versementRepo.save(any()) }
        coVerify(exactly = 0) { goalRepo.upsert(any()) }
    }

    @Test
    fun `objectif sans mensualité - échec`() = runTest {
        val sansPlan = goal.copy(monthlyContributionCents = 0L)
        val res = useCase(sansPlan, nbMensualites = 1, aujourdhui = LocalDate.of(2026, 8, 15))
        assertTrue(res.isFailure)
    }

    @Test
    fun `nbMensualites inférieur à 1 - échec`() = runTest {
        val res = useCase(goal, nbMensualites = 0, aujourdhui = LocalDate.of(2026, 8, 15))
        assertTrue(res.isFailure)
    }
}

package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsType
import com.dibitara.app.domain.repository.ChildRepository
import com.dibitara.app.domain.repository.SavingsRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SavingsUseCasesTest {

    private val savingsRepo: SavingsRepository = mockk()
    private val childRepo: ChildRepository = mockk()

    private fun buildAccount(label: String = "Livret A", balance: Long = 10000L) = SavingsAccount(
        type = SavingsType.LIVRET_A, label = label,
        currentBalanceCents = balance, monthlyContributionCents = 0L,
        currency = Currency.EUR, updatedAt = LocalDate.now()
    )

    // ─── SaveSavingsAccountUseCase ───────────────────────────────────────────

    @Test
    fun `SaveSavingsAccount retourne succès pour un compte valide`() = runTest {
        val account = buildAccount()
        coEvery { savingsRepo.save(account) } returns Result.success(1L)

        val result = SaveSavingsAccountUseCase(savingsRepo)(account)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `SaveSavingsAccount retourne échec si libellé vide`() = runTest {
        val result = SaveSavingsAccountUseCase(savingsRepo)(buildAccount(label = ""))
        assertTrue(result.isFailure)
        assertEquals("Le libellé est requis", result.exceptionOrNull()?.message)
    }

    @Test
    fun `SaveSavingsAccount retourne échec si solde négatif`() = runTest {
        val result = SaveSavingsAccountUseCase(savingsRepo)(buildAccount(balance = -1L))
        assertTrue(result.isFailure)
    }

    // ─── DeleteSavingsAccountUseCase ─────────────────────────────────────────

    @Test
    fun `DeleteSavingsAccount délègue au repository`() = runTest {
        val account = buildAccount()
        coJustRun { savingsRepo.delete(account) }

        DeleteSavingsAccountUseCase(savingsRepo)(account)

        coVerify { savingsRepo.delete(account) }
    }

    // ─── GetSavingsUseCase ───────────────────────────────────────────────────

    @Test
    fun `GetSavings retourne tous les comptes`() {
        every { savingsRepo.getAll() } returns flowOf(emptyList())
        assertNotNull(GetSavingsUseCase(savingsRepo)())
    }

    @Test
    fun `GetSavings byChild retourne les comptes d'un enfant`() {
        every { savingsRepo.getByChild(1L) } returns flowOf(emptyList())
        assertNotNull(GetSavingsUseCase(savingsRepo).byChild(1L))
    }

    // ─── SaveChildUseCase ────────────────────────────────────────────────────

    @Test
    fun `SaveChild retourne succès pour un prénom valide`() = runTest {
        val child = Child(name = "Emma")
        coEvery { childRepo.save(child) } returns Result.success(1L)

        val result = SaveChildUseCase(childRepo)(child)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `SaveChild retourne échec si prénom vide`() = runTest {
        val result = SaveChildUseCase(childRepo)(Child(name = ""))
        assertTrue(result.isFailure)
        assertEquals("Le prénom de l'enfant est requis", result.exceptionOrNull()?.message)
    }

    // ─── DeleteChildUseCase ──────────────────────────────────────────────────

    @Test
    fun `DeleteChild délègue au repository`() = runTest {
        val child = Child(id = 1L, name = "Emma")
        coJustRun { childRepo.delete(child) }

        DeleteChildUseCase(childRepo)(child)

        coVerify { childRepo.delete(child) }
    }

    // ─── GetChildrenUseCase ──────────────────────────────────────────────────

    @Test
    fun `GetChildren retourne le flow du repository`() {
        every { childRepo.getAll() } returns flowOf(emptyList())
        assertNotNull(GetChildrenUseCase(childRepo)())
    }
}

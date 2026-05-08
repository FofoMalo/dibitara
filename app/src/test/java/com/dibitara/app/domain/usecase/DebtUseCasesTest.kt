package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.model.DebtType
import com.dibitara.app.domain.repository.DebtRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DebtUseCasesTest {

    private val repository: DebtRepository = mockk()

    private fun buildDebt(label: String = "Crédit immo", totalCents: Long = 50000L) = Debt(
        label = label, totalCents = totalCents, monthlyPaymentCents = 500L,
        currency = Currency.EUR, type = DebtType.CREDIT_IMMO, updatedAt = LocalDate.now()
    )

    // ─── SaveDebtUseCase ─────────────────────────────────────────────────────

    @Test
    fun `SaveDebt retourne succès quand la dette est valide`() = runTest {
        val debt = buildDebt()
        coEvery { repository.save(debt) } returns Result.success(1L)

        val result = SaveDebtUseCase(repository)(debt)

        assertTrue(result.isSuccess)
        coVerify { repository.save(debt) }
    }

    @Test
    fun `SaveDebt retourne échec si libellé vide`() = runTest {
        val result = SaveDebtUseCase(repository)(buildDebt(label = ""))
        assertTrue(result.isFailure)
        assertEquals("Le libellé est requis", result.exceptionOrNull()?.message)
    }

    @Test
    fun `SaveDebt retourne échec si montant nul`() = runTest {
        val result = SaveDebtUseCase(repository)(buildDebt(totalCents = 0L))
        assertTrue(result.isFailure)
    }

    // ─── DeleteDebtUseCase ───────────────────────────────────────────────────

    @Test
    fun `DeleteDebt délègue au repository`() = runTest {
        val debt = buildDebt()
        coJustRun { repository.delete(debt) }

        DeleteDebtUseCase(repository)(debt)

        coVerify { repository.delete(debt) }
    }

    // ─── GetDebtsUseCase ─────────────────────────────────────────────────────

    @Test
    fun `GetDebts retourne le flow du repository`() {
        every { repository.getAll() } returns flowOf(emptyList())

        val flow = GetDebtsUseCase(repository)()

        assertNotNull(flow)
        verify { repository.getAll() }
    }
}

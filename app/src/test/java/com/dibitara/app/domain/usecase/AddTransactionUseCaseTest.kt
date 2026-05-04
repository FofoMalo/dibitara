package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tests unitaires de AddTransactionUseCase.
 * On utilise MockK pour simuler le repository — aucune base de données réelle.
 * runTest = exécuteur de coroutines pour les tests.
 */
class AddTransactionUseCaseTest {

    // mockk() crée un faux objet qui simule TransactionRepository
    private val repository: TransactionRepository = mockk()
    private lateinit var useCase: AddTransactionUseCase

    @BeforeEach
    fun setUp() {
        useCase = AddTransactionUseCase(repository)
    }

    @Test
    fun `retourne succès quand le montant est valide`() = runTest {
        val transaction = buildTransaction(amountCents = 1250L)
        coEvery { repository.insert(transaction) } returns 1L

        val result = useCase(transaction)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.insert(transaction) }
    }

    @Test
    fun `retourne échec quand le montant est nul ou négatif`() = runTest {
        val transaction = buildTransaction(amountCents = 0L)

        val result = useCase(transaction)

        assertTrue(result.isFailure)
        assertEquals("Le montant doit être supérieur à 0", result.exceptionOrNull()?.message)
    }

    private fun buildTransaction(amountCents: Long) = Transaction(
        amountCents = amountCents,
        currency = Currency.EUR,
        category = Category.ALIMENTATION,
        type = TransactionType.EXPENSE,
        date = LocalDate.now()
    )
}

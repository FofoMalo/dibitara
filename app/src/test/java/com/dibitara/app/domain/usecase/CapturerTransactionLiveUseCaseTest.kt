package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.BankAccountRepository
import com.dibitara.app.domain.repository.ImportRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CapturerTransactionLiveUseCaseTest {

    private val repository: ImportRepository = mockk()
    private val bankAccountRepository: BankAccountRepository = mockk()
    private lateinit var useCase: CapturerTransactionLiveUseCase

    @BeforeEach
    fun setUp() {
        useCase = CapturerTransactionLiveUseCase(repository, bankAccountRepository)
    }

    private fun buildImported() = ImportedTransaction(
        date         = LocalDate.of(2026, 8, 13),
        amountCents  = 3530L,
        currency     = Currency.EUR,
        category     = Category.ALIMENTATION,
        type         = TransactionType.EXPENSE,
        note         = "BAR LES ARCADES",
        externalId   = "bred_notification_xyz",
        rawType      = "PAIEMENT_CARTE_NOTIF",
        importSource = "bred_notification"
    )

    @Test
    fun `insère la transaction rattachée au compte BRED résolu`() = runTest {
        coEvery { repository.externalIdsExistants() } returns emptySet()
        coEvery { repository.importerTransactions(any()) } returns 1
        val compteBred = BankAccount(
            id = 3L, provider = BankProvider.BRED, label = "BRED",
            currentBalanceCents = 0L, currency = Currency.EUR, updatedAt = LocalDate.of(2026, 1, 1)
        )
        coEvery { bankAccountRepository.findByProvider(BankProvider.BRED) } returns compteBred

        val insere = useCase(buildImported())

        assertTrue(insere)
        coVerify {
            repository.importerTransactions(match { list -> list.size == 1 && list[0].bankAccountId == 3L })
        }
    }

    @Test
    fun `retourne false et n'insère rien si l'externalId existe déjà`() = runTest {
        coEvery { repository.externalIdsExistants() } returns setOf("bred_notification_xyz")

        val insere = useCase(buildImported())

        assertFalse(insere)
        coVerify(exactly = 0) { repository.importerTransactions(any()) }
    }

    @Test
    fun `insère sans bankAccountId si aucun compte BRED n'est configuré`() = runTest {
        coEvery { repository.externalIdsExistants() } returns emptySet()
        coEvery { repository.importerTransactions(any()) } returns 1
        coEvery { bankAccountRepository.findByProvider(BankProvider.BRED) } returns null

        useCase(buildImported())

        coVerify {
            repository.importerTransactions(match { list -> list.size == 1 && list[0].bankAccountId == null })
        }
    }
}

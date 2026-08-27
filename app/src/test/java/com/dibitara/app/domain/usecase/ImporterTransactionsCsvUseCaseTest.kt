package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.ImportRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ImporterTransactionsCsvUseCaseTest {

    private val importRepo: ImportRepository = mockk()
    private val prefsRepo: UserPreferencesRepository = mockk(relaxed = true)
    private val useCase = ImporterTransactionsCsvUseCase(importRepo, prefsRepo)

    private var compteurLigne = 0

    private fun imported(
        externalId: String,
        note: String = "Achat",
        amountCents: Long = 1000,
        inclure: Boolean = true,
        alreadyImported: Boolean = false,
    ) = ImportedTransaction(
        date = LocalDate.of(2026, 2, 1),
        amountCents = amountCents,
        currency = Currency.EUR,
        type = TransactionType.EXPENSE,
        note = note,
        category = Category.AUTRE,
        externalId = externalId,
        ligneIndex = compteurLigne++,
        inclure = inclure,
        alreadyImported = alreadyImported,
    )

    // ─── verifierDoublons ────────────────────────────────────────────────────

    @Test
    fun `verifierDoublons marque les transactions déjà en base`() = runTest {
        coEvery { importRepo.externalIdsExistants() } returns setOf("csv_aaa")

        val resultat = useCase.verifierDoublons(listOf(imported("csv_aaa"), imported("csv_bbb")))

        assertTrue(resultat.first { it.externalId == "csv_aaa" }.alreadyImported)
        assertFalse(resultat.first { it.externalId == "csv_bbb" }.alreadyImported)
    }

    // ─── confirmer ───────────────────────────────────────────────────────────

    @Test
    fun `confirmer n'insère que les nouvelles transactions`() = runTest {
        coEvery { importRepo.externalIdsExistants() } returns setOf("csv_deja")
        val capture = slot<List<Transaction>>()
        coEvery { importRepo.importerTransactions(capture(capture)) } answers { capture.captured.size }

        val res = useCase.confirmer(
            listOf(imported("csv_deja"), imported("csv_neuf1"), imported("csv_neuf2")),
            bankAccountId = null,
        ).getOrThrow()

        assertEquals(2, res.importees)
        assertEquals(1, res.doublonsIgnores)
        assertEquals(setOf("csv_neuf1", "csv_neuf2"), capture.captured.map { it.externalId }.toSet())
    }

    @Test
    fun `confirmer ignore les lignes décochées`() = runTest {
        coEvery { importRepo.externalIdsExistants() } returns emptySet()
        coEvery { importRepo.importerTransactions(any()) } answers { firstArg<List<Transaction>>().size }

        val res = useCase.confirmer(
            listOf(imported("csv_1"), imported("csv_2", inclure = false)),
            bankAccountId = null,
        ).getOrThrow()

        assertEquals(1, res.importees)
    }

    @Test
    fun `confirmer déduplique les doublons internes au fichier`() = runTest {
        coEvery { importRepo.externalIdsExistants() } returns emptySet()
        coEvery { importRepo.importerTransactions(any()) } answers { firstArg<List<Transaction>>().size }

        val res = useCase.confirmer(
            listOf(imported("csv_x"), imported("csv_x"), imported("csv_y")),
            bankAccountId = null,
        ).getOrThrow()

        assertEquals(2, res.importees)
    }

    @Test
    fun `confirmer rattache le compte bancaire choisi`() = runTest {
        coEvery { importRepo.externalIdsExistants() } returns emptySet()
        val capture = slot<List<Transaction>>()
        coEvery { importRepo.importerTransactions(capture(capture)) } answers { capture.captured.size }

        useCase.confirmer(listOf(imported("csv_1")), bankAccountId = 7L).getOrThrow()

        assertEquals(7L, capture.captured.single().bankAccountId)
    }

    @Test
    fun `confirmer met à jour la date du dernier import quand au moins une insertion`() = runTest {
        coEvery { importRepo.externalIdsExistants() } returns emptySet()
        coEvery { importRepo.importerTransactions(any()) } answers { firstArg<List<Transaction>>().size }
        coEvery { prefsRepo.updateDerniereImport(any()) } just Runs

        useCase.confirmer(listOf(imported("csv_1")), bankAccountId = null).getOrThrow()

        coVerify(exactly = 1) { prefsRepo.updateDerniereImport(any()) }
    }

    @Test
    fun `confirmer ne touche pas la date si rien n'est inséré`() = runTest {
        coEvery { importRepo.externalIdsExistants() } returns setOf("csv_1")
        coEvery { importRepo.importerTransactions(any()) } returns 0

        useCase.confirmer(listOf(imported("csv_1")), bankAccountId = null).getOrThrow()

        coVerify(exactly = 0) { prefsRepo.updateDerniereImport(any()) }
    }

    @Test
    fun `confirmer renvoie un échec si le repository jette`() = runTest {
        coEvery { importRepo.externalIdsExistants() } returns emptySet()
        coEvery { importRepo.importerTransactions(any()) } throws RuntimeException("db")

        val res = useCase.confirmer(listOf(imported("csv_1")), bankAccountId = null)

        assertTrue(res.isFailure)
    }
}

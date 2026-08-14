package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.BankAccountRepository
import com.dibitara.app.domain.repository.ImportRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tests unitaires de ImportTransactionsUseCase.
 * On vérifie les deux phases séparément : preview (verifierDoublons) et écriture (confirmer).
 */
class ImportTransactionsUseCaseTest {

    private val repository: ImportRepository = mockk()
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxUnitFun = true)
    private val bankAccountRepository: BankAccountRepository = mockk()
    private lateinit var useCase: ImportTransactionsUseCase

    @BeforeEach
    fun setUp() {
        coEvery { bankAccountRepository.findByProvider(any()) } returns null
        useCase = ImportTransactionsUseCase(repository, userPreferencesRepository, bankAccountRepository)
    }

    private fun buildImported(externalId: String, category: Category = Category.ALIMENTATION) =
        ImportedTransaction(
            date        = LocalDate.of(2025, 1, 14),
            amountCents = 2550L,
            currency    = Currency.EUR,
            category    = category,
            type        = TransactionType.EXPENSE,
            note        = "CARREFOUR",
            externalId  = externalId,
            rawType     = "CARD_TRANSACTION"
        )

    // ─── verifierDoublons ─────────────────────────────────────────────────────

    @Test
    fun `verifierDoublons marque alreadyImported pour les externalId existants`() = runTest {
        coEvery { repository.externalIdsExistants() } returns setOf("uuid-001")
        val transactions = listOf(buildImported("uuid-001"), buildImported("uuid-002"))

        val result = useCase.verifierDoublons(transactions)

        assertTrue(result[0].alreadyImported)
        assertFalse(result[1].alreadyImported)
    }

    @Test
    fun `verifierDoublons laisse tout à false quand aucun doublon`() = runTest {
        coEvery { repository.externalIdsExistants() } returns emptySet()
        val transactions = listOf(buildImported("uuid-001"), buildImported("uuid-002"))

        val result = useCase.verifierDoublons(transactions)

        assertTrue(result.none { it.alreadyImported })
    }

    @Test
    fun `verifierDoublons retourne liste vide si entrée vide`() = runTest {
        coEvery { repository.externalIdsExistants() } returns emptySet()

        val result = useCase.verifierDoublons(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `verifierDoublons marque tous en doublon si tous existent déjà`() = runTest {
        coEvery { repository.externalIdsExistants() } returns setOf("uuid-001", "uuid-002")
        val transactions = listOf(buildImported("uuid-001"), buildImported("uuid-002"))

        val result = useCase.verifierDoublons(transactions)

        assertTrue(result.all { it.alreadyImported })
    }

    @Test
    fun `verifierDoublons ne modifie pas les autres champs de la transaction`() = runTest {
        coEvery { repository.externalIdsExistants() } returns setOf("uuid-001")
        val original = buildImported("uuid-001", category = Category.LOISIRS)

        val result = useCase.verifierDoublons(listOf(original))

        assertEquals(Category.LOISIRS, result[0].category)
        assertEquals(2550L, result[0].amountCents)
        assertEquals("CARREFOUR", result[0].note)
    }

    // ─── confirmer ────────────────────────────────────────────────────────────

    @Test
    fun `confirmer insère uniquement les transactions non dupliquées`() = runTest {
        coEvery { repository.externalIdsExistants() } returns setOf("uuid-001")
        coEvery { repository.importerTransactions(any()) } returns 1
        val transactions = listOf(buildImported("uuid-001"), buildImported("uuid-002"))

        val result = useCase.confirmer(transactions)

        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(1, importResult.importees)
        assertEquals(1, importResult.ignorees)
        // Seule uuid-002 doit être insérée
        coVerify {
            repository.importerTransactions(match { list ->
                list.size == 1 && list[0].externalId == "uuid-002"
            })
        }
    }

    @Test
    fun `confirmer retourne 0 importées si toutes sont des doublons`() = runTest {
        coEvery { repository.externalIdsExistants() } returns setOf("uuid-001", "uuid-002")
        coEvery { repository.importerTransactions(emptyList()) } returns 0
        val transactions = listOf(buildImported("uuid-001"), buildImported("uuid-002"))

        val result = useCase.confirmer(transactions)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().importees)
        assertEquals(2, result.getOrThrow().ignorees)
    }

    @Test
    fun `confirmer retourne tous importés si aucun doublon`() = runTest {
        coEvery { repository.externalIdsExistants() } returns emptySet()
        coEvery { repository.importerTransactions(any()) } returns 3
        val transactions = listOf(
            buildImported("uuid-001"),
            buildImported("uuid-002"),
            buildImported("uuid-003")
        )

        val result = useCase.confirmer(transactions)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().importees)
        assertEquals(0, result.getOrThrow().ignorees)
    }

    @Test
    fun `confirmer retourne Result failure si le repository lève une exception`() = runTest {
        coEvery { repository.externalIdsExistants() } throws RuntimeException("Erreur base de données")

        val result = useCase.confirmer(listOf(buildImported("uuid-001")))

        assertTrue(result.isFailure)
        assertEquals("Erreur base de données", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { userPreferencesRepository.updateDerniereImport(any()) }
    }

    @Test
    fun `confirmer enregistre la date du dernier import en cas de succès`() = runTest {
        coEvery { repository.externalIdsExistants() } returns emptySet()
        coEvery { repository.importerTransactions(any()) } returns 1

        useCase.confirmer(listOf(buildImported("uuid-001")))

        coVerify(exactly = 1) { userPreferencesRepository.updateDerniereImport(any()) }
    }

    @Test
    fun `confirmer retourne liste vide importée si entrée vide`() = runTest {
        coEvery { repository.externalIdsExistants() } returns emptySet()
        coEvery { repository.importerTransactions(emptyList()) } returns 0

        val result = useCase.confirmer(emptyList())

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().importees)
        assertEquals(0, result.getOrThrow().ignorees)
    }

    // ─── Réconciliation capture live BRED ↔ import CSV ─────────────────────────

    private fun buildCaptureLive(id: Long = 42L) = Transaction(
        id           = id,
        amountCents  = 2550L,
        currency     = Currency.EUR,
        category     = Category.AUTRE,
        type         = TransactionType.EXPENSE,
        date         = LocalDate.of(2025, 1, 14),
        note         = "BAR LES ARCADES",
        importSource = "bred_notification",
        externalId   = "bred_notification_xyz"
    )

    @Test
    fun `confirmer réconcilie une ligne CSV BRED avec une capture live proche au lieu de dupliquer`() = runTest {
        coEvery { repository.externalIdsExistants() } returns emptySet()
        val captureLive = buildCaptureLive()
        coEvery { repository.trouverCaptureLiveProche(LocalDate.of(2025, 1, 14), 2550L) } returns captureLive
        coEvery { repository.mettreAJour(any()) } returns Unit
        coEvery { repository.importerTransactions(emptyList()) } returns 0

        val ligneCsv = buildImported("bred_csv_001").copy(
            note         = "CARTE BAR LES ARCADES",
            importSource = "bred"
        )

        val result = useCase.confirmer(listOf(ligneCsv))

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().importees)
        assertEquals(1, result.getOrThrow().ignorees)
        coVerify {
            repository.mettreAJour(match { t ->
                t.id == 42L && t.note == "CARTE BAR LES ARCADES" && t.externalId == "bred_csv_001"
            })
        }
        coVerify(exactly = 0) { repository.importerTransactions(match { it.isNotEmpty() }) }
    }

    @Test
    fun `confirmer importe normalement une ligne CSV BRED sans capture live correspondante`() = runTest {
        coEvery { repository.externalIdsExistants() } returns emptySet()
        coEvery { repository.trouverCaptureLiveProche(any(), any()) } returns null
        coEvery { repository.importerTransactions(any()) } returns 1

        val ligneCsv = buildImported("bred_csv_002").copy(importSource = "bred")

        val result = useCase.confirmer(listOf(ligneCsv))

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().importees)
        assertEquals(0, result.getOrThrow().ignorees)
        coVerify(exactly = 0) { repository.mettreAJour(any()) }
    }

    @Test
    fun `confirmer rattache bankAccountId résolu via le provider correspondant à importSource`() = runTest {
        coEvery { repository.externalIdsExistants() } returns emptySet()
        coEvery { repository.trouverCaptureLiveProche(any(), any()) } returns null
        coEvery { repository.importerTransactions(any()) } returns 1
        val compteBred = BankAccount(
            id = 7L, provider = BankProvider.BRED,
            label = "BRED", currentBalanceCents = 0L, currency = Currency.EUR,
            updatedAt = LocalDate.of(2026, 1, 1)
        )
        coEvery { bankAccountRepository.findByProvider(BankProvider.BRED) } returns compteBred

        val ligneCsv = buildImported("bred_csv_003").copy(importSource = "bred")

        useCase.confirmer(listOf(ligneCsv))

        coVerify {
            repository.importerTransactions(match { list -> list.size == 1 && list[0].bankAccountId == 7L })
        }
    }

    @Test
    fun `confirmer n'appelle pas trouverCaptureLiveProche pour les sources autres que bred`() = runTest {
        coEvery { repository.externalIdsExistants() } returns emptySet()
        coEvery { repository.importerTransactions(any()) } returns 1

        useCase.confirmer(listOf(buildImported("uuid-tr-001")))

        coVerify(exactly = 0) { repository.trouverCaptureLiveProche(any(), any()) }
    }
}

package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.SubCategory
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetRecategorizationSuggestionsUseCaseTest {

    private val transactionRepo: TransactionRepository = mockk()
    private val useCase = GetRecategorizationSuggestionsUseCase(transactionRepo)
    private val today = LocalDate.of(2026, 5, 10)

    // ─── Cas sans suggestion ──────────────────────────────────────────────────

    @Test
    fun `retourne liste vide si aucune transaction dans AUTRE`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(
            listOf(buildTransaction(note = "Leclerc", category = Category.ALIMENTATION))
        )

        val result = useCase(today).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `retourne liste vide si transaction AUTRE sans libellé reconnu`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(
            listOf(buildTransaction(note = "xyz inconnu", category = Category.AUTRE))
        )

        val result = useCase(today).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `retourne liste vide si libellé vide`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(
            listOf(buildTransaction(note = "", category = Category.AUTRE))
        )

        val result = useCase(today).first()

        assertTrue(result.isEmpty())
    }

    // ─── Détection par mot-clé ────────────────────────────────────────────────

    @Test
    fun `détecte ALIMENTATION pour libellé Leclerc`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(
            listOf(buildTransaction(note = "Leclerc drive", category = Category.AUTRE))
        )

        val result = useCase(today).first()

        assertEquals(1, result.size)
        assertEquals(Category.ALIMENTATION, result.first().suggestedCategory)
        assertEquals("leclerc", result.first().matchedKeyword)
    }

    @Test
    fun `détecte LOGEMENT pour libellé loyer`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(
            listOf(buildTransaction(note = "Paiement loyer mai", category = Category.AUTRE))
        )

        val result = useCase(today).first()

        assertEquals(Category.LOGEMENT, result.first().suggestedCategory)
    }

    @Test
    fun `détecte ABONNEMENTS pour libellé Netflix`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(
            listOf(buildTransaction(note = "NETFLIX.COM", category = Category.AUTRE))
        )

        val result = useCase(today).first()

        assertEquals(Category.ABONNEMENTS, result.first().suggestedCategory)
    }

    @Test
    fun `détecte TRANSPORT pour libellé SNCF`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(
            listOf(buildTransaction(note = "SNCF billet Paris-Lyon", category = Category.AUTRE))
        )

        val result = useCase(today).first()

        assertEquals(Category.TRANSPORT, result.first().suggestedCategory)
    }

    @Test
    fun `détecte HABILLEMENT pour libellé Zara`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(
            listOf(buildTransaction(note = "Zara online", category = Category.AUTRE))
        )

        val result = useCase(today).first()

        assertEquals(Category.HABILLEMENT, result.first().suggestedCategory)
    }

    // ─── Insensibilité à la casse ─────────────────────────────────────────────

    @Test
    fun `correspondance insensible à la casse`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(
            listOf(buildTransaction(note = "CARREFOUR CITY", category = Category.AUTRE))
        )

        val result = useCase(today).first()

        assertEquals(Category.ALIMENTATION, result.first().suggestedCategory)
    }

    // ─── Plusieurs transactions ───────────────────────────────────────────────

    @Test
    fun `retourne une suggestion par transaction éligible`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(
            listOf(
                buildTransaction(id = 1, note = "Leclerc", category = Category.AUTRE),
                buildTransaction(id = 2, note = "Netflix", category = Category.AUTRE),
                buildTransaction(id = 3, note = "texte inconnu", category = Category.AUTRE),
                buildTransaction(id = 4, note = "SNCF", category = Category.ALIMENTATION) // déjà bien catégorisé
            )
        )

        val result = useCase(today).first()

        assertEquals(2, result.size)
    }

    // ─── Exclusion des transactions déjà statuées ─────────────────────────────

    @Test
    fun `exclut une transaction AUTRE dont subCategory est déjà DIVERS (refus précédent)`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(
            listOf(
                buildTransaction(
                    note        = "Leclerc",
                    category    = Category.AUTRE,
                    subCategory = SubCategory.DIVERS  // refusé lors d'une session précédente
                )
            )
        )

        val result = useCase(today).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `exclut une transaction AUTRE avec toute subCategory non nulle`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(
            listOf(
                buildTransaction(
                    note        = "Netflix",
                    category    = Category.AUTRE,
                    subCategory = SubCategory.FRAIS_BANCAIRES  // sous-catégorie posée manuellement
                )
            )
        )

        val result = useCase(today).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `inclut une transaction AUTRE sans subCategory même si elle a déjà été vue`() = runTest {
        every { transactionRepo.getByDateRange(any(), any()) } returns flowOf(
            listOf(
                buildTransaction(
                    note        = "Spotify",
                    category    = Category.AUTRE,
                    subCategory = null  // pas encore statuée
                )
            )
        )

        val result = useCase(today).first()

        assertEquals(1, result.size)
        assertEquals(Category.ABONNEMENTS, result.first().suggestedCategory)
    }

    @Test
    fun `n analyse que les transactions des 90 derniers jours`() = runTest {
        // Vérifier que getByDateRange est appelé avec today-90 et today
        val debut = today.minusDays(90)
        var appelAvecBonsParams = false
        every { transactionRepo.getByDateRange(debut, today) } answers {
            appelAvecBonsParams = true
            flowOf(emptyList())
        }

        useCase(today).first()

        assertTrue(appelAvecBonsParams, "getByDateRange doit être appelé avec today-90 et today")
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun buildTransaction(
        id          : Long        = 0L,
        note        : String      = "",
        category    : Category    = Category.AUTRE,
        subCategory : SubCategory? = null
    ) = Transaction(
        id          = id,
        amountCents = 5_000L,
        currency    = Currency.EUR,
        category    = category,
        type        = TransactionType.EXPENSE,
        date        = today,
        note        = note,
        subCategory = subCategory
    )
}

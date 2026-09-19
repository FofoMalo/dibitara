package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TradeRepublicReconciliationTest {
    private val rows = mutableListOf<Transaction>()
    private val repo = mockk<ImportRepository>()
    private val banks = mockk<BankAccountRepository>()
    private val prefs = mockk<UserPreferencesRepository>(relaxUnitFun = true)
    private val importer = ImportTransactionsUseCase(repo, prefs, banks)
    private val capturer = CapturerTransactionLiveUseCase(repo, banks)
    private val date = LocalDate.of(2026, 9, 9)

    init {
        coEvery { repo.avecTransaction<Any?>(any()) } coAnswers {
            val avant = rows.toList()
            try { firstArg<suspend () -> Any?>().invoke() } catch (e: Exception) {
                rows.clear(); rows.addAll(avant); throw e
            }
        }
        coEvery { repo.transactionsTradeRepublic() } answers { rows.toList() }
        coEvery { repo.externalIdsExistants() } answers { rows.flatMap { listOfNotNull(it.externalId, it.notificationExternalId) }.toSet() }
        coEvery { banks.findByProvider(any()) } returns null
        coEvery { repo.mettreAJour(any()) } coAnswers {
            val tx = firstArg<Transaction>(); rows[rows.indexOfFirst { it.id == tx.id }] = tx
        }
        coEvery { repo.importerTransactions(any()) } coAnswers {
            val nouveaux = firstArg<List<Transaction>>()
            nouveaux.forEach { rows.add(it.copy(id = (rows.maxOfOrNull { row -> row.id } ?: 0L) + 1)) }
            nouveaux.size
        }
    }

    private fun csv(id: String = "csv1", key: String? = "plan:core msci world usd acc") = ImportedTransaction(
        date, 3500, Currency.EUR, Category.INVESTISSEMENT, TransactionType.EXPENSE,
        "Core MSCI World USD (Acc)", id, "BUY", reconciliationKey = key
    )
    private fun live(id: String = "live1", key: String? = "plan:core msci world usd acc") =
        csv(id, key).copy(importSource = "trade_republic_notification", note = "Plan d’épargne · Core MSCI World USD (Acc)")

    @Test
    fun `capture puis CSV conserve corrections et les deux identifiants puis ignore les rejeux`() = runTest {
        capturer(live())
        rows[0] = rows[0].copy(note = "Mon libellé", category = Category.AUTRE, customSubCategoryId = 9)
        assertTrue(importer.verifierDoublons(listOf(csv())).single().captureLiveReconnue)
        val result = importer.confirmer(listOf(csv())).getOrThrow()
        assertEquals(1, result.reconciliees)
        assertEquals(0, result.importees)
        assertEquals("csv1", rows.single().externalId)
        assertEquals("live1", rows.single().notificationExternalId)
        assertEquals("Mon libellé", rows.single().note)
        assertEquals(9L, rows.single().customSubCategoryId)
        assertFalse(capturer(live()))
        assertEquals(1, importer.confirmer(listOf(csv())).getOrThrow().ignorees)
        assertEquals(1, rows.size)
    }

    @Test
    fun `CSV puis capture puis rejeu conserve une seule ligne`() = runTest {
        importer.confirmer(listOf(csv())).getOrThrow()
        assertFalse(capturer(live()))
        assertFalse(capturer(live()))
        assertEquals(1, rows.size)
        assertEquals("live1", rows.single().notificationExternalId)
        // Une autre notification ne doit pas consommer une ligne CSV déjà liée.
        assertTrue(capturer(live("live2")))
        assertEquals(2, rows.size)
    }

    @Test
    fun `deux captures compatibles bloquent sans écriture`() = runTest {
        capturer(live()); capturer(live("live2"))
        val avant = rows.toList()
        assertTrue(importer.confirmer(listOf(csv())).isFailure)
        assertEquals(avant, rows)
    }

    @Test
    fun `deux CSV distincts ne consomment pas la même capture`() = runTest {
        capturer(live())
        assertTrue(importer.confirmer(listOf(csv(), csv("csv2"))).isFailure)
        assertEquals("live1", rows.single().externalId)
    }

    @Test
    fun `ligne CSV répétée dans un fichier est traitée une fois`() = runTest {
        capturer(live())
        assertEquals(1, importer.confirmer(listOf(csv(), csv())).getOrThrow().reconciliees)
        assertEquals(1, rows.size)
    }

    @Test
    fun `montant seul et achat de nature inconnue ne suffisent pas`() = runTest {
        capturer(live(key = "roundup"))
        assertTrue(importer.confirmer(listOf(csv(key = null))).isFailure)
        assertEquals("live1", rows.single().externalId)
    }

    @Test
    fun `roundup et plan de même montant exigent de vérifier l'origine CSV`() = runTest {
        capturer(live(key = "roundup"))
        assertTrue(importer.confirmer(listOf(csv())).isFailure)
        assertEquals(1, rows.size)
        assertEquals(1, importer.confirmer(listOf(csv("roundup-csv", "roundup"))).getOrThrow().reconciliees)
        assertEquals(1, rows.size)
    }

    @Test
    fun `date devise sens et marchand limitent les correspondances`() {
        val a = live(key = "card:franprix").toTransaction()
        val b = csv(key = "card:franprix").toTransaction()
        assertEquals(1, TradeRepublicReconciliation.candidats(a, listOf(b.copy(date = date.plusDays(1)))).size)
        listOf(b.copy(currency = Currency.USD), b.copy(type = TransactionType.INCOME),
            b.copy(date = date.plusDays(2)), b.copy(reconciliationKey = "card:carrefour"),
            b.copy(amountCents = 3501)).forEach {
            assertTrue(TradeRepublicReconciliation.candidats(a, listOf(it)).isEmpty())
        }
    }

    @Test
    fun `réimport enrichit une ancienne ligne CSV sans changer sa catégorie`() = runTest {
        rows.add(csv().toTransaction().copy(id = 1, reconciliationKey = null, category = Category.AUTRE))
        importer.confirmer(listOf(csv())).getOrThrow()
        assertEquals(csv().reconciliationKey, rows.single().reconciliationKey)
        assertEquals(Category.AUTRE, rows.single().category)
        assertFalse(capturer(live()))
    }
}

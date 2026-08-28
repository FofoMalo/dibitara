package com.dibitara.app.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dibitara.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Tests d'intégration Room pour [TransactionDao].
 * Utilise une base de données en mémoire - aucune donnée persistée entre les tests.
 */
@RunWith(AndroidJUnit4::class)
class TransactionDaoIntegrationTest : RoomIntegrationTestBase() {

    private fun buildEntity(
        amountCents : Long      = 1_000L,
        note        : String    = "Test",
        epochDay    : Long      = LocalDate.of(2026, 5, 1).toEpochDay(),
        isRecurring : Boolean   = false
    ) = TransactionEntity(
        amountCents  = amountCents,
        currency     = "EUR",
        category     = "ALIMENTATION",
        type         = "EXPENSE",
        dateEpochDay = epochDay,
        note         = note,
        isRecurring  = isRecurring
    )

    @Test
    fun `insère_et_récupère_une_transaction`() = runTest {
        val entity = buildEntity(amountCents = 2_500L, note = "Courses")

        val insertedId = db.transactionDao().insert(entity)
        val all = db.transactionDao().getAll().first()

        assertEquals(1, all.size)
        assertEquals(insertedId, all.first().id)
        assertEquals(2_500L, all.first().amountCents)
        assertEquals("Courses", all.first().note)
    }

    @Test
    fun `getByDateRange_retourne_uniquement_les_transactions_dans_la_plage`() = runTest {
        val dao = db.transactionDao()
        val may1  = LocalDate.of(2026, 5, 1).toEpochDay()
        val may15 = LocalDate.of(2026, 5, 15).toEpochDay()
        val jun1  = LocalDate.of(2026, 6, 1).toEpochDay()

        dao.insert(buildEntity(epochDay = may1,  note = "Mai 1"))
        dao.insert(buildEntity(epochDay = may15, note = "Mai 15"))
        dao.insert(buildEntity(epochDay = jun1,  note = "Juin 1"))

        val result = dao.getByDateRange(may1, may15).first()

        assertEquals(2, result.size)
        assertTrue(result.none { it.note == "Juin 1" })
    }

    @Test
    fun `getRecurring_retourne_uniquement_les_templates_isRecurring_true`() = runTest {
        val dao = db.transactionDao()
        dao.insert(buildEntity(isRecurring = true,  note = "Loyer"))
        dao.insert(buildEntity(isRecurring = false, note = "Café"))

        val recurring = dao.getRecurring().first()

        assertEquals(1, recurring.size)
        assertEquals("Loyer", recurring.first().note)
    }

    @Test
    fun `supprime_une_transaction`() = runTest {
        val dao = db.transactionDao()
        val id = dao.insert(buildEntity(note = "À supprimer"))
        val inserted = dao.getAll().first().first { it.id == id }

        dao.delete(inserted)

        val remaining = dao.getAll().first()
        assertTrue(remaining.none { it.id == id })
    }

    @Test
    fun `mise_à_jour_d_une_transaction`() = runTest {
        val dao = db.transactionDao()
        val id = dao.insert(buildEntity(amountCents = 500L, note = "Original"))
        val inserted = dao.getAll().first().first { it.id == id }

        dao.update(inserted.copy(amountCents = 999L, note = "Modifié"))

        val updated = dao.getAll().first().first { it.id == id }
        assertEquals(999L, updated.amountCents)
        assertEquals("Modifié", updated.note)
    }
}

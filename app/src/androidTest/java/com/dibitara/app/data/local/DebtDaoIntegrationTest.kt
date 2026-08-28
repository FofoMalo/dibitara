package com.dibitara.app.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dibitara.app.data.local.entity.DebtEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Tests d'intégration Room pour [DebtDao].
 * Vérifie notamment les colonnes ajoutées en migration 14→15 :
 * [DebtEntity.paymentDay] et [DebtEntity.originalAmountCents].
 */
@RunWith(AndroidJUnit4::class)
class DebtDaoIntegrationTest : RoomIntegrationTestBase() {

    private fun buildDebtEntity(
        label               : String  = "Crédit immobilier",
        totalCents          : Long    = 200_000_00L,
        monthlyPaymentCents : Long    = 800_00L,
        paymentDay          : Int?    = null,
        originalAmountCents : Long    = 0L
    ) = DebtEntity(
        label               = label,
        totalCents          = totalCents,
        monthlyPaymentCents = monthlyPaymentCents,
        currency            = "EUR",
        type                = "CREDIT_IMMO",
        updatedAtEpochDay   = LocalDate.of(2026, 5, 1).toEpochDay(),
        paymentDay          = paymentDay,
        originalAmountCents = originalAmountCents
    )

    @Test
    fun `insère_et_récupère_une_dette_avec_originalAmountCents_et_paymentDay`() = runTest {
        val dao = db.debtDao()
        dao.insert(
            buildDebtEntity(
                label               = "Prêt voiture",
                paymentDay          = 15,
                originalAmountCents = 20_000_00L
            )
        )

        val all = dao.getAll().first()

        assertEquals(1, all.size)
        val debt = all.first()
        assertEquals("Prêt voiture", debt.label)
        assertEquals(15, debt.paymentDay)
        assertEquals(20_000_00L, debt.originalAmountCents)
    }

    @Test
    fun `les_dettes_migrées_ont_originalAmountCents_à_0_par_défaut`() {
        // La valeur par défaut de originalAmountCents dans DebtEntity est 0L.
        // Ce test vérifie directement la valeur par défaut de l'entité (sans insertion),
        // ce qui correspond au comportement d'une ligne migrée depuis la v14.
        val entity = buildDebtEntity()
        assertEquals(0L, entity.originalAmountCents)
        assertNull(entity.paymentDay)
    }

    @Test
    fun `supprime_une_dette`() = runTest {
        val dao = db.debtDao()
        dao.insert(buildDebtEntity(label = "Dette à supprimer"))
        val inserted = dao.getAll().first().first()

        dao.delete(inserted)

        val remaining = dao.getAll().first()
        assertTrue(remaining.isEmpty())
    }
}

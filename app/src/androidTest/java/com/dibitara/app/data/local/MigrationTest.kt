package com.dibitara.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dibitara.app.data.local.database.DibitaraDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Test de migration Room v14 → v15.
 *
 * Vérifie que la migration [DibitaraDatabase.MIGRATION_14_15] ajoute correctement :
 *  - la colonne [paymentDay] (INTEGER nullable)
 *  - la colonne [originalAmountCents] (INTEGER NOT NULL DEFAULT 0)
 * sur la table [debts], sans détruire les lignes existantes.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DibitaraDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migration_14_vers_15_ajoute_colonnes_debts() {
        // 1. Créer la base en version 14 et insérer une ligne sans les nouvelles colonnes
        helper.createDatabase(TEST_DB, 14).use { db ->
            db.execSQL(
                """
                INSERT INTO debts
                    (label, totalCents, monthlyPaymentCents, currency, type, updatedAtEpochDay)
                VALUES
                    ('Prêt test', 100000, 50000, 'EUR', 'CREDIT_IMMO', 19845)
                """.trimIndent()
            )
        }

        // 2. Exécuter la migration 14→15
        helper.runMigrationsAndValidate(
            TEST_DB,
            15,
            true,
            DibitaraDatabase.MIGRATION_14_15
        ).use { db ->
            // 3. Vérifier que la ligne migrée a paymentDay = NULL et originalAmountCents = 0
            val cursor = db.query("SELECT paymentDay, originalAmountCents FROM debts WHERE label = 'Prêt test'")
            cursor.use {
                assertEquals(1, it.count)
                it.moveToFirst()

                val paymentDayIdx = it.getColumnIndex("paymentDay")
                val origAmountIdx = it.getColumnIndex("originalAmountCents")

                // paymentDay doit être NULL après migration
                assertTrue(
                    "paymentDay should be NULL after migration",
                    it.isNull(paymentDayIdx)
                )

                // originalAmountCents doit valoir 0 (DEFAULT 0)
                assertEquals(0L, it.getLong(origAmountIdx))
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}

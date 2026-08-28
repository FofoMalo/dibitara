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

    /**
     * Vérifie que [DibitaraDatabase.MIGRATION_21_22] :
     *  - crée la table [bank_accounts] et y insère les 2 comptes connus (BRED, TradeRepublic)
     *  - ajoute la colonne [bankAccountId] (nullable) sur [transactions]
     *  - rattache automatiquement les transactions existantes à leur compte via [importSource]
     */
    @Test
    @Throws(IOException::class)
    fun migration_21_vers_22_cree_bank_accounts_et_backfill_transactions() {
        helper.createDatabase(TEST_DB, 21).use { db ->
            db.execSQL(
                """
                INSERT INTO transactions
                    (amountCents, currency, category, type, dateEpochDay, note, isRecurring, importSource)
                VALUES
                    (5000, 'EUR', 'ALIMENTATION', 'EXPENSE', 19000, 'Courses BRED', 0, 'bred'),
                    (3000, 'EUR', 'AUTRE', 'INCOME', 19001, 'Virement TR', 0, 'trade_republic'),
                    (1000, 'EUR', 'ALIMENTATION', 'EXPENSE', 19002, 'Saisie manuelle', 0, NULL)
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            22,
            true,
            DibitaraDatabase.MIGRATION_21_22
        ).use { db ->
            // bank_accounts contient les 2 comptes connus
            db.query("SELECT provider FROM bank_accounts ORDER BY provider").use { cursor ->
                assertEquals(2, cursor.count)
                cursor.moveToFirst()
                assertEquals("BRED", cursor.getString(cursor.getColumnIndex("provider")))
                cursor.moveToNext()
                assertEquals("TRADE_REPUBLIC", cursor.getString(cursor.getColumnIndex("provider")))
            }

            // La transaction BRED est rattachée au compte BRED
            db.query("SELECT bankAccountId FROM transactions WHERE note = 'Courses BRED'").use { cursor ->
                cursor.moveToFirst()
                val bankAccountIdIdx = cursor.getColumnIndex("bankAccountId")
                assertTrue("bankAccountId ne doit pas être NULL pour une transaction BRED", !cursor.isNull(bankAccountIdIdx))
            }

            // La transaction TradeRepublic est rattachée au compte TradeRepublic
            db.query("SELECT bankAccountId FROM transactions WHERE note = 'Virement TR'").use { cursor ->
                cursor.moveToFirst()
                val bankAccountIdIdx = cursor.getColumnIndex("bankAccountId")
                assertTrue("bankAccountId ne doit pas être NULL pour une transaction TradeRepublic", !cursor.isNull(bankAccountIdIdx))
            }

            // La transaction saisie manuellement (importSource NULL) reste non rattachée
            db.query("SELECT bankAccountId FROM transactions WHERE note = 'Saisie manuelle'").use { cursor ->
                cursor.moveToFirst()
                val bankAccountIdIdx = cursor.getColumnIndex("bankAccountId")
                assertTrue("bankAccountId doit rester NULL pour une saisie manuelle", cursor.isNull(bankAccountIdIdx))
            }
        }
    }

    /**
     * Chaîne complète : une base créée dans une vieille version du Play Store
     * (v7 = tag `v3.1.0`, v2 = tout début) doit migrer sans erreur jusqu'à la
     * version courante, et le schéma final doit correspondre à `23.json`.
     *
     * Garde-fou pour les mises à jour des utilisateurs existants : `MIGRATION_14_15`
     * et `MIGRATION_21_22` sont testées isolément, mais rien ne validait le
     * chaînage bout-en-bout.
     */
    @Test
    @Throws(IOException::class)
    fun migration_chaine_complete_v7_vers_v23() {
        helper.createDatabase(TEST_DB, 7).close()
        helper.runMigrationsAndValidate(TEST_DB, 23, true, *TOUTES_MIGRATIONS).close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_chaine_complete_v2_vers_v23() {
        helper.createDatabase(TEST_DB, 2).close()
        helper.runMigrationsAndValidate(TEST_DB, 23, true, *TOUTES_MIGRATIONS).close()
    }

    companion object {
        private const val TEST_DB = "migration-test"

        private val TOUTES_MIGRATIONS = arrayOf(
            DibitaraDatabase.MIGRATION_1_2, DibitaraDatabase.MIGRATION_2_3,
            DibitaraDatabase.MIGRATION_3_4, DibitaraDatabase.MIGRATION_4_5,
            DibitaraDatabase.MIGRATION_5_6, DibitaraDatabase.MIGRATION_6_7,
            DibitaraDatabase.MIGRATION_7_8, DibitaraDatabase.MIGRATION_8_9,
            DibitaraDatabase.MIGRATION_9_10, DibitaraDatabase.MIGRATION_10_11,
            DibitaraDatabase.MIGRATION_11_12, DibitaraDatabase.MIGRATION_12_13,
            DibitaraDatabase.MIGRATION_13_14, DibitaraDatabase.MIGRATION_14_15,
            DibitaraDatabase.MIGRATION_15_16, DibitaraDatabase.MIGRATION_16_17,
            DibitaraDatabase.MIGRATION_17_18, DibitaraDatabase.MIGRATION_18_19,
            DibitaraDatabase.MIGRATION_19_20, DibitaraDatabase.MIGRATION_20_21,
            DibitaraDatabase.MIGRATION_21_22, DibitaraDatabase.MIGRATION_22_23,
        )
    }
}

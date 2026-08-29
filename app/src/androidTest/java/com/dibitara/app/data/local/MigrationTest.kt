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
     * Vérifie que [DibitaraDatabase.MIGRATION_23_24] ajoute la colonne [tauxAnnuelPct]
     * (REAL nullable) sur [savings_accounts] sans détruire les lignes existantes ni
     * altérer leur solde.
     */
    @Test
    @Throws(IOException::class)
    fun migration_23_vers_24_ajoute_tauxAnnuelPct_sur_savings_accounts() {
        helper.createDatabase(TEST_DB, 23).use { db ->
            db.execSQL(
                """
                INSERT INTO savings_accounts
                    (type, label, currentBalanceCents, monthlyContributionCents, currency, childId, updatedAtEpochDay)
                VALUES
                    ('LIVRET_A', 'Livret A', 500000, 20000, 'EUR', NULL, 19845)
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            24,
            true,
            DibitaraDatabase.MIGRATION_23_24
        ).use { db ->
            db.query("SELECT currentBalanceCents, tauxAnnuelPct FROM savings_accounts WHERE label = 'Livret A'").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(500000L, cursor.getLong(cursor.getColumnIndex("currentBalanceCents")))
                assertTrue(
                    "tauxAnnuelPct doit être NULL après migration",
                    cursor.isNull(cursor.getColumnIndex("tauxAnnuelPct"))
                )
            }
        }
    }

    /**
     * Vérifie que [DibitaraDatabase.MIGRATION_24_25] crée la table [savings_goals]
     * (objectifs d'épargne) sans toucher aux lignes des tables existantes.
     */
    @Test
    @Throws(IOException::class)
    fun migration_24_vers_25_cree_table_savings_goals() {
        helper.createDatabase(TEST_DB, 24).use { db ->
            // Une ligne quelconque en v24 : elle doit survivre à la migration.
            db.execSQL(
                """
                INSERT INTO savings_accounts
                    (type, label, currentBalanceCents, monthlyContributionCents, currency, childId, updatedAtEpochDay, tauxAnnuelPct)
                VALUES
                    ('LIVRET_A', 'Livret A', 500000, 20000, 'EUR', NULL, 19845, NULL)
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            25,
            true,
            DibitaraDatabase.MIGRATION_24_25
        ).use { db ->
            // La table est créée et vide.
            db.query("SELECT COUNT(*) FROM savings_goals").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            // On peut y insérer un objectif et le relire.
            db.execSQL(
                """
                INSERT INTO savings_goals
                    (name, targetAmountCents, currentAmountCents, targetDateEpochDay,
                     monthlyContributionCents, currency, colorKey, iconKey)
                VALUES
                    ('Voiture', 1500000, 420000, 20970, 40000, 'EUR', 'TEAL', 'VOITURE')
                """.trimIndent()
            )
            db.query("SELECT name, colorKey FROM savings_goals").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("Voiture", cursor.getString(cursor.getColumnIndex("name")))
                assertEquals("TEAL", cursor.getString(cursor.getColumnIndex("colorKey")))
            }
            // La ligne v24 est intacte.
            db.query("SELECT label FROM savings_accounts WHERE label = 'Livret A'").use { cursor ->
                assertEquals(1, cursor.count)
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}

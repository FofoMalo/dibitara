package com.dibitara.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dibitara.app.data.local.database.DibitaraDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.room.Room
import com.dibitara.app.data.repository.ImportRepositoryImpl
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import com.dibitara.app.data.repository.BankAccountRepositoryImpl
import com.dibitara.app.domain.model.ImportedTransaction
import com.dibitara.app.domain.usecase.CapturerTransactionLiveUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

@RunWith(AndroidJUnit4::class)
class TradeRepublicMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), DibitaraDatabase::class.java)

    @Test fun migration26vers27PreserveLaTransaction() {
        val nom = "test-reconciliation-migration"
        helper.createDatabase(nom, 26).use { db ->
            db.execSQL("INSERT INTO transactions (id, amountCents, currency, category, type, dateEpochDay, note, isRecurring, externalId, importSource) VALUES (1, 3500, 'EUR', 'INVESTISSEMENT', 'EXPENSE', 20705, 'Mon plan', 0, 'notification1', 'trade_republic_notification')")
        }
        helper.runMigrationsAndValidate(nom, 27, true, DibitaraDatabase.MIGRATION_26_27).use { db ->
            db.query("SELECT amountCents, note, externalId, notificationExternalId, reconciliationKey FROM transactions WHERE id = 1").use {
                assertTrue(it.moveToFirst())
                assertEquals(3500L, it.getLong(0))
                assertEquals("Mon plan", it.getString(1))
                assertEquals("notification1", it.getString(2))
                assertTrue(it.isNull(3)); assertTrue(it.isNull(4))
            }
        }
    }

    @Test fun aliasPersistesEtRollbackAtomique() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, DibitaraDatabase::class.java).build()
        try {
            val repo = ImportRepositoryImpl(db.transactionDao(), db)
            val tx = Transaction(amountCents = 3500, currency = Currency.EUR,
                category = Category.INVESTISSEMENT, type = TransactionType.EXPENSE,
                date = LocalDate.of(2026, 9, 9), externalId = "csv1", notificationExternalId = "live1",
                reconciliationKey = "roundup", importSource = "trade_republic")
            repo.avecTransaction { repo.importerTransactions(listOf(tx)) }
            assertEquals(setOf("csv1", "live1"), repo.externalIdsExistants())
            assertEquals("roundup", repo.transactionsTradeRepublic().single().reconciliationKey)
            try {
                repo.avecTransaction {
                    repo.mettreAJour(repo.transactionsTradeRepublic().single().copy(note = "À annuler"))
                    repo.importerTransactions(listOf(tx.copy(externalId = "csv2")))
                    error("Échec simulé")
                }
            } catch (_: IllegalStateException) { }
            assertEquals(1, repo.transactionsTradeRepublic().size)
            assertEquals("", repo.transactionsTradeRepublic().single().note)
        } finally { db.close() }
    }

    @Test fun capturesConcurrentesNeCreentPasDeDoublon() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            DibitaraDatabase::class.java).build()
        try {
            val repo = ImportRepositoryImpl(db.transactionDao(), db)
            val capturer = CapturerTransactionLiveUseCase(repo, BankAccountRepositoryImpl(db.bankAccountDao()))
            val tx = ImportedTransaction(LocalDate.of(2026, 9, 9), 4866, Currency.EUR,
                Category.INVESTISSEMENT, TransactionType.EXPENSE, "Roundup investi", "live-concurrent",
                "ROUNDUP_NOTIF", "trade_republic_notification", reconciliationKey = "roundup")
            val resultats = (1..8).map { async { capturer(tx) } }.awaitAll()
            assertEquals(1, resultats.count { it })
            assertEquals(1, repo.transactionsTradeRepublic().size)
        } finally { db.close() }
    }
}

package com.dibitara.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.dibitara.app.data.local.database.DibitaraDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TrashMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), DibitaraDatabase::class.java)
    @Test fun migration27vers28ConserveLesOperations() {
        val name = "migration-trash"
        helper.createDatabase(name, 27).use { db ->
            db.execSQL("INSERT INTO transactions (id, amountCents, currency, category, type, dateEpochDay, note, isRecurring, externalId, notificationExternalId) VALUES (1, 3500, 'EUR', 'AUTRE', 'EXPENSE', 20000, 'Conserver', 0, 'csv1', 'live1')")
        }
        helper.runMigrationsAndValidate(name, 28, true, DibitaraDatabase.MIGRATION_27_28).use { db ->
            db.query("SELECT amountCents, note, externalId, notificationExternalId FROM transactions WHERE id = 1").use {
                assertTrue(it.moveToFirst()); assertEquals(3500, it.getInt(0)); assertEquals("Conserver", it.getString(1))
                assertEquals("csv1", it.getString(2)); assertEquals("live1", it.getString(3))
            }
            db.query("SELECT COUNT(*) FROM transaction_trash").use { assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0)) }
        }
    }
}

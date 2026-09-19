package com.dibitara.app.data.local

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dibitara.app.data.export.CompleteBackup
import com.dibitara.app.data.local.entity.*
import com.dibitara.app.data.repository.*
import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.RestoreResult
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

class SafetyAndTrashTest : BackupIntegrationTestBase() {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun transaction(id: Long = 1) = TransactionEntity.fromDomain(Transaction(id, 1200, Currency.EUR,
        Category.AUTRE, TransactionType.EXPENSE, LocalDate.of(2026, 9, 1), note = "Libellé conservé", externalId = "csv-$id", notificationExternalId = "live-$id"))

    @Test fun corbeilleEtRestaurationConserventTousLesChamps() = runBlocking {
        val entity = transaction()
        db.transactionDao().insert(entity)
        val repo = TransactionActionsRepositoryImpl(db)
        repo.supprimer(1)
        assertNull(db.transactionDao().getById(1))
        assertEquals(1, repo.corbeille().first().size)
        // Nouvelle instance : la corbeille ne dépend pas de la durée de vie du ViewModel.
        TransactionActionsRepositoryImpl(db).restaurer(1)
        assertEquals(entity, db.transactionDao().getById(1))
        assertTrue(repo.corbeille().first().isEmpty())
    }

    @Test fun restaurationRefuseUnIdentifiantExterneReimporte() = runBlocking {
        db.transactionDao().insert(transaction())
        val repo = TransactionActionsRepositoryImpl(db)
        repo.supprimer(1)
        db.transactionDao().insert(transaction().copy(id = 2))
        assertTrue(runCatching { repo.restaurer(1) }.isFailure)
        assertEquals(1, repo.corbeille().first().size)
        assertNotNull(db.transactionDao().getById(2))
    }

    @Test fun categorisationGroupeeEstAtomiqueEtEffaceLesSousCategories() = runBlocking {
        db.transactionDao().insert(transaction().copy(customSubCategoryId = 5))
        db.transactionDao().insert(transaction(2).copy(type = "INCOME"))
        val repo = TransactionActionsRepositoryImpl(db)
        assertTrue(runCatching { repo.categoriser(setOf(1, 2), Category.ALIMENTATION) }.isFailure)
        assertEquals(5L, db.transactionDao().getById(1)!!.customSubCategoryId)
        assertEquals(1, repo.categoriser(setOf(1), Category.ALIMENTATION))
        assertNull(db.transactionDao().getById(1)!!.customSubCategoryId)
        assertEquals("ALIMENTATION", db.transactionDao().getById(1)!!.category)
    }

    @Test fun sauvegardeCompleteRestaureHistoriquesPreferencesEtCorbeille() = runBlocking {
        db.transactionDao().insert(transaction())
        TransactionActionsRepositoryImpl(db).supprimer(1)
        db.patrimoineSnapshotDao().insert(PatrimoineSnapshotEntity(1, 20000, 50000, 40000, "EUR"))
        db.assetValuationSnapshotDao().insert(AssetValuationSnapshotEntity(1, "REAL_ESTATE", 9, 20000, 50000, "EUR"))
        testPreferences.updateDevise(Currency.CAD)
        testPreferences.updateTwoFactorEnabled(true)
        val json = CompleteBackup.generer(db, testPreferences)
        assertFalse(JsonParser.parseString(json).asJsonObject.getAsJsonObject("preferences").has("twoFactorEnabled"))
        val file = File(context.cacheDir, "complete-test.json").apply { writeText(json) }
        testPreferences.updateDevise(Currency.EUR)
        testPreferences.updateTwoFactorEnabled(false)
        db.clearAllTables()
        val result = RestoreRepositoryImpl(context, db, testPreferences).restaurer(Uri.fromFile(file))
        assertTrue(result.toString(), result is RestoreResult.Success)
        assertEquals(1, db.patrimoineSnapshotDao().getAll().first().size)
        assertEquals(1, db.assetValuationSnapshotDao().getAll().size)
        assertEquals(1, db.transactionTrashDao().getAll().first().size)
        assertEquals(Currency.CAD, testPreferences.get().first().deviseParDefaut)
        assertFalse(testPreferences.get().first().twoFactorEnabled)
        assertTrue(File(context.filesDir, "restore-safety").listFiles().orEmpty().isNotEmpty())
    }

    @Test fun jsonQuelconqueNePeutPasEffacerLaBase() = runBlocking {
        db.transactionDao().insert(transaction())
        val file = File(context.cacheDir, "invalid-test.json").apply { writeText("{}") }
        assertTrue(RestoreRepositoryImpl(context, db, testPreferences).restaurer(Uri.fromFile(file)) is RestoreResult.Error)
        assertNotNull(db.transactionDao().getById(1))
    }
}

package com.dibitara.app.data.export

import androidx.room.withTransaction
import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.data.local.entity.*
import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.UserPreferencesRepository
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

class EtfCompleteBackupTest {
    @Test fun `sauvegarde complete embarque ETF avec ids et capital intacts`() = runTest {
        val db = mockk<DibitaraDatabase>(relaxed = true)
        val prefs = mockk<UserPreferencesRepository>()
        val today = LocalDate.now()
        val asset = CustomAssetEntity(7, "CTO", 123_000, "EUR", today.toEpochDay(), 80_000, today.minusDays(100).toEpochDay())
        val plan = EtfPlanEntity(7, "ETF", 2, 100_000, today.minusDays(1).toEpochDay(), 5_000, today.toEpochDay(), "EUR")
        val purchase = EtfPurchaseEntity(9, 7, today.toEpochDay(), 19_900, 100, 42)
        every { prefs.get() } returns flowOf(UserPreferences())
        every { db.childDao().getAll() } returns flowOf(emptyList())
        every { db.transactionDao().getAll() } returns flowOf(emptyList())
        every { db.budgetDao().getAll() } returns flowOf(emptyList())
        every { db.savingsAccountDao().getAll() } returns flowOf(emptyList())
        every { db.realEstateAssetDao().getAll() } returns flowOf(emptyList())
        every { db.scpiInvestmentDao().getAll() } returns flowOf(emptyList())
        every { db.airbnbRentalDao().getAll() } returns flowOf(emptyList())
        every { db.vehicleRentalEntryDao().getAll() } returns flowOf(emptyList())
        every { db.debtDao().getAll() } returns flowOf(emptyList())
        every { db.customAssetDao().getAll() } returns flowOf(listOf(asset))
        every { db.employeeSavingsDao().getAll() } returns flowOf(emptyList())
        every { db.customSubCategoryDao().getAll() } returns flowOf(emptyList())
        every { db.bankAccountDao().getAll() } returns flowOf(emptyList())
        every { db.categoryEnvelopeDao().getAll() } returns flowOf(emptyList())
        every { db.savingsGoalDao().getAll() } returns flowOf(emptyList())
        every { db.patrimoineSnapshotDao().getAll() } returns flowOf(emptyList())
        every { db.transactionTrashDao().getAll() } returns flowOf(emptyList())
        every { db.etfDao().observePlans() } returns flowOf(listOf(plan))
        coEvery { db.etfDao().purchases() } returns listOf(purchase)
        coEvery { db.categorizationRuleDao().getAll() } returns emptyList()
        coEvery { db.monthlyVersementDao().getAll() } returns emptyList()
        coEvery { db.categoryDefinitionDao().all() } returns emptyList()
        coEvery { db.assetValuationSnapshotDao().getAll() } returns emptyList()
        mockkStatic("androidx.room.RoomDatabaseKt")
        try {
            coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
            val json = CompleteBackup.generer(db, prefs)
            assertEquals(3, BackupJsonVerifier.verifier(json))
            val root = JsonParser.parseString(json).asJsonObject
            assertEquals(4, root["backupFormat"].asInt)
            assertEquals(plan, Gson().fromJson(root["etf_plans"].asJsonArray[0], EtfPlanEntity::class.java))
            assertEquals(purchase, Gson().fromJson(root["etf_purchases"].asJsonArray[0], EtfPurchaseEntity::class.java))
            assertEquals(80_000L, root["actifs_libres"].asJsonArray[0].asJsonObject["acquisitionValueCents"].asLong)
        } finally { unmockkStatic("androidx.room.RoomDatabaseKt") }
    }
}

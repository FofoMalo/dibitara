package com.dibitara.app.data.repository

import androidx.room.withTransaction
import com.dibitara.app.data.local.dao.*
import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.data.local.entity.*
import com.dibitara.app.domain.model.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

class EtfRepositoryTest {
    private val db = mockk<DibitaraDatabase>()
    private val dao = mockk<EtfDao>(relaxed = true)
    private val txDao = mockk<TransactionDao>(relaxed = true)
    private val assets = mockk<CustomAssetDao>(relaxed = true)
    private val banks = mockk<BankAccountDao>(relaxed = true)
    private val snapshots = mockk<AssetValuationSnapshotDao>(relaxed = true)
    private val today = LocalDate.now()
    private val plan = EtfPlan(1, "ETF", 2, 100_000, today.minusDays(10), 5_000, today, Currency.EUR)
    private val tx = TransactionEntity(4, 5_000, "EUR", "INVESTISSEMENT", "EXPENSE", today.toEpochDay(), "ETF",
        importSource = "trade_republic", externalId = "csv-4", bankAccountId = 2)
    private val buy = EtfPurchase(assetId = 1, date = today, amountCents = 5_000, transactionId = 4)
    private lateinit var repo: EtfRepositoryImpl

    @BeforeEach fun setup() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
        every { db.etfDao() } returns dao
        every { db.transactionDao() } returns txDao
        every { db.customAssetDao() } returns assets
        every { db.bankAccountDao() } returns banks
        every { db.assetValuationSnapshotDao() } returns snapshots
        coEvery { dao.plan(1) } returns EtfPlanEntity.fromDomain(plan)
        coEvery { dao.purchases() } returns emptyList()
        coEvery { txDao.getById(4) } returns tx
        every { assets.getAll() } returns flowOf(listOf(CustomAssetEntity(1, "CTO", 123_000, "EUR", today.toEpochDay(), 80_000, today.minusDays(100).toEpochDay())))
        every { banks.getAll() } returns flowOf(listOf(BankAccountEntity(2, "TRADE_REPUBLIC", "Trade Republic", 100_000, "EUR", today.toEpochDay())))
        repo = EtfRepositoryImpl(db)
    }
    @AfterEach fun cleanup() { unmockkStatic("androidx.room.RoomDatabaseKt") }

    @Test fun `validation achat ne modifie ni compte ni valeur ni transaction`() = runTest {
        repo.confirmPurchase(buy, false)
        coVerify { dao.insert(match { it.transactionId == 4L && it.amountCents == 5_000L }) }
        coVerify(exactly = 0) { assets.update(any()); banks.upsert(any()); txDao.insert(any()); txDao.update(any()) }
    }
    @Test fun `double validation refuse seconde ecriture`() = runTest {
        coEvery { dao.purchases() } returns listOf(EtfPurchaseEntity.fromDomain(buy.copy(id = 8)))
        assertTrue(runCatching { repo.confirmPurchase(buy, false) }.isFailure)
        coVerify(exactly = 0) { dao.insert(any()) }
    }
    @Test fun `achat manuel puis csv exige association ou achat distinct explicite`() = runTest {
        val manual = EtfPurchaseEntity.fromDomain(buy.copy(id = 8, transactionId = null, feesCents = 100))
        coEvery { dao.purchases() } returns listOf(manual)
        assertTrue(runCatching { repo.confirmPurchase(buy, false) }.isFailure)
        repo.linkPurchase(1, 8, 4)
        coVerify(exactly = 1) { dao.update(manual.copy(transactionId = 4)) }
        coVerify(exactly = 0) { dao.insert(any()) }
    }
    @Test fun `transaction changee ou supprimee refuse validation`() = runTest {
        coEvery { txDao.getById(4) } returns tx.copy(amountCents = 7_000)
        assertTrue(runCatching { repo.confirmPurchase(buy, false) }.isFailure)
        coEvery { txDao.getById(4) } returns null
        assertTrue(runCatching { repo.confirmPurchase(buy, false) }.isFailure)
        coVerify(exactly = 0) { dao.insert(any()) }
    }
    @Test fun `correction reference ne peut absorber achat valide`() = runTest {
        coEvery { dao.purchases() } returns listOf(EtfPurchaseEntity.fromDomain(buy.copy(id = 8)))
        assertTrue(runCatching { repo.savePlan(plan.copy(referenceDate = today)) }.isFailure)
        coVerify(exactly = 0) { dao.savePlan(any()) }
    }
    @Test fun `configurer conserve acquisition valeur et mouvements historiques`() = runTest {
        repo.savePlan(plan)
        coVerify { dao.savePlan(EtfPlanEntity.fromDomain(plan)) }
        coVerify(exactly = 0) { assets.update(any()); txDao.update(any()) }
    }
    @Test fun `actualisation corrige snapshot du jour sans toucher aux achats`() = runTest {
        repo.updateValue(1, 124_000, today)
        coVerify { assets.update(match { it.totalValueCents == 124_000L && it.acquisitionValueCents == 80_000L }) }
        coVerify { snapshots.deleteForDay("CUSTOM_ASSET", 1, today.toEpochDay()); snapshots.insert(match { it.valueCents == 124_000L }) }
        coVerify(exactly = 0) { dao.insert(any()); txDao.update(any()); banks.upsert(any()) }
    }
    @Test fun `retirer un achat conserve operation bancaire`() = runTest {
        repo.removePurchase(1, 8)
        coVerify { dao.delete(1, 8) }
        coVerify(exactly = 0) { txDao.delete(any()); assets.update(any()) }
    }
}

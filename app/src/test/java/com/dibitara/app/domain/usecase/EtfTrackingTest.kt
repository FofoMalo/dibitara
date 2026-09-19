package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.EtfRepository
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EtfTrackingTest {
    private val today = LocalDate.of(2026, 9, 16)
    private val plan = EtfPlan(1, "MSCI World", 2, 100_000, today.minusDays(30), 5_000, today.minusDays(7), Currency.EUR)
    private val asset = CustomAsset(1, "CTO", 123_000, Currency.EUR, today)
    private fun buy(id: Long = 1, date: LocalDate = today, amount: Long = 20_000, fee: Long = 0, tx: Long? = null) =
        EtfPurchase(id, 1, date, amount, fee, tx)
    private fun tx(id: Long = 4, date: LocalDate = today) = Transaction(id, 20_000, Currency.EUR,
        Category.INVESTISSEMENT, TransactionType.EXPENSE, date, "MSCI World", importSource = "trade_republic",
        externalId = "csv-4", bankAccountId = 2, reconciliationKey = "plan:msci world")
    private fun summary(buys: List<EtfPurchase> = emptyList(), txs: List<Transaction> = emptyList(), valuations: List<AssetValuationSnapshot> = emptyList()) =
        EtfTracking.summary(plan, asset, EtfData(listOf(plan), buys, listOf(asset), transactions = txs, valuations = valuations), today)

    @Test fun `gain exclut achats et compte frais connus`() {
        val result = summary(listOf(buy(amount = 19_900, fee = 100)))
        assertEquals(120_000L, result.investedCents)
        assertEquals(3_000L, result.gainCents)
    }
    @Test fun `plusieurs achats le meme mois sont tous comptabilises`() {
        assertEquals(120_000L, summary((1L..4).map { buy(it, today.minusDays(it), 5_000) }).investedCents)
    }
    @Test fun `calendrier ne cree pas achats ni valorisations`() {
        val result = summary()
        assertEquals(100_000L, result.investedCents)
        assertEquals(today, result.nextPlannedDate)
        assertEquals(listOf(EtfPoint(today, 123_000)), result.valueHistory)
        assertEquals(1, result.investedHistory.size)
    }
    @Test fun `plan futur reste futur et echeance passee avance de semaines entieres`() {
        val p = plan.copy(nextPurchaseDate = today.minusDays(8))
        assertEquals(today.plusDays(6), EtfTracking.summary(p, asset, EtfData(), today).nextPlannedDate)
        assertEquals(today.plusDays(3), EtfTracking.summary(p.copy(nextPurchaseDate = today.plusDays(3)), asset, EtfData(), today).nextPlannedDate)
    }
    @Test fun `reference inclut tout le jour et interdit achat futur`() {
        assertThrows(IllegalArgumentException::class.java) { EtfTracking.validate(buy(date = plan.referenceDate), plan, today) }
        assertThrows(IllegalArgumentException::class.java) { EtfTracking.validate(buy(date = today.plusDays(1)), plan, today) }
        EtfTracking.validate(buy(date = plan.referenceDate.plusDays(1)), plan, today)
        assertEquals(100_000L, summary(listOf(buy(date = plan.referenceDate), buy(date = today.plusDays(1)))).investedCents)
    }
    @Test fun `validation refuse mauvais actif montants negatifs et depassements`() {
        listOf(buy().copy(assetId = 8), buy(amount = 0), buy(fee = -1)).forEach {
            assertThrows(IllegalArgumentException::class.java) { EtfTracking.validate(it, plan, today) }
        }
        assertThrows(ArithmeticException::class.java) { EtfTracking.validate(buy(amount = Long.MAX_VALUE, fee = 1), plan, today) }
    }
    @Test fun `plan exige capital positif ou nul et reference pas future`() {
        listOf(plan.copy(initialInvestedCents = -1), plan.copy(weeklyAmountCents = 0), plan.copy(name = " "),
            plan.copy(referenceDate = today.plusDays(1)), plan.copy(assetId = 0)).forEach {
            assertThrows(IllegalArgumentException::class.java) { EtfTracking.validate(it, today) }
        }
        EtfTracking.validate(plan.copy(initialInvestedCents = 0), today)
    }
    @Test fun `import valide disparait meme apres rapprochement notification csv`() {
        val notification = tx().copy(importSource = "trade_republic_notification", externalId = "notif-4")
        val csv = tx().copy(notificationExternalId = "notif-4") // Le rapprochement existant conserve id=4.
        assertTrue(summary(listOf(buy(tx = 4)), listOf(notification)).candidates.isEmpty())
        assertTrue(summary(listOf(buy(tx = 4)), listOf(csv)).candidates.isEmpty())
    }
    @Test fun `achat lie a autre actif ne peut etre repropose`() {
        assertTrue(summary(listOf(buy(tx = 4).copy(assetId = 9)), listOf(tx())).candidates.isEmpty())
    }
    @Test fun `rapprochement manuel conserve frais et capital`() {
        val manual = buy(amount = 20_000, fee = 100, date = today.minusDays(1))
        assertEquals(listOf(manual), summary(listOf(manual), listOf(tx())).candidates.single().manualMatches)
        assertEquals(summary(listOf(manual)).investedCents, summary(listOf(manual.copy(transactionId = 4))).investedCents)
        assertTrue(summary(listOf(manual.copy(date = today.minusDays(2))), listOf(tx())).candidates.single().manualMatches.isEmpty())
        assertEquals(listOf(manual), EtfTracking.manualMatches(tx().copy(amountCents = 20_100), listOf(manual), 1))
    }
    @Test fun `libelle inconnu exige selection et nom exact reconnait ETF`() {
        val candidates = summary(txs = listOf(tx(), tx(5).copy(note = "Autre ETF", reconciliationKey = null))).candidates
        assertTrue(candidates.first().matchesName)
        assertFalse(candidates.last().matchesName)
    }
    @Test fun `ignore autre compte devise revenus recurrence et dates invalides`() {
        val base = tx()
        listOf(base.copy(bankAccountId = 3), base.copy(currency = Currency.USD), base.copy(type = TransactionType.INCOME),
            base.copy(category = Category.ALIMENTATION), base.copy(isRecurring = true), base.copy(importSource = "bred"),
            base.copy(importSource = null), base.copy(date = plan.referenceDate), base.copy(date = today.plusDays(1)),
            base.copy(amountCents = 0)).forEach { assertFalse(EtfTracking.eligible(it, plan, today)) }
        assertTrue(EtfTracking.eligible(base, plan, today))
        assertTrue(EtfTracking.eligible(base.copy(type = TransactionType.INVESTMENT), plan, today))
    }
    @Test fun `historique conserve derniers points connus sans inventer cours`() {
        val snapshots = listOf(
            AssetValuationSnapshot(1, AssetValuationType.CUSTOM_ASSET, 1, today.minusDays(2), 110_000, Currency.EUR),
            AssetValuationSnapshot(2, AssetValuationType.CUSTOM_ASSET, 1, today.minusDays(2), 111_000, Currency.EUR),
            AssetValuationSnapshot(3, AssetValuationType.CUSTOM_ASSET, 9, today.minusDays(1), 9_000, Currency.EUR))
        val s = summary(listOf(buy(1, today.minusDays(1), 5_000), buy(2, today.minusDays(1), 5_000)), valuations = snapshots)
        assertEquals(listOf(EtfPoint(today.minusDays(2), 111_000), EtfPoint(today, 123_000)), s.valueHistory)
        assertEquals(listOf(EtfPoint(plan.referenceDate, 100_000), EtfPoint(today.minusDays(1), 110_000)), s.investedHistory)
    }
    @Test fun `valeur ancienne signalee et perte conservee`() {
        val a = asset.copy(totalValueCents = 90_000, updatedAt = today.minusDays(1))
        val s = EtfTracking.summary(plan, a, EtfData(purchases = listOf(buy())), today)
        assertTrue(s.valueIsOlderThanPurchases)
        assertEquals(-30_000L, s.gainCents)
    }
    @Test fun `usecases transmettent commandes et observation`() = runTest {
        val repo = mockk<EtfRepository>(relaxed = true)
        every { repo.observe() } returns flowOf(EtfData(listOf(plan), listOf(buy()), listOf(asset)))
        assertEquals(120_000L, ObserveEtfUseCase(repo)().first().summaries[1]?.investedCents)
        SaveEtfPlanUseCase(repo)(plan)
        ConfirmEtfPurchaseUseCase(repo)(buy(), true)
        LinkEtfPurchaseUseCase(repo)(1, 2, 4)
        RemoveEtfPurchaseUseCase(repo)(1, 2)
        UpdateEtfValueUseCase(repo)(1, 130_000, today)
        coVerify { repo.savePlan(plan); repo.confirmPurchase(buy(), true); repo.linkPurchase(1, 2, 4); repo.removePurchase(1, 2); repo.updateValue(1, 130_000, today) }
    }
}

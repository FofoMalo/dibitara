package com.dibitara.app.domain.model

import java.time.LocalDate
import kotlin.math.abs

/** Le capital de référence inclut tous les achats jusqu'à cette date, jour inclus. */
data class EtfPlan(
    val assetId: Long,
    val name: String,
    val sourceAccountId: Long,
    val initialInvestedCents: Long,
    val referenceDate: LocalDate,
    val weeklyAmountCents: Long,
    val nextPurchaseDate: LocalDate,
    val currency: Currency
)

data class EtfPurchase(
    val id: Long = 0,
    val assetId: Long,
    val date: LocalDate,
    val amountCents: Long,
    val feesCents: Long = 0,
    val transactionId: Long? = null
) {
    val totalCents: Long get() = Math.addExact(amountCents, feesCents)
}

data class EtfData(
    val plans: List<EtfPlan> = emptyList(),
    val purchases: List<EtfPurchase> = emptyList(),
    val assets: List<CustomAsset> = emptyList(),
    val accounts: List<BankAccount> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val valuations: List<AssetValuationSnapshot> = emptyList()
)

data class EtfPoint(val date: LocalDate, val cents: Long)
data class EtfCandidate(val transaction: Transaction, val matchesName: Boolean, val manualMatches: List<EtfPurchase>)
data class EtfSummary(
    val investedCents: Long,
    val gainCents: Long,
    val candidates: List<EtfCandidate>,
    val investedHistory: List<EtfPoint>,
    val valueHistory: List<EtfPoint>,
    val nextPlannedDate: LocalDate,
    val valueIsOlderThanPurchases: Boolean
)

/** Règles communes à l'écran, aux écritures et à la restauration. Aucun cours n'est extrapolé. */
object EtfTracking {
    fun validate(plan: EtfPlan, today: LocalDate = LocalDate.now()) {
        require(plan.assetId > 0 && plan.sourceAccountId > 0 && plan.name.isNotBlank()) { "Nom de l’ETF requis" }
        require(plan.initialInvestedCents >= 0 && plan.weeklyAmountCents > 0) { "Montants invalides" }
        require(plan.referenceDate <= today) { "La référence ne peut pas être dans le futur" }
    }

    fun validate(purchase: EtfPurchase, plan: EtfPlan, today: LocalDate = LocalDate.now()) {
        require(purchase.assetId == plan.assetId) { "ETF incorrect" }
        require(purchase.amountCents > 0 && purchase.feesCents >= 0) { "Montant ou frais invalides" }
        purchase.totalCents // Vérifie aussi le dépassement de capacité avant toute écriture.
        require(purchase.date > plan.referenceDate && purchase.date <= today) {
            "L’achat doit être après la référence et au plus tard aujourd’hui"
        }
    }

    fun eligible(tx: Transaction, plan: EtfPlan, today: LocalDate = LocalDate.now()): Boolean =
        !tx.isRecurring && tx.bankAccountId == plan.sourceAccountId && tx.currency == plan.currency &&
            tx.importSource?.startsWith("trade_republic") == true &&
            tx.category == Category.INVESTISSEMENT && tx.type != TransactionType.INCOME &&
            tx.amountCents > 0 && tx.date > plan.referenceDate && tx.date <= today

    fun manualMatches(tx: Transaction, purchases: List<EtfPurchase>, assetId: Long): List<EtfPurchase> =
        purchases.filter { it.assetId == assetId && it.transactionId == null &&
            abs(it.date.toEpochDay() - tx.date.toEpochDay()) <= 1 &&
            (it.amountCents == tx.amountCents || it.totalCents == tx.amountCents) }

    fun summary(plan: EtfPlan, asset: CustomAsset, data: EtfData, today: LocalDate = LocalDate.now()): EtfSummary {
        val purchases = data.purchases.filter { it.assetId == plan.assetId }.sortedBy { it.date }
        var invested = plan.initialInvestedCents
        val steps = mutableListOf(EtfPoint(plan.referenceDate, invested))
        purchases.groupBy { it.date }.toSortedMap().forEach { (date, rows) ->
            if (date > plan.referenceDate && date <= today) {
                rows.forEach { invested = Math.addExact(invested, it.totalCents) }
                steps += EtfPoint(date, invested)
            }
        }
        val used = data.purchases.mapNotNull { it.transactionId }.toSet()
        val nameKey = TradeRepublicReconciliation.normaliser(plan.name)
        val candidates = data.transactions.filter { eligible(it, plan, today) && it.id !in used }.map { tx ->
            val support = TradeRepublicReconciliation.cleHistorique(tx)?.removePrefix("plan:")
            EtfCandidate(tx, support == nameKey || TradeRepublicReconciliation.normaliser(tx.note) == nameKey,
                manualMatches(tx, purchases, plan.assetId))
        }.sortedWith(compareByDescending<EtfCandidate> { it.matchesName }.thenByDescending { it.transaction.date })
        // La dernière valeur saisie reste un point réel, même si l'historique précédent est vide.
        val values = (data.valuations.filter { it.assetType == AssetValuationType.CUSTOM_ASSET &&
            it.assetId == plan.assetId && it.currency == plan.currency && it.snapshotDate >= plan.referenceDate }
            .sortedBy { it.id }.map { EtfPoint(it.snapshotDate, it.valueCents) } +
            listOf(EtfPoint(asset.updatedAt, asset.totalValueCents)))
            .filter { it.date >= plan.referenceDate && it.date <= today }.associateBy { it.date }.values.sortedBy { it.date }
        val overdueWeeks = if (plan.nextPurchaseDate < today)
            (today.toEpochDay() - plan.nextPurchaseDate.toEpochDay() + 6) / 7 else 0
        return EtfSummary(invested, Math.subtractExact(asset.totalValueCents, invested), candidates, steps, values,
            plan.nextPurchaseDate.plusWeeks(overdueWeeks), purchases.any { it.date > asset.updatedAt })
    }
}

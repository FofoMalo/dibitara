package com.dibitara.app.data.repository

import androidx.room.withTransaction
import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.data.local.entity.*
import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.EtfRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class EtfRepositoryImpl @Inject constructor(private val db: DibitaraDatabase) : EtfRepository {
    override fun observe() = combine(db.etfDao().observePlans(), db.etfDao().observePurchases(),
        db.customAssetDao().getAll(), db.bankAccountDao().getAll(), db.transactionDao().getAll()) { plans, buys, assets, banks, txs ->
        EtfData(plans.map { it.toDomain() }, buys.map { it.toDomain() }, assets.map { it.toDomain() },
            banks.map { it.toDomain() }, txs.map { it.toDomain() })
    }.combine(db.assetValuationSnapshotDao().observeAll()) { data, snapshots ->
        data.copy(valuations = snapshots.map { it.toDomain() })
    }

    override suspend fun savePlan(plan: EtfPlan) = db.withTransaction {
        EtfTracking.validate(plan)
        val asset = requireNotNull(db.customAssetDao().getAll().first().find { it.id == plan.assetId }) { "Actif introuvable" }
        require(asset.currency == plan.currency.name) { "La devise doit rester celle de l’actif" }
        val old = db.etfDao().plan(plan.assetId)?.toDomain()
        // Une correction de référence reste possible, mais ne peut engloutir des achats enregistrés.
        db.etfDao().purchases().filter { it.assetId == plan.assetId }.forEach { EtfTracking.validate(it.toDomain(), plan) }
        val account = db.bankAccountDao().getAll().first().find { it.id == plan.sourceAccountId }
        require((old?.sourceAccountId == plan.sourceAccountId && account == null) ||
            (account?.provider == BankProvider.TRADE_REPUBLIC.name && account.currency == plan.currency.name)) {
            "Choisis un compte Trade Republic dans la devise de l’ETF"
        }
        db.etfDao().purchases().filter { it.assetId == plan.assetId }.fold(plan.initialInvestedCents) { total, row ->
            Math.addExact(total, row.toDomain().totalCents)
        }
        db.etfDao().savePlan(EtfPlanEntity.fromDomain(plan.copy(name = plan.name.trim())))
    }

    override suspend fun confirmPurchase(purchase: EtfPurchase, distinctPurchaseConfirmed: Boolean) = db.withTransaction {
        require(purchase.id == 0L) { "Cet achat existe déjà" }
        val plan = requireNotNull(db.etfDao().plan(purchase.assetId)) { "Configure d’abord l’ETF" }.toDomain()
        EtfTracking.validate(purchase, plan)
        val all = db.etfDao().purchases().map { it.toDomain() }
        purchase.transactionId?.let { id ->
            val tx = requireNotNull(db.transactionDao().getById(id)) { "Opération supprimée : actualise la liste" }.toDomain()
            require(EtfTracking.eligible(tx, plan)) { "Cette opération ne correspond plus au compte ou à la période" }
            require(purchase.amountCents == tx.amountCents && purchase.date == tx.date) { "L’opération a changé : actualise la liste" }
            require(all.none { it.transactionId == id }) { "Cette opération est déjà validée" }
            require(distinctPurchaseConfirmed || EtfTracking.manualMatches(tx, all, plan.assetId).isEmpty()) {
                "Un achat manuel correspond : associe-le ou confirme un achat distinct"
            }
        }
        if (purchase.transactionId == null) {
            require(distinctPurchaseConfirmed || all.none { it.assetId == purchase.assetId && it.date == purchase.date &&
                (it.amountCents == purchase.amountCents || it.totalCents == purchase.totalCents) }) { "Un achat identique existe déjà" }
        }
        val invested = all.filter { it.assetId == plan.assetId }.fold(plan.initialInvestedCents) { total, row -> Math.addExact(total, row.totalCents) }
        Math.addExact(invested, purchase.totalCents)
        db.etfDao().insert(EtfPurchaseEntity.fromDomain(purchase))
        Unit
    }

    override suspend fun linkPurchase(assetId: Long, purchaseId: Long, transactionId: Long) = db.withTransaction {
        val plan = requireNotNull(db.etfDao().plan(assetId)).toDomain()
        val all = db.etfDao().purchases()
        val purchase = requireNotNull(all.find { it.id == purchaseId && it.assetId == assetId }) { "Achat introuvable" }
        val tx = requireNotNull(db.transactionDao().getById(transactionId)) { "Opération introuvable" }.toDomain()
        require(EtfTracking.eligible(tx, plan)) { "Opération incompatible" }
        require(all.none { it.transactionId == transactionId }) { "Cette opération est déjà validée" }
        require(EtfTracking.manualMatches(tx, listOf(purchase.toDomain()), assetId).isNotEmpty()) { "L’achat manuel ne correspond plus" }
        // Ne changer ni le montant ni les frais : le capital a déjà été comptabilisé.
        db.etfDao().update(purchase.copy(transactionId = transactionId))
    }

    override suspend fun removePurchase(assetId: Long, purchaseId: Long) = db.withTransaction {
        db.etfDao().delete(assetId, purchaseId)
    }

    override suspend fun updateValue(assetId: Long, valueCents: Long, date: LocalDate) = db.withTransaction {
        require(valueCents >= 0 && date <= LocalDate.now()) { "Valeur ou date invalide" }
        val plan = requireNotNull(db.etfDao().plan(assetId)) { "ETF introuvable" }
        require(date.toEpochDay() >= plan.referenceEpochDay) { "La valeur doit être datée après la référence" }
        val asset = requireNotNull(db.customAssetDao().getAll().first().find { it.id == assetId })
        require(date.toEpochDay() >= asset.updatedAtEpochDay) { "La date précède la dernière valorisation" }
        db.customAssetDao().update(asset.copy(totalValueCents = valueCents, updatedAtEpochDay = date.toEpochDay()))
        // Une seconde saisie le même jour corrige le point de ce jour, pas le capital investi.
        db.assetValuationSnapshotDao().deleteForDay("CUSTOM_ASSET", assetId, date.toEpochDay())
        db.assetValuationSnapshotDao().insert(AssetValuationSnapshotEntity(assetType = "CUSTOM_ASSET", assetId = assetId,
            snapshotEpochDay = date.toEpochDay(), valueCents = valueCents, currency = plan.currency))
    }
}

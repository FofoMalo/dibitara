package com.dibitara.app.data.export

import com.dibitara.app.data.local.entity.EtfPlanEntity
import com.dibitara.app.data.local.entity.EtfPurchaseEntity
import com.dibitara.app.domain.model.CustomAsset
import com.dibitara.app.domain.model.EtfTracking

/** Validation complète avant le DELETE de restauration, y compris les références et doublons. */
object EtfBackupValidator {
    fun validate(plans: List<EtfPlanEntity>, purchases: List<EtfPurchaseEntity>, assets: List<CustomAsset>) {
        require(plans.map { it.assetId }.distinct().size == plans.size) { "Suivis ETF dupliqués" }
        require(purchases.map { it.id }.distinct().size == purchases.size && purchases.all { it.id > 0 }) { "Identifiants d’achats ETF invalides" }
        val linked = purchases.mapNotNull { it.transactionId }
        require(linked.distinct().size == linked.size && linked.all { it > 0 }) { "Opération liée à plusieurs achats ETF" }
        plans.forEach { entity ->
            val plan = entity.toDomain()
            EtfTracking.validate(plan)
            require(assets.any { it.id == plan.assetId && it.currency == plan.currency }) { "Actif ETF absent ou devise incompatible" }
        }
        purchases.forEach { entity ->
            val plan = requireNotNull(plans.find { it.assetId == entity.assetId }) { "Achat sans suivi ETF" }.toDomain()
            EtfTracking.validate(entity.toDomain(), plan)
        }
        plans.forEach { plan ->
            purchases.filter { it.assetId == plan.assetId }.fold(plan.initialInvestedCents) { sum, p -> Math.addExact(sum, p.toDomain().totalCents) }
        }
    }
}

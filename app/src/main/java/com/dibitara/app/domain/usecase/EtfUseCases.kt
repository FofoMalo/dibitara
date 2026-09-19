package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.EtfRepository
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

data class EtfState(val data: EtfData, val summaries: Map<Long, EtfSummary>)

class ObserveEtfUseCase @Inject constructor(private val repository: EtfRepository) {
    operator fun invoke() = repository.observe().map { data ->
        EtfState(data, data.plans.mapNotNull { plan ->
            data.assets.find { it.id == plan.assetId }?.let { plan.assetId to EtfTracking.summary(plan, it, data) }
        }.toMap())
    }
}
class SaveEtfPlanUseCase @Inject constructor(private val repository: EtfRepository) {
    suspend operator fun invoke(plan: EtfPlan) = repository.savePlan(plan)
}
class ConfirmEtfPurchaseUseCase @Inject constructor(private val repository: EtfRepository) {
    suspend operator fun invoke(purchase: EtfPurchase, distinct: Boolean = false) = repository.confirmPurchase(purchase, distinct)
}
class LinkEtfPurchaseUseCase @Inject constructor(private val repository: EtfRepository) {
    suspend operator fun invoke(assetId: Long, purchaseId: Long, transactionId: Long) = repository.linkPurchase(assetId, purchaseId, transactionId)
}
class RemoveEtfPurchaseUseCase @Inject constructor(private val repository: EtfRepository) {
    suspend operator fun invoke(assetId: Long, purchaseId: Long) = repository.removePurchase(assetId, purchaseId)
}
class UpdateEtfValueUseCase @Inject constructor(private val repository: EtfRepository) {
    suspend operator fun invoke(assetId: Long, value: Long, date: LocalDate) = repository.updateValue(assetId, value, date)
}

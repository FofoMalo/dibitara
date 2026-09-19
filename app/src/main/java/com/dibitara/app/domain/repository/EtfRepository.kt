package com.dibitara.app.domain.repository

import com.dibitara.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface EtfRepository {
    fun observe(): Flow<EtfData>
    suspend fun savePlan(plan: EtfPlan)
    suspend fun confirmPurchase(purchase: EtfPurchase, distinctPurchaseConfirmed: Boolean = false)
    suspend fun linkPurchase(assetId: Long, purchaseId: Long, transactionId: Long)
    suspend fun removePurchase(assetId: Long, purchaseId: Long)
    suspend fun updateValue(assetId: Long, valueCents: Long, date: LocalDate)
}

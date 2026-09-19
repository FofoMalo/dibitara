package com.dibitara.app.presentation.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.EtfPlan
import com.dibitara.app.domain.model.EtfPurchase
import com.dibitara.app.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class EtfViewModel @Inject constructor(
    observe: ObserveEtfUseCase,
    private val savePlan: SaveEtfPlanUseCase,
    private val confirmPurchase: ConfirmEtfPurchaseUseCase,
    private val linkPurchase: LinkEtfPurchaseUseCase,
    private val removePurchase: RemoveEtfPurchaseUseCase,
    private val updateValue: UpdateEtfValueUseCase
) : ViewModel() {
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val state = observe().catch { error.value = it.message ?: "Suivi ETF indisponible" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun clearError() { error.value = null }
    private fun action(done: () -> Unit, block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        error.value = null
        viewModelScope.launch {
            try { block(); done() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Enregistrement impossible" }
            finally { busy.value = false }
        }
    }
    fun configure(plan: EtfPlan, done: () -> Unit) = action(done) { savePlan(plan) }
    fun confirm(purchase: EtfPurchase, distinct: Boolean, done: () -> Unit) = action(done) { confirmPurchase(purchase, distinct) }
    fun link(assetId: Long, purchaseId: Long, transactionId: Long, done: () -> Unit) =
        action(done) { linkPurchase(assetId, purchaseId, transactionId) }
    fun remove(assetId: Long, purchaseId: Long, done: () -> Unit) = action(done) { removePurchase(assetId, purchaseId) }
    fun value(assetId: Long, cents: Long, date: LocalDate, done: () -> Unit) = action(done) { updateValue(assetId, cents, date) }
}

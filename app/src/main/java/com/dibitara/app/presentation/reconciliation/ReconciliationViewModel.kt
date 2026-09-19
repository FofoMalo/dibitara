package com.dibitara.app.presentation.reconciliation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.usecase.*
import com.dibitara.app.domain.model.BankAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.YearMonth
import javax.inject.Inject

data class ReconciliationInput(val accountId: Long? = null, val month: YearMonth = YearMonth.now(), val opening: String = "", val closing: String = "")
data class ReconciliationState(val accounts: List<BankAccount> = emptyList(), val result: RapprochementResult? = null, val error: String? = null)

@HiltViewModel
class ReconciliationViewModel @Inject constructor(
    comptes: GetBankAccountsUseCase,
    transactions: GetAllTransactionsUseCase,
    private val rapprocher: RapprocherReleveUseCase
) : ViewModel() {
    private val _input = MutableStateFlow(ReconciliationInput())
    val input = _input.asStateFlow()
    fun change(value: ReconciliationInput) { _input.value = value }
    val state = combine(comptes(), transactions(), input) { accounts, entries, form ->
        val account = accounts.firstOrNull { it.id == form.accountId }
        if (account == null || form.opening.isBlank() || form.closing.isBlank()) ReconciliationState(accounts)
        else runCatching {
            fun cents(text: String) = text.replace(',', '.').trim().toBigDecimal().movePointRight(2).longValueExact()
            ReconciliationState(accounts, rapprocher(account, form.month, cents(form.opening), cents(form.closing), entries))
        }.getOrElse { ReconciliationState(accounts, error = "Saisissez des montants valides avec au plus deux décimales.") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReconciliationState())
}

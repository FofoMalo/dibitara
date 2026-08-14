package com.dibitara.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.BankAccountsSummary
import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.usecase.DeleteBankAccountUseCase
import com.dibitara.app.domain.usecase.GetBankAccountsSummaryUseCase
import com.dibitara.app.domain.usecase.UpsertBankAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToLong

@HiltViewModel
class BankAccountsViewModel @Inject constructor(
    getBankAccountsSummary: GetBankAccountsSummaryUseCase,
    private val upsertBankAccount: UpsertBankAccountUseCase,
    private val deleteBankAccount: DeleteBankAccountUseCase
) : ViewModel() {

    val summary: StateFlow<BankAccountsSummary> = getBankAccountsSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BankAccountsSummary(emptyList(), 0L, Currency.EUR))

    fun sauvegarder(
        compteExistant: BankAccount?,
        provider: BankProvider,
        label: String,
        soldeStr: String,
        currency: Currency
    ) {
        val cents = soldeStr.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: return
        viewModelScope.launch {
            upsertBankAccount(
                BankAccount(
                    id                  = compteExistant?.id ?: 0L,
                    provider            = provider,
                    label               = label,
                    currentBalanceCents = cents,
                    currency            = currency,
                    updatedAt           = LocalDate.now()
                )
            )
        }
    }

    fun supprimer(compte: BankAccount) {
        viewModelScope.launch { deleteBankAccount(compte) }
    }
}

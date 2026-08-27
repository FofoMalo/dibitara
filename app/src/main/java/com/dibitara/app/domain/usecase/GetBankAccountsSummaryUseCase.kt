package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BankAccountsSummary
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.repository.BankAccountRepository
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Comptes bancaires + somme de leurs soldes convertie dans la devise par défaut. */
class GetBankAccountsSummaryUseCase @Inject constructor(
    private val bankAccountRepository: BankAccountRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exchangeRateRepository: ExchangeRateRepository
) {
    operator fun invoke(): Flow<BankAccountsSummary> = combine(
        bankAccountRepository.getAll(),
        userPreferencesRepository.get(),
        exchangeRateRepository.getRatesFlow()
    ) { comptes, prefs, rates ->
        val target = prefs.deviseParDefaut
        val total = comptes.sumOf { CurrencyConverter.convertCents(it.currentBalanceCents, it.currency, target, rates) }
        BankAccountsSummary(comptes = comptes, totalCents = total, currency = target)
    }
}

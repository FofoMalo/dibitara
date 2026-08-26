package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BankProvider
import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.repository.BankAccountRepository
import com.dibitara.app.domain.repository.ExchangeRateRepository
import com.dibitara.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Calcule le solde réel disponible sur les comptes courants suivis, hors compte pro (Qonto).
 *
 * Même source que [GetCashflowProjectionUseCase] : le solde réel des [com.dibitara.app.domain.model.BankAccount],
 * pas un flux recalculé depuis les transactions du mois en cours - cet ancien calcul repartait
 * quasiment de zéro chaque mois avant la paie et faussait l'alerte "liquidités insuffisantes"
 * vers un déclenchement systématique.
 *
 * Retourne le solde en centimes, dans la devise par défaut de l'utilisateur.
 */
class CheckAvailableFundsUseCase @Inject constructor(
    private val bankAccountRepository: BankAccountRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exchangeRateRepository: ExchangeRateRepository
) {
    suspend operator fun invoke(): Long {
        val comptes = bankAccountRepository.getAll().first()
        val prefs   = userPreferencesRepository.get().first()
        val rates   = exchangeRateRepository.getRatesFlow().first()

        return comptes
            .filter { it.provider != BankProvider.QONTO }
            .sumOf { CurrencyConverter.convertCents(it.currentBalanceCents, it.currency, prefs.deviseParDefaut, rates) }
    }
}

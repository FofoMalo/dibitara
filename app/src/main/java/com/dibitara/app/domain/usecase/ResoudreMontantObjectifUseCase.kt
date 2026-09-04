package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CurrencyConverter
import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.model.FundingMode
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsGoal
import javax.inject.Inject

/**
 * Résout le montant « épargné » à afficher pour un objectif, selon son
 * `fundingModeEffectif` (§4 de CADRAGE_OBJECTIFS_CONNECTES.md). UseCase pur (pas d'accès
 * base) : [comptes] et [rates] sont déjà chargés par l'appelant (SavingsViewModel les a
 * déjà pour construire son propre `uiState`).
 *
 * - MANUEL / VERSEMENTS : `goal.currentAmountCents` fait foi tel quel - VERSEMENTS
 *   l'avance déjà via `AppliquerVersementsObjectifUseCase`, rien à recalculer ici.
 * - SOLDE_COMPTE : dérivé en lecture seule du solde de `sourceAccountId`, converti dans
 *   la devise de l'objectif si besoin. Ne touche JAMAIS `currentAmountCents` en base -
 *   c'est un simple `copy()` d'affichage (voir usage dans `SavingsViewModel.objectifs`).
 *   Si le compte lié a été supprimé entre-temps, on retombe sur la dernière valeur
 *   stockée plutôt que de planter ou d'afficher 0.
 */
class ResoudreMontantObjectifUseCase @Inject constructor() {
    operator fun invoke(
        goal: SavingsGoal,
        comptes: List<SavingsAccount>,
        rates: ExchangeRates
    ): Long {
        if (goal.fundingModeEffectif != FundingMode.SOLDE_COMPTE) return goal.currentAmountCents

        val compte = comptes.firstOrNull { it.id == goal.sourceAccountId } ?: return goal.currentAmountCents
        return CurrencyConverter.convertCents(compte.currentBalanceCents, compte.currency, goal.currency, rates)
    }
}

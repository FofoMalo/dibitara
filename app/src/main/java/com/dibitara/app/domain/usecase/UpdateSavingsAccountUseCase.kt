package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.repository.SavingsRepository
import javax.inject.Inject

/**
 * Modifie un compte épargne existant.
 * L'id du compte doit être > 0 pour que Room mette à jour la bonne ligne.
 */
class UpdateSavingsAccountUseCase @Inject constructor(
    private val repository: SavingsRepository
) {
    suspend operator fun invoke(account: SavingsAccount): Result<Unit> {
        if (account.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (account.currentBalanceCents < 0) return Result.failure(IllegalArgumentException("Le solde ne peut pas être négatif"))
        return runCatching { repository.update(account) }
    }
}

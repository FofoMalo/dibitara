package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.repository.SavingsRepository
import javax.inject.Inject

class SaveSavingsAccountUseCase @Inject constructor(
    private val repository: SavingsRepository
) {
    suspend operator fun invoke(account: SavingsAccount): Result<Long> {
        if (account.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (account.currentBalanceCents < 0) return Result.failure(IllegalArgumentException("Le solde ne peut pas être négatif"))
        return repository.save(account)
    }
}

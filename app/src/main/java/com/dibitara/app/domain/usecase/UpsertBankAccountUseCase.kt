package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.repository.BankAccountRepository
import javax.inject.Inject

/** Crée ou met à jour un compte bancaire. Le libellé est obligatoire. */
class UpsertBankAccountUseCase @Inject constructor(
    private val repository: BankAccountRepository
) {
    suspend operator fun invoke(account: BankAccount): Result<Long> {
        if (account.label.isBlank()) {
            return Result.failure(IllegalArgumentException("Le libellé du compte est obligatoire"))
        }
        return runCatching { repository.upsert(account) }
    }
}

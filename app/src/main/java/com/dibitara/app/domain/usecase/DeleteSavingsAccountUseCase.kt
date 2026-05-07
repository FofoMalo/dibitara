package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.repository.SavingsRepository
import javax.inject.Inject

class DeleteSavingsAccountUseCase @Inject constructor(
    private val repository: SavingsRepository
) {
    suspend operator fun invoke(account: SavingsAccount) = repository.delete(account)
}

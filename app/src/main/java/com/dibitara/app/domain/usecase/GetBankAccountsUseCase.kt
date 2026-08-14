package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.repository.BankAccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Retourne tous les comptes bancaires suivis, en temps réel. */
class GetBankAccountsUseCase @Inject constructor(
    private val repository: BankAccountRepository
) {
    operator fun invoke(): Flow<List<BankAccount>> = repository.getAll()
}

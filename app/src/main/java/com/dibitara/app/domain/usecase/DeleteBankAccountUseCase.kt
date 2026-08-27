package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.repository.BankAccountRepository
import javax.inject.Inject

/**
 * Supprime un compte bancaire.
 * Les transactions déjà rattachées via [Transaction.bankAccountId] ne sont pas modifiées :
 * l'id devient orphelin et est traité comme "non rattaché", au même titre que null.
 */
class DeleteBankAccountUseCase @Inject constructor(
    private val repository: BankAccountRepository
) {
    suspend operator fun invoke(account: BankAccount) = repository.delete(account)
}

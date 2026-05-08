package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.repository.DebtRepository
import javax.inject.Inject

class DeleteDebtUseCase @Inject constructor(
    private val repository: DebtRepository
) {
    suspend operator fun invoke(debt: Debt) = repository.delete(debt)
}

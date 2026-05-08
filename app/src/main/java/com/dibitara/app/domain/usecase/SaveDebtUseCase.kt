package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.repository.DebtRepository
import javax.inject.Inject

class SaveDebtUseCase @Inject constructor(
    private val repository: DebtRepository
) {
    suspend operator fun invoke(debt: Debt): Result<Long> {
        if (debt.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        if (debt.totalCents <= 0) return Result.failure(IllegalArgumentException("Le montant doit être supérieur à 0"))
        return repository.save(debt)
    }
}

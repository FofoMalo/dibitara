package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.EmployeeSavings
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import javax.inject.Inject

class SaveEmployeeSavingsUseCase @Inject constructor(
    private val repository: CustomInvestmentRepository
) {
    suspend operator fun invoke(savings: EmployeeSavings): Result<Long> {
        if (savings.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        return repository.saveEmployeeSavings(savings)
    }
}

package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.EmployeeSavings
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import javax.inject.Inject

class UpdateEmployeeSavingsUseCase @Inject constructor(
    private val repository: CustomInvestmentRepository
) {
    suspend operator fun invoke(savings: EmployeeSavings): Result<Unit> {
        if (savings.label.isBlank()) return Result.failure(IllegalArgumentException("Le libellé est requis"))
        return runCatching { repository.updateEmployeeSavings(savings) }
    }
}

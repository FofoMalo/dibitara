package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.EmployeeSavings
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import javax.inject.Inject

class DeleteEmployeeSavingsUseCase @Inject constructor(
    private val repository: CustomInvestmentRepository
) {
    suspend operator fun invoke(savings: EmployeeSavings) = repository.deleteEmployeeSavings(savings)
}

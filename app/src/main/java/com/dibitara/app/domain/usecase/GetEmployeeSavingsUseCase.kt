package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.EmployeeSavings
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEmployeeSavingsUseCase @Inject constructor(
    private val repository: CustomInvestmentRepository
) {
    operator fun invoke(): Flow<List<EmployeeSavings>> = repository.getAllEmployeeSavings()
}

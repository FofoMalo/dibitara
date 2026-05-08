package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Debt
import com.dibitara.app.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDebtsUseCase @Inject constructor(
    private val repository: DebtRepository
) {
    operator fun invoke(): Flow<List<Debt>> = repository.getAll()
}

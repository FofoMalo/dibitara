package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.repository.SavingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSavingsUseCase @Inject constructor(
    private val repository: SavingsRepository
) {
    operator fun invoke(): Flow<List<SavingsAccount>> = repository.getAll()
    fun byChild(childId: Long): Flow<List<SavingsAccount>> = repository.getByChild(childId)
}

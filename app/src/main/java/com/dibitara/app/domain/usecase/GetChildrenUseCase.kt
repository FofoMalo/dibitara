package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.repository.ChildRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChildrenUseCase @Inject constructor(
    private val repository: ChildRepository
) {
    operator fun invoke(): Flow<List<Child>> = repository.getAll()
}

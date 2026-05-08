package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.repository.ChildRepository
import javax.inject.Inject

class DeleteChildUseCase @Inject constructor(
    private val repository: ChildRepository
) {
    suspend operator fun invoke(child: Child) = repository.delete(child)
}

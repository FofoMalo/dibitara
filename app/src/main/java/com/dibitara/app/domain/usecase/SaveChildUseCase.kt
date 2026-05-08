package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.repository.ChildRepository
import javax.inject.Inject

class SaveChildUseCase @Inject constructor(
    private val repository: ChildRepository
) {
    suspend operator fun invoke(child: Child): Result<Long> {
        if (child.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Le prénom de l'enfant est requis"))
        }
        return repository.save(child)
    }
}

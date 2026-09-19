package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.repository.TransactionActionsRepository
import javax.inject.Inject

class CategoriserSelectionUseCase @Inject constructor(private val repository: TransactionActionsRepository) {
    suspend operator fun invoke(ids: Set<Long>, category: Category): Result<Int> = runCatching {
        require(ids.isNotEmpty()) { "Sélectionnez au moins une dépense." }
        repository.categoriser(ids, category)
    }
}

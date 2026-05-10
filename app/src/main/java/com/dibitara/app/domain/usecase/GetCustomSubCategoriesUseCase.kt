package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CustomSubCategory
import com.dibitara.app.domain.repository.CustomSubCategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Retourne toutes les sous-catégories personnalisées, optionnellement filtrées par catégorie.
class GetCustomSubCategoriesUseCase @Inject constructor(
    private val repository: CustomSubCategoryRepository
) {
    operator fun invoke(): Flow<List<CustomSubCategory>> = repository.getAll()
    operator fun invoke(category: Category): Flow<List<CustomSubCategory>> = repository.getByCategory(category)
}

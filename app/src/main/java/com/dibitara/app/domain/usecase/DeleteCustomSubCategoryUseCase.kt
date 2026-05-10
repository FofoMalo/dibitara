package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CustomSubCategory
import com.dibitara.app.domain.repository.CustomSubCategoryRepository
import javax.inject.Inject

// Supprime une sous-catégorie personnalisée. Les transactions liées ne sont pas modifiées.
class DeleteCustomSubCategoryUseCase @Inject constructor(
    private val repository: CustomSubCategoryRepository
) {
    suspend operator fun invoke(subCategory: CustomSubCategory) = repository.delete(subCategory)
}

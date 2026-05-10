package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.CustomSubCategory
import com.dibitara.app.domain.repository.CustomSubCategoryRepository
import javax.inject.Inject

// Crée ou met à jour une sous-catégorie personnalisée.
class UpsertCustomSubCategoryUseCase @Inject constructor(
    private val repository: CustomSubCategoryRepository
) {
    suspend operator fun invoke(subCategory: CustomSubCategory) = repository.upsert(subCategory)
}

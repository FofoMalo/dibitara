package com.dibitara.app.data.repository

import com.dibitara.app.data.local.dao.CustomSubCategoryDao
import com.dibitara.app.data.local.entity.CustomSubCategoryEntity
import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CustomSubCategory
import com.dibitara.app.domain.repository.CustomSubCategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CustomSubCategoryRepositoryImpl @Inject constructor(
    private val dao: CustomSubCategoryDao
) : CustomSubCategoryRepository {

    override fun getAll(): Flow<List<CustomSubCategory>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getByCategory(category: Category): Flow<List<CustomSubCategory>> =
        dao.getByCategory(category.name).map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(subCategory: CustomSubCategory) =
        dao.upsert(CustomSubCategoryEntity.fromDomain(subCategory))

    override suspend fun delete(subCategory: CustomSubCategory) =
        dao.delete(CustomSubCategoryEntity.fromDomain(subCategory))
}

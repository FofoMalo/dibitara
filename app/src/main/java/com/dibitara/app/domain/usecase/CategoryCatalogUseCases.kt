package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.CategoryCatalogRepository
import javax.inject.Inject

class ObserveCategoryCatalogUseCase @Inject constructor(private val r: CategoryCatalogRepository) { operator fun invoke() = r.observe() }
class SaveCategoryNodeUseCase @Inject constructor(private val r: CategoryCatalogRepository) { suspend operator fun invoke(n: CategoryNode) = r.save(n) }
class CreateCategoryNodeUseCase @Inject constructor(private val r: CategoryCatalogRepository) { suspend operator fun invoke(name: String, parent: String?) = r.create(name,parent) }
class CategoryImpactUseCase @Inject constructor(private val r: CategoryCatalogRepository) { suspend operator fun invoke(key: String) = r.impact(key) }
class DeleteUnusedCategoryUseCase @Inject constructor(private val r: CategoryCatalogRepository) { suspend operator fun invoke(key: String) = r.deleteUnused(key) }
class MergeCategoryUseCase @Inject constructor(private val r: CategoryCatalogRepository) { suspend operator fun invoke(a: String,b: String,impact: CategoryImpact) = r.merge(a,b,impact) }
class MoveSubCategoryUseCase @Inject constructor(private val r: CategoryCatalogRepository) { suspend operator fun invoke(a: String,b: String,impact: CategoryImpact) = r.move(a,b,impact) }
class ClassifyTransactionsUseCase @Inject constructor(private val r: CategoryCatalogRepository) { suspend operator fun invoke(ids: Set<Long>,choice: CategoryChoice) = r.classify(ids,choice) }
class RememberCategoryRuleUseCase @Inject constructor(private val r: CategoryCatalogRepository) { suspend operator fun invoke(note: String,choice: CategoryChoice) = r.rememberRule(note,choice) }
class FindCategoryMatchesUseCase @Inject constructor(private val r: CategoryCatalogRepository) { suspend operator fun invoke(note: String,choice: CategoryChoice) = r.matching(note,choice) }

package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Category
import com.dibitara.app.domain.model.CustomSubCategory
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.Transaction
import com.dibitara.app.domain.model.TransactionType
import com.dibitara.app.domain.repository.CustomSubCategoryRepository
import com.dibitara.app.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MigrerTabacVersCategorieUseCaseTest {

    private val customSubCategoryRepository: CustomSubCategoryRepository = mockk()
    private val transactionRepository      : TransactionRepository       = mockk()
    private val useCase = MigrerTabacVersCategorieUseCase(customSubCategoryRepository, transactionRepository)

    private fun buildTransaction(id: Long, customSubCategoryId: Long?, category: Category = Category.AUTRE) =
        Transaction(
            id = id, amountCents = 500L, currency = Currency.EUR,
            category = category, type = TransactionType.EXPENSE,
            date = LocalDate.of(2026, 5, 10), customSubCategoryId = customSubCategoryId
        )

    @Test
    fun `ne fait rien si la sous-catégorie Tabac n'existe pas`() = runTest {
        every { customSubCategoryRepository.getByCategory(Category.AUTRE) } returns flowOf(emptyList())

        useCase()

        coVerify(exactly = 0) { transactionRepository.update(any()) }
        coVerify(exactly = 0) { customSubCategoryRepository.delete(any()) }
    }

    @Test
    fun `réassigne les transactions liées vers Category-TABAC et supprime la sous-catégorie`() = runTest {
        val tabac = CustomSubCategory(id = 7, name = "Tabac", parentCategory = Category.AUTRE)
        every { customSubCategoryRepository.getByCategory(Category.AUTRE) } returns flowOf(listOf(tabac))
        every { transactionRepository.getAll() } returns flowOf(listOf(
            buildTransaction(id = 1, customSubCategoryId = 7),
            buildTransaction(id = 2, customSubCategoryId = 7),
            buildTransaction(id = 3, customSubCategoryId = 99), // autre sous-catégorie, ignorée
            buildTransaction(id = 4, customSubCategoryId = null, category = Category.ALIMENTATION)
        ))
        coEvery { transactionRepository.update(any()) } returns Unit
        coEvery { customSubCategoryRepository.delete(tabac) } returns Unit

        useCase()

        coVerify(exactly = 1) {
            transactionRepository.update(match { it.id == 1L && it.category == Category.TABAC && it.customSubCategoryId == null })
        }
        coVerify(exactly = 1) {
            transactionRepository.update(match { it.id == 2L && it.category == Category.TABAC && it.customSubCategoryId == null })
        }
        coVerify(exactly = 0) { transactionRepository.update(match { it.id == 3L || it.id == 4L }) }
        coVerify(exactly = 1) { customSubCategoryRepository.delete(tabac) }
    }

    @Test
    fun `correspondance insensible à la casse`() = runTest {
        val tabac = CustomSubCategory(id = 3, name = "tabac", parentCategory = Category.AUTRE)
        every { customSubCategoryRepository.getByCategory(Category.AUTRE) } returns flowOf(listOf(tabac))
        every { transactionRepository.getAll() } returns flowOf(emptyList())
        coEvery { customSubCategoryRepository.delete(tabac) } returns Unit

        useCase()

        coVerify(exactly = 1) { customSubCategoryRepository.delete(tabac) }
    }
}

package com.dibitara.app.domain.usecase

import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.CustomAsset
import com.dibitara.app.domain.model.EmployeeSavings
import com.dibitara.app.domain.model.EmployeeSavingsType
import com.dibitara.app.domain.model.MetalType
import com.dibitara.app.domain.model.PreciousMetalAsset
import com.dibitara.app.domain.repository.CustomInvestmentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CustomInvestmentUseCasesTest {

    private val repository: CustomInvestmentRepository = mockk()

    private fun buildMetal(
        label: String = "Or Lingot",
        quantity: Double = 10.0,
        price: Long = 6000L
    ) = PreciousMetalAsset(
        metalType = MetalType.OR, label = label,
        quantityGrams = quantity, pricePerGramCents = price,
        currency = Currency.EUR, updatedAt = LocalDate.now()
    )

    private fun buildAsset(label: String = "Cryptos", value: Long = 50000L) =
        CustomAsset(label = label, totalValueCents = value, currency = Currency.EUR, updatedAt = LocalDate.now())

    private fun buildSavings(label: String = "PEE Entreprise") =
        EmployeeSavings(
            type = EmployeeSavingsType.PEE, label = label,
            currentBalanceCents = 100000L, employerContributionCents = 5000L,
            currency = Currency.EUR, updatedAt = LocalDate.now()
        )

    // ─── GetPreciousMetalsUseCase ─────────────────────────────────────────────

    @Test
    fun `GetPreciousMetals délègue au repository`() {
        every { repository.getAllPreciousMetals() } returns flowOf(emptyList())
        assertNotNull(GetPreciousMetalsUseCase(repository)())
    }

    // ─── SavePreciousMetalUseCase ─────────────────────────────────────────────

    @Test
    fun `SavePreciousMetal retourne succès`() = runTest {
        coEvery { repository.savePreciousMetal(any()) } returns Result.success(1L)
        assertTrue(SavePreciousMetalUseCase(repository)(buildMetal()).isSuccess)
    }

    @Test
    fun `SavePreciousMetal échoue si libellé vide`() = runTest {
        assertTrue(SavePreciousMetalUseCase(repository)(buildMetal(label = "")).isFailure)
    }

    @Test
    fun `SavePreciousMetal échoue si quantité nulle`() = runTest {
        assertTrue(SavePreciousMetalUseCase(repository)(buildMetal(quantity = 0.0)).isFailure)
    }

    @Test
    fun `SavePreciousMetal échoue si quantité négative`() = runTest {
        assertTrue(SavePreciousMetalUseCase(repository)(buildMetal(quantity = -5.0)).isFailure)
    }

    // ─── UpdatePreciousMetalUseCase ───────────────────────────────────────────

    @Test
    fun `UpdatePreciousMetal retourne succès`() = runTest {
        coEvery { repository.updatePreciousMetal(any()) } returns Unit
        assertTrue(UpdatePreciousMetalUseCase(repository)(buildMetal()).isSuccess)
    }

    @Test
    fun `UpdatePreciousMetal échoue si libellé vide`() = runTest {
        assertTrue(UpdatePreciousMetalUseCase(repository)(buildMetal(label = "")).isFailure)
    }

    @Test
    fun `UpdatePreciousMetal échoue si quantité nulle`() = runTest {
        assertTrue(UpdatePreciousMetalUseCase(repository)(buildMetal(quantity = 0.0)).isFailure)
    }

    // ─── DeletePreciousMetalUseCase ───────────────────────────────────────────

    @Test
    fun `DeletePreciousMetal délègue au repository`() = runTest {
        val metal = buildMetal()
        coEvery { repository.deletePreciousMetal(metal) } returns Unit
        DeletePreciousMetalUseCase(repository)(metal)
        coVerify { repository.deletePreciousMetal(metal) }
    }

    // ─── GetCustomAssetsUseCase ───────────────────────────────────────────────

    @Test
    fun `GetCustomAssets délègue au repository`() {
        every { repository.getAllCustomAssets() } returns flowOf(emptyList())
        assertNotNull(GetCustomAssetsUseCase(repository)())
    }

    // ─── SaveCustomAssetUseCase ───────────────────────────────────────────────

    @Test
    fun `SaveCustomAsset retourne succès`() = runTest {
        coEvery { repository.saveCustomAsset(any()) } returns Result.success(1L)
        assertTrue(SaveCustomAssetUseCase(repository)(buildAsset()).isSuccess)
    }

    @Test
    fun `SaveCustomAsset échoue si libellé vide`() = runTest {
        assertTrue(SaveCustomAssetUseCase(repository)(buildAsset(label = "")).isFailure)
    }

    @Test
    fun `SaveCustomAsset échoue si valeur nulle`() = runTest {
        assertTrue(SaveCustomAssetUseCase(repository)(buildAsset(value = 0L)).isFailure)
    }

    @Test
    fun `SaveCustomAsset échoue si valeur négative`() = runTest {
        assertTrue(SaveCustomAssetUseCase(repository)(buildAsset(value = -100L)).isFailure)
    }

    // ─── UpdateCustomAssetUseCase ─────────────────────────────────────────────

    @Test
    fun `UpdateCustomAsset retourne succès`() = runTest {
        coEvery { repository.updateCustomAsset(any()) } returns Unit
        assertTrue(UpdateCustomAssetUseCase(repository)(buildAsset()).isSuccess)
    }

    @Test
    fun `UpdateCustomAsset échoue si libellé vide`() = runTest {
        assertTrue(UpdateCustomAssetUseCase(repository)(buildAsset(label = "")).isFailure)
    }

    // ─── DeleteCustomAssetUseCase ─────────────────────────────────────────────

    @Test
    fun `DeleteCustomAsset délègue au repository`() = runTest {
        val asset = buildAsset()
        coEvery { repository.deleteCustomAsset(asset) } returns Unit
        DeleteCustomAssetUseCase(repository)(asset)
        coVerify { repository.deleteCustomAsset(asset) }
    }

    // ─── GetEmployeeSavingsUseCase ────────────────────────────────────────────

    @Test
    fun `GetEmployeeSavings délègue au repository`() {
        every { repository.getAllEmployeeSavings() } returns flowOf(emptyList())
        assertNotNull(GetEmployeeSavingsUseCase(repository)())
    }

    // ─── SaveEmployeeSavingsUseCase ───────────────────────────────────────────

    @Test
    fun `SaveEmployeeSavings retourne succès`() = runTest {
        coEvery { repository.saveEmployeeSavings(any()) } returns Result.success(1L)
        assertTrue(SaveEmployeeSavingsUseCase(repository)(buildSavings()).isSuccess)
    }

    @Test
    fun `SaveEmployeeSavings échoue si libellé vide`() = runTest {
        assertTrue(SaveEmployeeSavingsUseCase(repository)(buildSavings(label = "")).isFailure)
    }

    // ─── UpdateEmployeeSavingsUseCase ─────────────────────────────────────────

    @Test
    fun `UpdateEmployeeSavings retourne succès`() = runTest {
        coEvery { repository.updateEmployeeSavings(any()) } returns Unit
        assertTrue(UpdateEmployeeSavingsUseCase(repository)(buildSavings()).isSuccess)
    }

    @Test
    fun `UpdateEmployeeSavings échoue si libellé vide`() = runTest {
        assertTrue(UpdateEmployeeSavingsUseCase(repository)(buildSavings(label = "")).isFailure)
    }

    // ─── DeleteEmployeeSavingsUseCase ─────────────────────────────────────────

    @Test
    fun `DeleteEmployeeSavings délègue au repository`() = runTest {
        val savings = buildSavings()
        coEvery { repository.deleteEmployeeSavings(savings) } returns Unit
        DeleteEmployeeSavingsUseCase(repository)(savings)
        coVerify { repository.deleteEmployeeSavings(savings) }
    }

    // ─── PreciousMetalAsset.totalValueCents ───────────────────────────────────

    @Test
    fun `totalValueCents calcule quantité × prix`() {
        // 10 g × 6 000 centimes/g = 60 000 centimes
        val metal = buildMetal(quantity = 10.0, price = 6000L)
        assertEquals(60000L, metal.totalValueCents)
    }

    @Test
    fun `totalValueCents arrondit à l'entier inférieur pour les fractions`() {
        // 2.5 g × 3 centimes/g = 7.5 → tronqué à 7
        val metal = buildMetal(quantity = 2.5, price = 3L)
        assertEquals(7L, metal.totalValueCents)
    }
}

package com.dibitara.app.presentation.auth

import com.dibitara.app.security.AuthResult
import com.dibitara.app.security.BiometricAuthManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests unitaires de AuthViewModel.
 *
 * On remplace le Dispatcher Main par un TestDispatcher pour que les coroutines
 * s'exécutent de façon synchrone dans les tests (pas de vrai thread Android).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val biometricAuthManager: BiometricAuthManager = mockk()
    private lateinit var viewModel: AuthViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(biometricAuthManager)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `état initial est Idle`() {
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `authentification réussie → état Authenticated`() = runTest {
        val activity = mockk<androidx.fragment.app.FragmentActivity>()
        every { biometricAuthManager.authenticate(activity) } returns flowOf(AuthResult.Success)

        viewModel.authenticate(activity)

        assertEquals(AuthUiState.Authenticated, viewModel.uiState.value)
    }

    @Test
    fun `authentification annulée → retour à Idle`() = runTest {
        val activity = mockk<androidx.fragment.app.FragmentActivity>()
        every { biometricAuthManager.authenticate(activity) } returns flowOf(AuthResult.Cancelled)

        viewModel.authenticate(activity)

        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `erreur biométrique → état Error avec message`() = runTest {
        val activity = mockk<androidx.fragment.app.FragmentActivity>()
        every { biometricAuthManager.authenticate(activity) } returns
                flowOf(AuthResult.Error("Capteur non disponible"))

        viewModel.authenticate(activity)

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertEquals("Capteur non disponible", (viewModel.uiState.value as AuthUiState.Error).message)
    }
}

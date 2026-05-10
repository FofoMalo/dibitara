package com.dibitara.app.presentation.auth

import com.dibitara.app.security.AuthResult
import com.dibitara.app.security.BiometricAuthManager
import com.dibitara.app.security.CredentialManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
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
 * UnconfinedTestDispatcher est utilisé pour que les coroutines du init{}
 * s'exécutent de façon synchrone au moment de la construction du ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val biometricAuthManager: BiometricAuthManager = mockk()
    private val credentialManager: CredentialManager = mockk()
    private lateinit var viewModel: AuthViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Par défaut : PIN configuré, pas de mot de passe
        every { credentialManager.isPinSetup()      } returns true
        every { credentialManager.isPasswordSetup() } returns false
        every { credentialManager.getStoredEmail()  } returns null
        viewModel = AuthViewModel(biometricAuthManager, credentialManager)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `état initial est NeedsSetup quand aucune méthode configurée`() {
        every { credentialManager.isPinSetup()      } returns false
        every { credentialManager.isPasswordSetup() } returns false
        val vm = AuthViewModel(biometricAuthManager, credentialManager)
        assertEquals(AuthUiState.NeedsSetup, vm.uiState.value)
    }

    @Test
    fun `état initial est Idle quand PIN configuré`() {
        // setUp configure isPinSetup = true → Idle attendu
        assertTrue(viewModel.uiState.value is AuthUiState.Idle)
        val idle = viewModel.uiState.value as AuthUiState.Idle
        assertTrue(idle.hasPin)
        assertFalse(idle.hasPassword)
    }

    @Test
    fun `authentification biométrique réussie → état Authenticated`() = runTest {
        val activity = mockk<androidx.fragment.app.FragmentActivity>()
        every { biometricAuthManager.authenticate(activity) } returns flowOf(AuthResult.Success)

        viewModel.authenticate(activity)

        assertEquals(AuthUiState.Authenticated, viewModel.uiState.value)
    }

    @Test
    fun `authentification biométrique annulée → retour à Idle`() = runTest {
        val activity = mockk<androidx.fragment.app.FragmentActivity>()
        every { biometricAuthManager.authenticate(activity) } returns flowOf(AuthResult.Cancelled)

        viewModel.authenticate(activity)

        assertTrue(viewModel.uiState.value is AuthUiState.Idle)
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

    @Test
    fun `verifyPin correct → état Authenticated`() = runTest {
        coEvery { credentialManager.verifyPin("1234") } returns true

        viewModel.verifyPin("1234")

        assertEquals(AuthUiState.Authenticated, viewModel.uiState.value)
    }

    @Test
    fun `verifyPin incorrect → Idle avec pinError`() = runTest {
        coEvery { credentialManager.verifyPin("9999") } returns false

        viewModel.verifyPin("9999")

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Idle)
        assertNotNull((state as AuthUiState.Idle).pinError)
    }

    @Test
    fun `verifyPassword correct → état Authenticated`() = runTest {
        every { credentialManager.isPasswordSetup() } returns true
        every { credentialManager.getStoredEmail()  } returns "test@example.com"
        // Recréer le ViewModel avec mot de passe configuré pour avoir Idle(hasPassword=true)
        val vm = AuthViewModel(biometricAuthManager, credentialManager)
        coEvery { credentialManager.verifyPassword("test@example.com", "monMdp!A1b") } returns true

        vm.verifyPassword("test@example.com", "monMdp!A1b")

        assertEquals(AuthUiState.Authenticated, vm.uiState.value)
    }

    @Test
    fun `verifyPassword incorrect → Idle avec passwordError`() = runTest {
        coEvery { credentialManager.verifyPassword(any(), any()) } returns false

        viewModel.verifyPassword("x@x.com", "mauvaisMdp")

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Idle)
        assertNotNull((state as AuthUiState.Idle).passwordError)
    }

    @Test
    fun `clearPinError efface le message d'erreur`() = runTest {
        coEvery { credentialManager.verifyPin("0000") } returns false
        viewModel.verifyPin("0000")

        viewModel.clearPinError()

        val state = viewModel.uiState.value as? AuthUiState.Idle
        assertNull(state?.pinError)
    }

    @Test
    fun `reinitialiserAccesViaBiometrie réussie efface les credentials et passe à NeedsSetup`() = runTest {
        val activity = mockk<androidx.fragment.app.FragmentActivity>()
        every { biometricAuthManager.authenticate(activity) } returns flowOf(AuthResult.Success)
        every { credentialManager.clearCredentials() } just Runs

        viewModel.reinitialiserAccesViaBiometrie(activity)

        verify { credentialManager.clearCredentials() }
        assertEquals(AuthUiState.NeedsSetup, viewModel.uiState.value)
    }

    @Test
    fun `reinitialiserAccesViaBiometrie annulée → retour à Idle sans effacer les credentials`() = runTest {
        val activity = mockk<androidx.fragment.app.FragmentActivity>()
        every { biometricAuthManager.authenticate(activity) } returns flowOf(AuthResult.Cancelled)

        viewModel.reinitialiserAccesViaBiometrie(activity)

        verify(exactly = 0) { credentialManager.clearCredentials() }
        assertTrue(viewModel.uiState.value is AuthUiState.Idle)
    }
}

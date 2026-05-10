package com.dibitara.app.presentation.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.security.AuthResult
import com.dibitara.app.security.BiometricAuthManager
import com.dibitara.app.security.CredentialManager
import com.dibitara.app.security.TotpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de l'écran de verrouillage.
 *
 * Gère trois méthodes d'authentification :
 *  1. Biométrique (empreinte / visage) via BiometricAuthManager
 *  2. PIN à 4 chiffres via CredentialManager
 *  3. Email + mot de passe local via CredentialManager
 *
 * Au démarrage, si aucun secret n'est configuré → NeedsSetup → l'UI navigue
 * vers SetupAuthScreen pour que l'utilisateur crée un PIN.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val biometricAuthManager: BiometricAuthManager,
    private val credentialManager: CredentialManager,
    private val totpManager: TotpManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Les lectures EncryptedSharedPreferences sont en mémoire après init
            val hasPin      = credentialManager.isPinSetup()
            val hasPassword = credentialManager.isPasswordSetup()
            val email       = credentialManager.getStoredEmail()

            _uiState.value = if (!hasPin && !hasPassword) {
                AuthUiState.NeedsSetup
            } else {
                AuthUiState.Idle(hasPin = hasPin, hasPassword = hasPassword, storedEmail = email)
            }
        }
    }

    /** Lance l'authentification biométrique. */
    fun authenticate(activity: FragmentActivity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            biometricAuthManager.authenticate(activity).collect { result ->
                _uiState.value = when (result) {
                    is AuthResult.Success   -> AuthUiState.Authenticated
                    is AuthResult.Cancelled -> buildIdle()
                    is AuthResult.Failed    -> buildIdle()
                    is AuthResult.Error     -> AuthUiState.Error(result.message)
                }
            }
        }
    }

    /**
     * Vérifie le PIN saisi.
     * Si correct et TOTP activé → [AuthUiState.PendingTotp] (étape 2FA).
     * Si correct sans TOTP → [AuthUiState.Authenticated].
     */
    fun verifyPin(pin: String) {
        viewModelScope.launch {
            val correct = credentialManager.verifyPin(pin)
            _uiState.value = when {
                !correct                      -> buildIdle(pinError = "PIN incorrect — réessayez")
                credentialManager.isTotpSetup() -> AuthUiState.PendingTotp()
                else                          -> AuthUiState.Authenticated
            }
        }
    }

    /** Vérifie l'email et le mot de passe saisis. Même logique TOTP que [verifyPin]. */
    fun verifyPassword(email: String, password: String) {
        viewModelScope.launch {
            val correct = credentialManager.verifyPassword(email, password)
            _uiState.value = when {
                !correct                      -> buildIdle(passwordError = "Email ou mot de passe incorrect")
                credentialManager.isTotpSetup() -> AuthUiState.PendingTotp()
                else                          -> AuthUiState.Authenticated
            }
        }
    }

    /**
     * Vérifie le code TOTP à 6 chiffres.
     * Appelé uniquement depuis l'état [AuthUiState.PendingTotp].
     */
    fun verifyTotp(code: String) {
        val secret = credentialManager.getTotpSecret()
        if (secret == null || totpManager.verify(secret, code)) {
            // Secret absent = TOTP mal configuré, on laisse passer par sécurité
            _uiState.value = AuthUiState.Authenticated
        } else {
            _uiState.value = AuthUiState.PendingTotp(codeError = "Code incorrect — réessayez")
        }
    }

    /** Efface le message d'erreur PIN (appelé quand l'utilisateur recommence à saisir). */
    fun clearPinError() {
        val current = _uiState.value as? AuthUiState.Idle ?: return
        _uiState.value = current.copy(pinError = null)
    }

    /**
     * Récupération d'accès via biométrie.
     *
     * Si l'empreinte/visage est reconnu, les hashes PIN et mot de passe sont effacés
     * et l'état passe à [AuthUiState.NeedsSetup] pour rediriger vers SetupAuthScreen.
     * Les données Room ne sont PAS supprimées — l'utilisateur repart du setup d'accès
     * sans perdre ses données financières.
     */
    fun reinitialiserAccesViaBiometrie(activity: FragmentActivity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            biometricAuthManager.authenticate(activity).collect { result ->
                _uiState.value = when (result) {
                    is AuthResult.Success -> {
                        credentialManager.clearCredentials()
                        AuthUiState.NeedsSetup
                    }
                    is AuthResult.Cancelled -> buildIdle()
                    is AuthResult.Failed    -> buildIdle()
                    is AuthResult.Error     -> AuthUiState.Error(result.message)
                }
            }
        }
    }

    private fun buildIdle(pinError: String? = null, passwordError: String? = null) =
        AuthUiState.Idle(
            hasPin        = credentialManager.isPinSetup(),
            hasPassword   = credentialManager.isPasswordSetup(),
            storedEmail   = credentialManager.getStoredEmail(),
            pinError      = pinError,
            passwordError = passwordError
        )
}

sealed class AuthUiState {
    /** Chargement initial — vérification des secrets en cours. */
    data object Loading : AuthUiState()

    /** Aucune méthode d'auth configurée — premier lancement. */
    data object NeedsSetup : AuthUiState()

    /**
     * En attente de saisie.
     * [hasPin] / [hasPassword] détermine quels modes afficher.
     * [pinError] / [passwordError] transportent les messages d'échec.
     */
    data class Idle(
        val hasPin        : Boolean = false,
        val hasPassword   : Boolean = false,
        val storedEmail   : String? = null,
        val pinError      : String? = null,
        val passwordError : String? = null
    ) : AuthUiState()

    /**
     * PIN ou mot de passe correct, mais TOTP activé — l'utilisateur doit saisir le code 2FA.
     * [codeError] contient le message d'erreur si le dernier code était invalide.
     */
    data class PendingTotp(val codeError: String? = null) : AuthUiState()

    data object Authenticated : AuthUiState()
    data class  Error(val message: String) : AuthUiState()
}

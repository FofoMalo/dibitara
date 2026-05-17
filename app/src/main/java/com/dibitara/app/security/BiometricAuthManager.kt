package com.dibitara.app.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Gère l'authentification biométrique (empreinte, Face ID, PIN).
 *
 * On utilise callbackFlow pour transformer le callback de BiometricPrompt
 * en Flow Kotlin — ce qui permet au ViewModel de l'observer proprement.
 *
 * AUTHENTICATORS : on accepte BIOMETRIC_STRONG (empreinte/face) ET DEVICE_CREDENTIAL
 * (PIN/schéma) pour maximiser la compatibilité des appareils.
 */
class BiometricAuthManager {

    /**
     * Retourne true si l'appareil supporte l'authentification biométrique.
     * À vérifier avant d'afficher l'écran de verrouillage.
     */
    fun canAuthenticate(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Lance le dialogue d'authentification et émet le résultat dans un Flow.
     * Le ViewModel observe ce Flow pour mettre à jour l'UiState.
     */
    fun authenticate(activity: FragmentActivity): Flow<AuthResult> = callbackFlow {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                trySend(AuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    // L'utilisateur a explicitement annulé
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    // Matériel absent ou non configuré → retomber silencieusement sur le PIN
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> trySend(AuthResult.Cancelled)
                    else -> trySend(AuthResult.Error(errString.toString()))
                }
            }

            override fun onAuthenticationFailed() {
                // Tentative échouée (mauvaise empreinte) — ne pas fermer le dialogue
                trySend(AuthResult.Failed)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Accès à Dibitara")
            .setSubtitle("Authentifiez-vous pour accéder à vos données financières")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(promptInfo)

        awaitClose { /* Pas de ressource à libérer */ }
    }
}

sealed class AuthResult {
    data object Success   : AuthResult()
    data object Cancelled : AuthResult()
    data object Failed    : AuthResult()
    data class Error(val message: String) : AuthResult()
}

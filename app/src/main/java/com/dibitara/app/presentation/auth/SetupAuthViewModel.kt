package com.dibitara.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.security.CredentialManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de l'écran de configuration initiale.
 * Gère les deux étapes : création du PIN puis (optionnellement) email + mot de passe.
 */
@HiltViewModel
class SetupAuthViewModel @Inject constructor(
    private val credentialManager: CredentialManager
) : ViewModel() {

    private val _event = MutableSharedFlow<SetupAuthEvent>()
    val event = _event.asSharedFlow()

    /**
     * Enregistre le PIN (4 chiffres).
     * CredentialManager gère l'IO en interne — on appelle directement depuis Main.
     */
    fun setupPin(pin: String) {
        viewModelScope.launch {
            credentialManager.setupPin(pin)
            // La preuve d'installation marque que ce device a fait son propre setup.
            // Elle vit dans noBackupFilesDir et ne sera jamais restaurée sur un autre device.
            credentialManager.createInstallProof()
            _event.emit(SetupAuthEvent.PinSaved)
        }
    }

    /**
     * Enregistre l'email + mot de passe, puis émet SetupComplete.
     */
    fun setupPassword(email: String, password: String) {
        viewModelScope.launch {
            credentialManager.setupPassword(email, password)
            _event.emit(SetupAuthEvent.SetupComplete)
        }
    }

    /**
     * L'utilisateur choisit de ne pas configurer de mot de passe.
     * Le PIN seul suffit comme second facteur.
     */
    fun skipPassword() {
        viewModelScope.launch { _event.emit(SetupAuthEvent.SetupComplete) }
    }
}

sealed class SetupAuthEvent {
    /** PIN enregistré — passer à l'étape 2 (mot de passe). */
    data object PinSaved     : SetupAuthEvent()
    /** Configuration terminée — naviguer vers le tableau de bord. */
    data object SetupComplete : SetupAuthEvent()
}

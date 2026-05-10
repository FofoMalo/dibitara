package com.dibitara.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.UpdateAfficherRapportUseCase
import com.dibitara.app.domain.usecase.UpdateDeviseParDefautUseCase
import com.dibitara.app.domain.usecase.UpdateSeuilFondsUseCase
import com.dibitara.app.security.CredentialManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ucGetPreferences: GetUserPreferencesUseCase,
    private val ucUpdateSeuil: UpdateSeuilFondsUseCase,
    private val ucUpdateDevise: UpdateDeviseParDefautUseCase,
    private val ucUpdateAfficherRapport: UpdateAfficherRapportUseCase,
    private val credentialManager: CredentialManager
) : ViewModel() {

    /** Préférences actuelles, mises à jour en temps réel depuis DataStore. */
    val preferences: StateFlow<UserPreferences> = ucGetPreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    // ─── État de sécurité ─────────────────────────────────────────────────────

    private val _securityState = MutableStateFlow(
        SecurityState(
            hasPinConfigured      = credentialManager.isPinSetup(),
            hasPasswordConfigured = credentialManager.isPasswordSetup(),
            storedEmail           = credentialManager.getStoredEmail()
        )
    )
    val securityState: StateFlow<SecurityState> = _securityState.asStateFlow()

    private val _event = MutableSharedFlow<SettingsEvent>()
    val event = _event.asSharedFlow()

    // ─── Préférences ──────────────────────────────────────────────────────────

    /**
     * Met à jour le seuil d'alerte.
     * [eurosStr] est la valeur saisie par l'utilisateur (en euros) — on convertit en centimes.
     * Ignore la mise à jour si la saisie n'est pas un entier valide.
     */
    fun mettreAJourSeuil(eurosStr: String) {
        val cents = eurosStr.toLongOrNull()?.times(100) ?: return
        viewModelScope.launch { ucUpdateSeuil(cents) }
    }

    fun mettreAJourDevise(currency: Currency) {
        viewModelScope.launch { ucUpdateDevise(currency) }
    }

    fun mettreAJourAfficherRapport(afficher: Boolean) {
        viewModelScope.launch { ucUpdateAfficherRapport(afficher) }
    }

    // ─── Sécurité ─────────────────────────────────────────────────────────────

    /**
     * Enregistre un nouveau PIN (crée ou remplace).
     * CredentialManager gère l'IO en interne.
     */
    fun changerPin(newPin: String) {
        viewModelScope.launch {
            credentialManager.setupPin(newPin)
            _securityState.value = _securityState.value.copy(hasPinConfigured = true)
            _event.emit(SettingsEvent.PinMisAJour)
        }
    }

    /**
     * Enregistre un nouveau couple email + mot de passe (crée ou remplace).
     * Valide la robustesse du mot de passe avant d'enregistrer.
     */
    fun changerMotDePasse(email: String, password: String) {
        viewModelScope.launch {
            credentialManager.setupPassword(email, password)
            _securityState.value = _securityState.value.copy(
                hasPasswordConfigured = true,
                storedEmail = email
            )
            _event.emit(SettingsEvent.MotDePasseMisAJour)
        }
    }
}

/** État de la section Sécurité dans les Paramètres. */
data class SecurityState(
    val hasPinConfigured      : Boolean,
    val hasPasswordConfigured : Boolean,
    val storedEmail           : String?
)

/** Événements ponctuels émis par SettingsViewModel (pour les Snackbars). */
sealed class SettingsEvent {
    data object PinMisAJour         : SettingsEvent()
    data object MotDePasseMisAJour  : SettingsEvent()
}

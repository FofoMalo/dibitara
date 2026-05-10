package com.dibitara.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.UpdateAfficherEpargneUseCase
import com.dibitara.app.domain.usecase.UpdateAfficherInvestissementsUseCase
import com.dibitara.app.domain.usecase.UpdateAfficherRapportUseCase
import com.dibitara.app.domain.usecase.UpdateDeviseParDefautUseCase
import com.dibitara.app.domain.usecase.UpdateSeuilFondsUseCase
import com.dibitara.app.domain.usecase.UpdateTwoFactorEnabledUseCase
import com.dibitara.app.security.CredentialManager
import com.dibitara.app.security.TotpManager
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
    private val ucUpdateAfficherEpargne: UpdateAfficherEpargneUseCase,
    private val ucUpdateAfficherInvestissements: UpdateAfficherInvestissementsUseCase,
    private val ucUpdateTwoFactorEnabled: UpdateTwoFactorEnabledUseCase,
    private val credentialManager: CredentialManager,
    private val totpManager: TotpManager
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
            storedEmail           = credentialManager.getStoredEmail(),
            hasTotpConfigured     = credentialManager.isTotpSetup()
        )
    )
    val securityState: StateFlow<SecurityState> = _securityState.asStateFlow()

    // État de la configuration TOTP en cours — null si aucun setup ouvert
    private val _totpSetupState = MutableStateFlow<TotpSetupUiState?>(null)
    val totpSetupState: StateFlow<TotpSetupUiState?> = _totpSetupState.asStateFlow()

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

    fun mettreAJourAfficherEpargne(afficher: Boolean) {
        viewModelScope.launch { ucUpdateAfficherEpargne(afficher) }
    }

    fun mettreAJourAfficherInvestissements(afficher: Boolean) {
        viewModelScope.launch { ucUpdateAfficherInvestissements(afficher) }
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

    // ─── TOTP (2FA) ───────────────────────────────────────────────────────────

    /**
     * Génère un nouveau secret TOTP et ouvre le dialogue de configuration.
     * L'utilisateur devra scanner le QR code et valider avec un code avant activation.
     */
    fun preparerSetupTotp() {
        val email  = credentialManager.getStoredEmail() ?: "utilisateur"
        val secret = totpManager.generateSecret()
        val uri    = totpManager.buildOtpAuthUri(secret, email)
        _totpSetupState.value = TotpSetupUiState(secret = secret, uri = uri)
    }

    /**
     * Valide le code saisi par l'utilisateur et active le TOTP si correct.
     * Si le code est invalide, [TotpSetupUiState.codeError] est renseigné.
     */
    fun activerTotp(code: String) {
        val state = _totpSetupState.value ?: return
        if (!totpManager.verify(state.secret, code)) {
            _totpSetupState.value = state.copy(codeError = "Code incorrect — réessayez")
            return
        }
        viewModelScope.launch {
            credentialManager.setupTotp(state.secret)
            ucUpdateTwoFactorEnabled(true)
            _totpSetupState.value = null
            _securityState.value  = _securityState.value.copy(hasTotpConfigured = true)
            _event.emit(SettingsEvent.TotpActive)
        }
    }

    /** Ferme le dialogue de configuration sans rien enregistrer. */
    fun annulerSetupTotp() {
        _totpSetupState.value = null
    }

    /** Efface le secret TOTP et désactive la double authentification. */
    fun desactiverTotp() {
        viewModelScope.launch {
            credentialManager.clearTotp()
            ucUpdateTwoFactorEnabled(false)
            _securityState.value = _securityState.value.copy(hasTotpConfigured = false)
            _event.emit(SettingsEvent.TotpDesactive)
        }
    }
}

/** État de la section Sécurité dans les Paramètres. */
data class SecurityState(
    val hasPinConfigured      : Boolean,
    val hasPasswordConfigured : Boolean,
    val storedEmail           : String?,
    val hasTotpConfigured     : Boolean = false
)

/** État intermédiaire pendant la configuration du TOTP — visible dans le dialogue. */
data class TotpSetupUiState(
    val secret    : String,
    val uri       : String,
    val codeError : String? = null
)

/** Événements ponctuels émis par SettingsViewModel (pour les Snackbars). */
sealed class SettingsEvent {
    data object PinMisAJour         : SettingsEvent()
    data object MotDePasseMisAJour  : SettingsEvent()
    data object TotpActive          : SettingsEvent()
    data object TotpDesactive       : SettingsEvent()
}

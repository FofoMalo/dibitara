package com.dibitara.app.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dibitara.app.data.worker.MonthlyReportNotificationWorker
import com.dibitara.app.data.worker.WeeklyRecapWorker
import com.dibitara.app.domain.model.Currency
import dagger.hilt.android.qualifiers.ApplicationContext
import com.dibitara.app.domain.model.ExchangeRates
import com.dibitara.app.domain.model.ExportFormat
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.usecase.ExporterDonneesUseCase
import com.dibitara.app.domain.usecase.GetExchangeRatesUseCase
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.UpdateAfficherEpargneUseCase
import com.dibitara.app.domain.usecase.UpdateAfficherInvestissementsUseCase
import com.dibitara.app.domain.usecase.UpdateAfficherProchainsPaiementsUseCase
import com.dibitara.app.domain.usecase.UpdateAfficherRapportUseCase
import com.dibitara.app.domain.usecase.UpdateDeviseParDefautUseCase
import com.dibitara.app.domain.usecase.RestaurerDonneesUseCase
import com.dibitara.app.domain.usecase.SupprimerToutesDonneesUseCase
import com.dibitara.app.domain.usecase.UpdateAfficherRecommandationsUseCase
import com.dibitara.app.domain.usecase.UpdateNotificationsMensuellesUseCase
import com.dibitara.app.domain.usecase.UpdateSeuilFondsUseCase
import com.dibitara.app.domain.usecase.UpdateTwoFactorEnabledUseCase
import java.util.concurrent.TimeUnit
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
    @ApplicationContext private val context: Context,
    private val ucGetPreferences: GetUserPreferencesUseCase,
    private val ucGetExchangeRates: GetExchangeRatesUseCase,
    private val ucUpdateSeuil: UpdateSeuilFondsUseCase,
    private val ucUpdateDevise: UpdateDeviseParDefautUseCase,
    private val ucUpdateAfficherRapport: UpdateAfficherRapportUseCase,
    private val ucUpdateAfficherEpargne: UpdateAfficherEpargneUseCase,
    private val ucUpdateAfficherInvestissements: UpdateAfficherInvestissementsUseCase,
    private val ucUpdateAfficherProchainsPaiements: UpdateAfficherProchainsPaiementsUseCase,
    private val ucUpdateTwoFactorEnabled: UpdateTwoFactorEnabledUseCase,
    private val ucUpdateNotificationsMensuelles: UpdateNotificationsMensuellesUseCase,
    private val ucUpdateAfficherRecommandations: UpdateAfficherRecommandationsUseCase,
    private val ucSupprimerToutesDonnees: SupprimerToutesDonneesUseCase,
    private val ucExporterDonnees: ExporterDonneesUseCase,
    private val ucRestaurerDonnees: RestaurerDonneesUseCase,
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

    // ─── Taux de change ───────────────────────────────────────────────────────

    /** null = chargement en cours, Failure = erreur réseau, Success = taux disponibles */
    private val _tauxDeChange = MutableStateFlow<Result<ExchangeRates>?>(null)
    val tauxDeChange: StateFlow<Result<ExchangeRates>?> = _tauxDeChange.asStateFlow()

    init {
        rafraichirTauxDeChange()
    }

    fun rafraichirTauxDeChange() {
        viewModelScope.launch {
            _tauxDeChange.value = ucGetExchangeRates()
        }
    }

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

    // ─── Export des données ───────────────────────────────────────────────────

    /** true pendant la génération du fichier pour désactiver le bouton. */
    private val _exportEnCours = MutableStateFlow(false)
    val exportEnCours: StateFlow<Boolean> = _exportEnCours.asStateFlow()

    private val _exportEvent = MutableSharedFlow<ExportEvent>()
    val exportEvent = _exportEvent.asSharedFlow()

    // ─── Restauration des données ──────────────────────────────────────────────

    /** true pendant la lecture et l'insertion du fichier de sauvegarde. */
    private val _restoreEnCours = MutableStateFlow(false)
    val restoreEnCours: StateFlow<Boolean> = _restoreEnCours.asStateFlow()

    private val _restoreEvent = MutableSharedFlow<RestoreEvent>()
    val restoreEvent = _restoreEvent.asSharedFlow()

    /**
     * Restaure les données depuis le fichier JSON sélectionné par l'utilisateur.
     * Émet [RestoreEvent.Succes] avec le nombre d'entités restaurées,
     * ou [RestoreEvent.Erreur] si le fichier est invalide.
     */
    fun restaurerDonnees(uri: Uri) {
        viewModelScope.launch {
            _restoreEnCours.value = true
            try {
                val result = ucRestaurerDonnees(uri)
                when (result) {
                    is com.dibitara.app.domain.repository.RestoreResult.Success ->
                        _restoreEvent.emit(RestoreEvent.Succes(result.nbElements))
                    is com.dibitara.app.domain.repository.RestoreResult.Error ->
                        _restoreEvent.emit(RestoreEvent.Erreur(result.message))
                }
            } catch (e: Exception) {
                _restoreEvent.emit(RestoreEvent.Erreur(e.message ?: "Erreur inconnue"))
            } finally {
                _restoreEnCours.value = false
            }
        }
    }

    /**
     * Lance la collecte et l'écriture du fichier en arrière-plan.
     * Émet [ExportEvent.Succes] avec l'Uri FileProvider, ou [ExportEvent.Erreur] si ça échoue.
     */
    fun exporterDonnees(format: ExportFormat) {
        viewModelScope.launch {
            _exportEnCours.value = true
            try {
                val uri = ucExporterDonnees(format)
                _exportEvent.emit(ExportEvent.Succes(uri, format))
            } catch (e: Exception) {
                _exportEvent.emit(ExportEvent.Erreur)
            } finally {
                _exportEnCours.value = false
            }
        }
    }

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

    fun mettreAJourAfficherProchainsPaiements(afficher: Boolean) {
        viewModelScope.launch { ucUpdateAfficherProchainsPaiements(afficher) }
    }

    /**
     * Active ou désactive les notifications mensuelles.
     * Si activé : planifie un [MonthlyReportNotificationWorker] tous les 30 jours.
     * Si désactivé : annule le travail planifié.
     */
    fun mettreAJourAfficherRecommandations(afficher: Boolean) {
        viewModelScope.launch { ucUpdateAfficherRecommandations(afficher) }
    }

    /**
     * Efface définitivement toutes les données personnelles (RGPD Art. 17).
     * Après l'appel, l'appelant doit naviguer vers SetupAuth —
     * les credentials n'existent plus.
     */
    fun supprimerToutesDonnees(onTermine: () -> Unit) {
        viewModelScope.launch {
            ucSupprimerToutesDonnees()
            onTermine()
        }
    }

    /**
     * Active ou désactive les notifications mensuelles.
     * Si activé : planifie un [MonthlyReportNotificationWorker] tous les 30 jours.
     * Si désactivé : annule le travail planifié.
     */
    fun mettreAJourNotificationsMensuelles(enabled: Boolean) {
        viewModelScope.launch {
            ucUpdateNotificationsMensuelles(enabled)
            val workManager = WorkManager.getInstance(context)
            if (enabled) {
                val request = PeriodicWorkRequestBuilder<MonthlyReportNotificationWorker>(
                    30, TimeUnit.DAYS
                ).build()
                workManager.enqueueUniquePeriodicWork(
                    MonthlyReportNotificationWorker.NOM_TRAVAIL_UNIQUE,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
                val weeklyRequest = PeriodicWorkRequestBuilder<WeeklyRecapWorker>(
                    7, TimeUnit.DAYS
                ).build()
                workManager.enqueueUniquePeriodicWork(
                    WeeklyRecapWorker.NOM_TRAVAIL_UNIQUE,
                    ExistingPeriodicWorkPolicy.KEEP,
                    weeklyRequest
                )
            } else {
                workManager.cancelUniqueWork(MonthlyReportNotificationWorker.NOM_TRAVAIL_UNIQUE)
                workManager.cancelUniqueWork(WeeklyRecapWorker.NOM_TRAVAIL_UNIQUE)
            }
        }
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

/** Résultat de l'opération d'export. */
sealed class ExportEvent {
    /** Fichier prêt — [uri] à passer à Intent.ACTION_SEND, [format] pour déterminer le mimeType. */
    data class Succes(val uri: Uri, val format: ExportFormat) : ExportEvent()
    data object Erreur : ExportEvent()
}

/** Résultat de l'opération de restauration. */
sealed class RestoreEvent {
    data class Succes(val nbElements: Int) : RestoreEvent()
    data class Erreur(val message: String) : RestoreEvent()
}

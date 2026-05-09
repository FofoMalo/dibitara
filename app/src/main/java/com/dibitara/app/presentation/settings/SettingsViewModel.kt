package com.dibitara.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.UserPreferences
import com.dibitara.app.domain.usecase.GetUserPreferencesUseCase
import com.dibitara.app.domain.usecase.UpdateAfficherRapportUseCase
import com.dibitara.app.domain.usecase.UpdateDeviseParDefautUseCase
import com.dibitara.app.domain.usecase.UpdateSeuilFondsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ucGetPreferences: GetUserPreferencesUseCase,
    private val ucUpdateSeuil: UpdateSeuilFondsUseCase,
    private val ucUpdateDevise: UpdateDeviseParDefautUseCase,
    private val ucUpdateAfficherRapport: UpdateAfficherRapportUseCase
) : ViewModel() {

    /** Préférences actuelles, mises à jour en temps réel depuis DataStore. */
    val preferences: StateFlow<UserPreferences> = ucGetPreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

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
}

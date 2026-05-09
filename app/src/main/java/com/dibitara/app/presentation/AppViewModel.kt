package com.dibitara.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.usecase.GenerateMonthlyRecurringUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel attaché à MainActivity.
 * Son seul rôle : déclencher les tâches d'initialisation au démarrage de l'app,
 * notamment la génération des transactions récurrentes du mois en cours.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val generateMonthlyRecurring: GenerateMonthlyRecurringUseCase
) : ViewModel() {

    init {
        viewModelScope.launch {
            generateMonthlyRecurring()
        }
    }
}

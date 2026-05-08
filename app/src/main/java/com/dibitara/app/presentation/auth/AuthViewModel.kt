package com.dibitara.app.presentation.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.security.AuthResult
import com.dibitara.app.security.BiometricAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de l'écran de verrouillage.
 * Il orchestre la demande d'authentification et expose l'état à la UI.
 * La logique biométrique est dans BiometricAuthManager — pas ici.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun authenticate(activity: FragmentActivity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            biometricAuthManager.authenticate(activity).collect { result ->
                _uiState.value = when (result) {
                    is AuthResult.Success   -> AuthUiState.Authenticated
                    is AuthResult.Cancelled -> AuthUiState.Idle
                    is AuthResult.Failed    -> AuthUiState.Idle
                    is AuthResult.Error     -> AuthUiState.Error(result.message)
                }
            }
        }
    }
}

sealed class AuthUiState {
    data object Idle          : AuthUiState()
    data object Loading       : AuthUiState()
    data object Authenticated : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

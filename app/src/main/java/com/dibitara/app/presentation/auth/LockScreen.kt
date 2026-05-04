package com.dibitara.app.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Écran de verrouillage affiché au lancement de l'application.
 * Dès que l'utilisateur est authentifié, on appelle [onAuthenticated]
 * pour naviguer vers le tableau de bord.
 */
@Composable
fun LockScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as FragmentActivity

    // Dès que l'état passe à Authenticated, on navigue
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) {
            onAuthenticated()
        }
    }

    // Lancer automatiquement l'authentification au premier affichage
    LaunchedEffect(Unit) {
        viewModel.authenticate(activity)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Dibitara",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Vos finances, en sécurité",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(64.dp))

            when (uiState) {
                is AuthUiState.Loading -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )

                is AuthUiState.Error -> {
                    Text(
                        text = (uiState as AuthUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.authenticate(activity) }) {
                        Text("Réessayer")
                    }
                }

                else -> Button(onClick = { viewModel.authenticate(activity) }) {
                    Text("Se connecter")
                }
            }
        }
    }
}

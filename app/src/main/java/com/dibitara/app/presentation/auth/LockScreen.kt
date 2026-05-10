package com.dibitara.app.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Écran de verrouillage.
 *
 * Propose trois méthodes d'authentification, dans l'ordre de priorité :
 *  1. Biométrique — lancé automatiquement à l'ouverture
 *  2. PIN à 4 chiffres (mode par défaut si PIN configuré)
 *  3. Email + mot de passe (accessible via un lien de basculement)
 *
 * [onAuthenticated] est appelé dès qu'une méthode réussit.
 * [onNeedsSetup] est appelé si aucune méthode n'est encore configurée.
 */
@Composable
fun LockScreen(
    onAuthenticated: () -> Unit,
    onNeedsSetup: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as FragmentActivity

    // Réagir aux changements d'état globaux (navigation)
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Authenticated -> onAuthenticated()
            is AuthUiState.NeedsSetup    -> onNeedsSetup()
            else -> {}
        }
    }

    // Lancer la biométrie automatiquement dès que l'état Idle est prêt
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Idle) {
            viewModel.authenticate(activity)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            is AuthUiState.Loading       -> LoadingContent()
            is AuthUiState.NeedsSetup    -> LoadingContent() // transition rapide vers SetupAuth
            is AuthUiState.Authenticated -> LoadingContent() // transition rapide vers Dashboard

            is AuthUiState.Idle -> IdleContent(
                state                      = state,
                activity                   = activity,
                onBiometrics               = { viewModel.authenticate(activity) },
                onPinDigit                 = { viewModel.clearPinError() },
                onPinComplete              = { pin -> viewModel.verifyPin(pin) },
                onPasswordSubmit           = { email, pwd -> viewModel.verifyPassword(email, pwd) },
                onRecuperationViaBiometrie = { viewModel.reinitialiserAccesViaBiometrie(activity) }
            )

            is AuthUiState.PendingTotp -> PendingTotpContent(
                codeError      = state.codeError,
                onCodeComplete = { code -> viewModel.verifyTotp(code) }
            )

            is AuthUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = { viewModel.authenticate(activity) }
            )
        }
    }
}

// ─── Contenu : état Idle (saisie en attente) ─────────────────────────────────

@Composable
private fun IdleContent(
    state: AuthUiState.Idle,
    activity: FragmentActivity,
    onBiometrics: () -> Unit,
    onPinDigit: (String) -> Unit,
    onPinComplete: (String) -> Unit,
    onPasswordSubmit: (String, String) -> Unit,
    onRecuperationViaBiometrie: () -> Unit
) {
    // Mode courant : PIN ou mot de passe
    var modeMdp by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Dibitara", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            "Vos finances, en sécurité",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(48.dp))

        if (!modeMdp && state.hasPin) {
            // ── Mode PIN ──────────────────────────────────────────────────────
            ModePinContent(
                pinError  = state.pinError,
                onComplete = onPinComplete
            )

            Spacer(Modifier.height(24.dp))

            // Bouton biométrique
            IconButton(onClick = onBiometrics, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = "Biométrie",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (state.hasPassword) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { modeMdp = true }) {
                    Text("Utiliser un mot de passe")
                }
            }
        } else {
            // ── Mode mot de passe ─────────────────────────────────────────────
            ModeMotDePasseContent(
                emailPreRempli = state.storedEmail ?: "",
                passwordError  = state.passwordError,
                onSubmit       = onPasswordSubmit
            )

            Spacer(Modifier.height(16.dp))

            IconButton(onClick = onBiometrics, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = "Biométrie",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (state.hasPin) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { modeMdp = false }) {
                    Text("Utiliser le PIN")
                }
            }
        }

        // Lien de récupération — toujours visible en bas
        Spacer(Modifier.height(32.dp))
        TextButton(onClick = { showRecoveryDialog = true }) {
            Text(
                "Accès oublié ?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
    }

    // Dialogue de confirmation avant déclenchement biométrique
    if (showRecoveryDialog) {
        DialogueRecuperationAcces(
            onConfirm = {
                showRecoveryDialog = false
                onRecuperationViaBiometrie()
            },
            onDismiss = { showRecoveryDialog = false }
        )
    }
}

// ─── Saisie PIN ───────────────────────────────────────────────────────────────

@Composable
private fun ModePinContent(
    pinError: String?,
    onComplete: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Entrez votre PIN", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))

        PinDots(longueur = pin.length)

        pinError?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            // Vider le PIN après une erreur
            LaunchedEffect(it) { pin = "" }
        }

        Spacer(Modifier.height(24.dp))

        ClavierNumerique(
            onChiffre = { digit ->
                if (pin.length < 4) {
                    pin += digit
                    if (pin.length == 4) {
                        onComplete(pin)
                    }
                }
            },
            onEffacer = { pin = pin.dropLast(1) }
        )
    }
}

// ─── Saisie email + mot de passe ─────────────────────────────────────────────

@Composable
private fun ModeMotDePasseContent(
    emailPreRempli: String,
    passwordError: String?,
    onSubmit: (String, String) -> Unit
) {
    var email   by remember { mutableStateOf(emailPreRempli) }
    var mdp     by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Connexion par mot de passe", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = mdp,
            onValueChange = { mdp = it },
            label = { Text("Mot de passe") },
            singleLine = true,
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it) } },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { onSubmit(email.trim(), mdp) },
            enabled = email.isNotBlank() && mdp.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Se connecter") }
    }
}

// ─── Dialogue de récupération d'accès ────────────────────────────────────────

@Composable
private fun DialogueRecuperationAcces(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Récupération de l'accès") },
        text = {
            Text(
                "Votre empreinte ou reconnaissance faciale est nécessaire pour réinitialiser " +
                "votre PIN et mot de passe.\n\n" +
                "Vos données financières ne seront pas supprimées."
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Utiliser la biométrie") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

// ─── Saisie code TOTP ────────────────────────────────────────────────────────

@Composable
private fun PendingTotpContent(
    codeError: String?,
    onCodeComplete: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Dibitara", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            "Vos finances, en sécurité",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(48.dp))

        Text("Double authentification", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ouvrez votre application d'authentification et saisissez le code à 6 chiffres.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        PinDots(longueur = code.length, total = 6)

        codeError?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            LaunchedEffect(it) { code = "" }
        }

        Spacer(Modifier.height(24.dp))

        ClavierNumerique(
            onChiffre = { digit ->
                if (code.length < 6) {
                    code += digit
                    if (code.length == 6) onCodeComplete(code)
                }
            },
            onEffacer = { code = code.dropLast(1) }
        )
    }
}

// ─── États transitoires ───────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Réessayer") }
    }
}

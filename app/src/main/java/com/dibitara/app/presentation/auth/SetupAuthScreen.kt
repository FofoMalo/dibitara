package com.dibitara.app.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Écran de configuration initiale — affiché une seule fois au premier lancement.
 * Seul le PIN est configuré ici ; l'email/mot de passe n'a pas sa place sans backend.
 *
 * [onSetupComplete] est appelé dès que le PIN est créé.
 */
@Composable
fun SetupAuthScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupAuthViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                // PIN sauvegardé → configuration terminée, pas d'étape 2
                is SetupAuthEvent.PinSaved      -> viewModel.skipPassword()
                is SetupAuthEvent.SetupComplete -> onSetupComplete()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Dibitara", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Configuration de la sécurité",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(40.dp))

            EtapePinSetup(onPinConfirmed = { pin -> viewModel.setupPin(pin) })
        }
    }
}

// ─── Étape 1 : PIN ───────────────────────────────────────────────────────────

@Composable
private fun EtapePinSetup(onPinConfirmed: (String) -> Unit) {
    // Phase A : saisir le PIN ; Phase B : confirmer le PIN
    var phase   by remember { mutableStateOf("saisie") }
    var pin     by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var erreur  by remember { mutableStateOf<String?>(null) }

    val pinCourant = if (phase == "saisie") pin else confirm

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (phase == "saisie") "Créer un PIN" else "Confirmer le PIN",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (phase == "saisie") "Choisissez un code à 4 chiffres."
                   else "Saisissez à nouveau votre PIN pour confirmer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // 4 points — pleins si un chiffre est saisi, vides sinon
        PinDots(longueur = pinCourant.length)

        erreur?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(32.dp))

        ClavierNumerique(
            onChiffre = { chiffre ->
                erreur = null
                if (pinCourant.length < 4) {
                    if (phase == "saisie") pin += chiffre else confirm += chiffre

                    // Auto-validation à 4 chiffres
                    val courant = if (phase == "saisie") pin else confirm
                    if (courant.length == 4) {
                        if (phase == "saisie") {
                            phase = "confirmation"
                        } else {
                            if (pin == confirm) {
                                onPinConfirmed(pin)
                            } else {
                                erreur = "Les PIN ne correspondent pas — recommencez"
                                pin = ""; confirm = ""; phase = "saisie"
                            }
                        }
                    }
                }
            },
            onEffacer = {
                erreur = null
                if (phase == "saisie") pin = pin.dropLast(1) else confirm = confirm.dropLast(1)
            }
        )
    }
}

// ─── Étape 2 : Email + mot de passe ─────────────────────────────────────────

@Composable
private fun EtapeMotDePasseSetup(
    onPasswordSaved: (email: String, password: String) -> Unit,
    onSkip: () -> Unit
) {
    var email          by remember { mutableStateOf("") }
    var motDePasse     by remember { mutableStateOf("") }
    var visible        by remember { mutableStateOf(false) }
    var emailErreur    by remember { mutableStateOf<String?>(null) }

    val criteres = passwordCriteria(motDePasse)
    val toutValide = email.contains("@") && criteres.all { it.second }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Mot de passe (optionnel)", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ajoutez un email et un mot de passe comme alternative au PIN.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailErreur = null },
            label = { Text("Email") },
            isError = emailErreur != null,
            supportingText = emailErreur?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = motDePasse,
            onValueChange = { motDePasse = it },
            label = { Text("Mot de passe") },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (visible) "Masquer" else "Afficher"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Indicateurs de robustesse du mot de passe
        criteres.forEach { (label, valide) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (valide) Icons.Filled.CheckCircle else Icons.Filled.Close,
                    contentDescription = null,
                    tint = if (valide) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                if (!email.contains("@")) {
                    emailErreur = "Email invalide"
                } else {
                    onPasswordSaved(email.trim(), motDePasse)
                }
            },
            enabled = toutValide,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Enregistrer") }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("Ignorer cette étape")
        }
    }
}

// ─── Composants partagés ────────────────────────────────────────────────────

/** Cercles indiquant la progression de la saisie — 4 pour un PIN, 6 pour un code TOTP. */
@Composable
fun PinDots(longueur: Int, total: Int = 4) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(total) { index ->
            val rempli = index < longueur
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (rempli) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(18.dp)
            ) {}
        }
    }
}

/** Clavier numérique 3×3 + 0 + effacement arrière. */
@Composable
fun ClavierNumerique(onChiffre: (String) -> Unit, onEffacer: () -> Unit) {
    val lignes = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lignes.forEach { ligne ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ligne.forEach { chiffre ->
                    ToucheClavier(label = chiffre, onClick = { onChiffre(chiffre) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Espace vide à gauche du 0
            Spacer(Modifier.size(80.dp))
            ToucheClavier(label = "0", onClick = { onChiffre("0") })
            ToucheClavier(label = "⌫", onClick = onEffacer)
        }
    }
}

@Composable
private fun ToucheClavier(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, style = MaterialTheme.typography.headlineSmall)
    }
}

/** Retourne la liste des critères de robustesse avec leur état (validé ou non). */
fun passwordCriteria(password: String): List<Pair<String, Boolean>> = listOf(
    "Au moins 12 caractères"     to (password.length >= 12),
    "Au moins une majuscule"     to password.any { it.isUpperCase() },
    "Au moins un chiffre"        to password.any { it.isDigit() },
    "Au moins un caractère spécial (!@#\$…)" to password.any { !it.isLetterOrDigit() }
)

package com.dibitara.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.presentation.auth.ClavierNumerique
import com.dibitara.app.presentation.auth.PinDots
import com.dibitara.app.presentation.auth.passwordCriteria

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsState()
    val security by viewModel.securityState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var seuilEuros by remember(prefs.seuilFondsCents) {
        mutableStateOf((prefs.seuilFondsCents / 100).toString())
    }

    // Dialogues de sécurité
    var showChangerPin by remember { mutableStateOf(false) }
    var showChangerMdp by remember { mutableStateOf(false) }

    // Écouter les événements du ViewModel pour les Snackbars
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            val message = when (event) {
                is SettingsEvent.PinMisAJour        -> "PIN mis à jour"
                is SettingsEvent.MotDePasseMisAJour -> "Mot de passe mis à jour"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Paramètres", style = MaterialTheme.typography.headlineMedium)

            // ─── Section notifications ────────────────────────────────────────
            SectionCard(titre = "Notifications") {
                Text("Seuil d'alerte — liquidités insuffisantes", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Une alerte est envoyée si le solde du mois passe sous ce montant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = seuilEuros,
                        onValueChange = { seuilEuros = it },
                        label = { Text("Seuil (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { viewModel.mettreAJourSeuil(seuilEuros) },
                        modifier = Modifier.padding(top = 4.dp)
                    ) { Text("Appliquer") }
                }
            }

            // ─── Section tableau de bord ──────────────────────────────────────
            SectionCard(titre = "Tableau de bord") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rapport mensuel", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Remplace le graphique des 6 derniers mois.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.afficherRapportMensuel,
                        onCheckedChange = { viewModel.mettreAJourAfficherRapport(it) }
                    )
                }
            }

            // ─── Section devise ───────────────────────────────────────────────
            SectionCard(titre = "Devise par défaut") {
                Text(
                    "Devise utilisée à la saisie des nouvelles transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                val devises = listOf(Currency.EUR, Currency.USD, Currency.XOF)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    devises.forEach { devise ->
                        FilterChip(
                            selected = prefs.deviseParDefaut == devise,
                            onClick  = { viewModel.mettreAJourDevise(devise) },
                            label    = { Text("${devise.symbol} ${devise.isoCode}") }
                        )
                    }
                }
            }

            // ─── Section sécurité ─────────────────────────────────────────────
            SectionCard(titre = "Sécurité") {
                // PIN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Code PIN", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (security.hasPinConfigured) "PIN configuré" else "Aucun PIN configuré",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showChangerPin = true }) {
                        Text(if (security.hasPinConfigured) "Modifier" else "Configurer")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Email + mot de passe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mot de passe", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (security.hasPasswordConfigured) security.storedEmail ?: "Configuré"
                            else "Aucun mot de passe configuré",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showChangerMdp = true }) {
                        Text(if (security.hasPasswordConfigured) "Modifier" else "Configurer")
                    }
                }
            }
        }
    }

    // ─── Dialogue PIN ─────────────────────────────────────────────────────────
    if (showChangerPin) {
        DialogueChangerPin(
            onConfirmed = { newPin ->
                viewModel.changerPin(newPin)
                showChangerPin = false
            },
            onDismiss = { showChangerPin = false }
        )
    }

    // ─── Dialogue mot de passe ────────────────────────────────────────────────
    if (showChangerMdp) {
        DialogueChangerMotDePasse(
            emailActuel = security.storedEmail ?: "",
            onConfirmed = { email, pwd ->
                viewModel.changerMotDePasse(email, pwd)
                showChangerMdp = false
            },
            onDismiss = { showChangerMdp = false }
        )
    }
}

// ─── Dialogue : changement de PIN ─────────────────────────────────────────────

@Composable
private fun DialogueChangerPin(
    onConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin     by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var phase   by remember { mutableStateOf("saisie") }
    var erreur  by remember { mutableStateOf<String?>(null) }

    val pinCourant = if (phase == "saisie") pin else confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (phase == "saisie") "Nouveau PIN" else "Confirmer le PIN") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (phase == "saisie") "Choisissez un code à 4 chiffres."
                    else "Saisissez à nouveau votre nouveau PIN.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(16.dp))
                PinDots(longueur = pinCourant.length)
                erreur?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                ClavierNumerique(
                    onChiffre = { digit ->
                        erreur = null
                        if (pinCourant.length < 4) {
                            if (phase == "saisie") pin += digit else confirm += digit
                            val courant = if (phase == "saisie") pin else confirm
                            if (courant.length == 4) {
                                if (phase == "saisie") {
                                    phase = "confirmation"
                                } else {
                                    if (pin == confirm) {
                                        onConfirmed(pin)
                                    } else {
                                        erreur = "Les PIN ne correspondent pas"
                                        pin = ""; confirm = ""; phase = "saisie"
                                    }
                                }
                            }
                        }
                    },
                    onEffacer = {
                        if (phase == "saisie") pin = pin.dropLast(1) else confirm = confirm.dropLast(1)
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

// ─── Dialogue : changement de mot de passe ────────────────────────────────────

@Composable
private fun DialogueChangerMotDePasse(
    emailActuel: String,
    onConfirmed: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var email      by remember { mutableStateOf(emailActuel) }
    var mdp        by remember { mutableStateOf("") }
    var visible    by remember { mutableStateOf(false) }
    var emailErr   by remember { mutableStateOf<String?>(null) }

    val criteres    = passwordCriteria(mdp)
    val toutValide  = email.contains("@") && criteres.all { it.second }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mot de passe") },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; emailErr = null },
                    label = { Text("Email") },
                    isError = emailErr != null,
                    supportingText = emailErr?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = mdp,
                    onValueChange = { mdp = it },
                    label = { Text("Nouveau mot de passe") },
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                criteres.forEach { (label, valide) ->
                    Text(
                        text = "${if (valide) "✓" else "✗"} $label",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (valide) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!email.contains("@")) emailErr = "Email invalide"
                    else onConfirmed(email.trim(), mdp)
                },
                enabled = toutValide
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

// ─── Composant utilitaire ─────────────────────────────────────────────────────

@Composable
private fun SectionCard(titre: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titre, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

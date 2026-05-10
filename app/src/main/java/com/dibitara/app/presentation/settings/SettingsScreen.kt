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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.presentation.auth.ClavierNumerique
import com.dibitara.app.presentation.auth.PinDots
import com.dibitara.app.presentation.auth.passwordCriteria
import com.dibitara.app.presentation.common.QrCodeImage

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsState()
    val security by viewModel.securityState.collectAsState()
    val totpSetupState by viewModel.totpSetupState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var seuilEuros by remember(prefs.seuilFondsCents) {
        mutableStateOf((prefs.seuilFondsCents / 100).toString())
    }

    // Dialogues de sécurité
    var showChangerPin        by remember { mutableStateOf(false) }
    var showChangerMdp        by remember { mutableStateOf(false) }
    var showDesactiverTotp    by remember { mutableStateOf(false) }

    // Écouter les événements du ViewModel pour les Snackbars
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            val message = when (event) {
                is SettingsEvent.PinMisAJour        -> "PIN mis à jour"
                is SettingsEvent.MotDePasseMisAJour -> "Mot de passe mis à jour"
                is SettingsEvent.TotpActive         -> "Double authentification activée"
                is SettingsEvent.TotpDesactive      -> "Double authentification désactivée"
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

            // ─── Section navigation ───────────────────────────────────────────
            SectionCard(titre = "Navigation") {
                Text(
                    "Masquer un onglet le retire de la barre de navigation. Il reste accessible depuis le tableau de bord.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Épargne", style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = prefs.afficherEpargne,
                        onCheckedChange = { viewModel.mettreAJourAfficherEpargne(it) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Placements", style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = prefs.afficherInvestissements,
                        onCheckedChange = { viewModel.mettreAJourAfficherInvestissements(it) }
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Double authentification TOTP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Double authentification", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (security.hasTotpConfigured) "Code TOTP requis à chaque connexion"
                            else "Non activée",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (security.hasTotpConfigured) {
                        TextButton(onClick = { showDesactiverTotp = true }) { Text("Désactiver") }
                    } else {
                        TextButton(onClick = { viewModel.preparerSetupTotp() }) { Text("Configurer") }
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

    // ─── Dialogue setup TOTP ──────────────────────────────────────────────────
    totpSetupState?.let { state ->
        DialogueSetupTotp(
            state     = state,
            onActiver = { code -> viewModel.activerTotp(code) },
            onDismiss = { viewModel.annulerSetupTotp() }
        )
    }

    // ─── Dialogue désactivation TOTP ─────────────────────────────────────────
    if (showDesactiverTotp) {
        AlertDialog(
            onDismissRequest = { showDesactiverTotp = false },
            title = { Text("Désactiver la 2FA") },
            text  = { Text("Le code TOTP ne sera plus demandé à la connexion. Vous pouvez le réactiver à tout moment.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.desactiverTotp()
                        showDesactiverTotp = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Désactiver") }
            },
            dismissButton = {
                TextButton(onClick = { showDesactiverTotp = false }) { Text("Annuler") }
            }
        )
    }
}

// ─── Dialogue : configuration TOTP ────────────────────────────────────────────

@Composable
private fun DialogueSetupTotp(
    state: TotpSetupUiState,
    onActiver: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurer la 2FA") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "1. Scannez ce QR code avec Google Authenticator, Authy ou une application compatible.",
                    style = MaterialTheme.typography.bodySmall
                )

                QrCodeImage(
                    content  = state.uri,
                    taillePx = 400,
                    modifier = Modifier.size(200.dp)
                )

                Text(
                    "Ou saisissez manuellement la clé :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Clé secrète affichée en police monospace pour faciliter la saisie manuelle
                Text(
                    state.secret,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider()

                Text(
                    "2. Saisissez le code à 6 chiffres affiché dans l'application pour valider la configuration.",
                    style = MaterialTheme.typography.bodySmall
                )

                PinDots(longueur = code.length, total = 6)

                state.codeError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    // Vider le champ après une erreur
                    LaunchedEffect(it) { code = "" }
                }

                ClavierNumerique(
                    onChiffre = { digit ->
                        if (code.length < 6) {
                            code += digit
                            if (code.length == 6) onActiver(code)
                        }
                    },
                    onEffacer = { code = code.dropLast(1) }
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
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

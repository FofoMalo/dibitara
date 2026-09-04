package com.dibitara.app.presentation.savings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.shape.CircleShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Child
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.FundingMode
import com.dibitara.app.domain.model.GoalColor
import com.dibitara.app.domain.model.GoalIcon
import com.dibitara.app.domain.model.PlafondDefaut
import com.dibitara.app.domain.model.SavingsAccount
import com.dibitara.app.domain.model.SavingsGoal
import com.dibitara.app.domain.model.SavingsType
import com.dibitara.app.presentation.common.HeroCard
import com.dibitara.app.presentation.common.TrendChip
import com.dibitara.app.presentation.common.accent
import com.dibitara.app.presentation.common.chartIcon
import com.dibitara.app.presentation.common.formatCurrency
import com.dibitara.app.presentation.common.icon
import com.dibitara.app.presentation.common.toCurrencyDisplay

@Composable
fun SavingsScreen(viewModel: SavingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val objectifs by viewModel.objectifs.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddChild by remember { mutableStateOf(false) }
    // Compte à modifier : null = pas d'édition en cours
    var accountToEdit by remember { mutableStateOf<SavingsAccount?>(null) }
    var showAddObjectif by remember { mutableStateOf(false) }
    // Objectif à modifier : null = pas d'édition en cours
    var objectifToEdit by remember { mutableStateOf<SavingsGoal?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is SavingsEvent.Saved             -> {
                    showAddSheet = false
                    accountToEdit = null
                    snackbarHostState.showSnackbar("Compte enregistré")
                }
                is SavingsEvent.Deleted           -> snackbarHostState.showSnackbar("Compte supprimé")
                is SavingsEvent.ChildSaved        -> { showAddChild = false; snackbarHostState.showSnackbar("Enfant ajouté") }
                is SavingsEvent.VersementApplique -> snackbarHostState.showSnackbar("Versement appliqué ✓")
                is SavingsEvent.ObjectifEnregistre -> {
                    showAddObjectif = false
                    objectifToEdit = null
                    snackbarHostState.showSnackbar("Objectif enregistré")
                }
                is SavingsEvent.ObjectifSupprime  -> snackbarHostState.showSnackbar("Objectif supprimé")
                is SavingsEvent.VersementObjectifApplique -> snackbarHostState.showSnackbar(
                    when (event.mensualites) {
                        0    -> "Objectif déjà à jour ce mois-ci"
                        1    -> "Versement enregistré · +${event.montantCents.formatCurrency(event.currency)}"
                        else -> "${event.mensualites} mensualités enregistrées · +${event.montantCents.formatCurrency(event.currency)}"
                    }
                )
                is SavingsEvent.AvertissementPlafond ->
                    snackbarHostState.showSnackbar("Versement appliqué - plafond dépassé sur « ${event.compteLabel} »")
                is SavingsEvent.Error             -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un compte épargne")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is SavingsUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is SavingsUiState.Error ->
                    Text(state.message, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center))
                is SavingsUiState.Success ->
                    SavingsContent(
                        state = state,
                        objectifs = objectifs,
                        onEditAccount = { accountToEdit = it },
                        onDeleteAccount = viewModel::deleteAccount,
                        onAddChild = { showAddChild = true },
                        onDeleteChild = viewModel::removeChild,
                        onAppliquerVersement = viewModel::appliquerVersement,
                        onAddObjectif = { showAddObjectif = true },
                        onEditObjectif = { objectifToEdit = it },
                        onDeleteObjectif = viewModel::deleteObjectif,
                        onVerserObjectif = viewModel::appliquerVersementsObjectif,
                        onAssocierComptes = { child, selectionnes ->
                            viewModel.associerComptesEnfant(
                                child,
                                (state as SavingsUiState.Success).accounts,
                                selectionnes
                            )
                        },
                        getTrend = viewModel::tendancePourActif
                    )
            }
        }
    }

    if (showAddSheet) {
        val children = (uiState as? SavingsUiState.Success)?.children ?: emptyList()
        AddSavingsSheet(
            children = children,
            defaultCurrency = defaultCurrency,
            onSave = { type, label, balance, contribution, currency, childId, plafond, taux ->
                viewModel.saveAccount(type, label, balance, contribution, currency, childId, plafond, taux)
            },
            onDismiss = { showAddSheet = false }
        )
    }

    // Feuille d'édition : s'ouvre quand l'utilisateur tape le crayon sur une carte
    accountToEdit?.let { compte ->
        val children = (uiState as? SavingsUiState.Success)?.children ?: emptyList()
        EditSavingsSheet(
            account = compte,
            children = children,
            onSave = { type, label, balance, contribution, currency, childId, plafond, taux ->
                viewModel.updateAccount(compte, type, label, balance, contribution, currency, childId, plafond, taux)
            },
            onDismiss = { accountToEdit = null }
        )
    }

    if (showAddChild) {
        AddChildDialog(
            onConfirm = viewModel::addChild,
            onDismiss = { showAddChild = false }
        )
    }

    // Comptes épargne disponibles comme source d'un objectif (FundingMode.SOLDE_COMPTE) -
    // même liste que celle affichée sur l'écran, casting nécessaire car ces feuilles
    // vivent hors du `when (state)` (comme accountToEdit ci-dessus).
    val savingsAccountsForObjectif = (uiState as? SavingsUiState.Success)?.accounts ?: emptyList()

    if (showAddObjectif) {
        ObjectifSheet(
            existant = null,
            defaultCurrency = defaultCurrency,
            comptesDisponibles = savingsAccountsForObjectif,
            onSave = { name, target, current, monthly, currency, date, color, icon, sourceAccountId, fundingMode ->
                viewModel.upsertObjectif(null, name, target, current, monthly, currency, date, color, icon, sourceAccountId, fundingMode)
            },
            onDismiss = { showAddObjectif = false }
        )
    }

    objectifToEdit?.let { goal ->
        ObjectifSheet(
            existant = goal,
            defaultCurrency = goal.currency,
            comptesDisponibles = savingsAccountsForObjectif,
            onSave = { name, target, current, monthly, currency, date, color, icon, sourceAccountId, fundingMode ->
                viewModel.upsertObjectif(goal, name, target, current, monthly, currency, date, color, icon, sourceAccountId, fundingMode)
            },
            onDismiss = { objectifToEdit = null }
        )
    }
}

@Composable
private fun SavingsContent(
    state: SavingsUiState.Success,
    objectifs: List<ObjectifUi>,
    onEditAccount: (SavingsAccount) -> Unit,
    onDeleteAccount: (SavingsAccount) -> Unit,
    onAddChild: () -> Unit,
    onDeleteChild: (Child) -> Unit,
    onAppliquerVersement: (SavingsAccount) -> Unit,
    onAddObjectif: () -> Unit,
    onEditObjectif: (SavingsGoal) -> Unit,
    onDeleteObjectif: (SavingsGoal) -> Unit,
    onVerserObjectif: (SavingsGoal, Int) -> Unit,
    onAssocierComptes: (Child, Set<Long>) -> Unit,
    getTrend: suspend (Long) -> Float?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Épargne", style = MaterialTheme.typography.headlineMedium)
        }

        // Résumé global
        item { SavingsHeroCard(state) }

        // Fonds d'urgence (§2) : mois de charges courantes couverts par l'épargne liquide.
        // Repris tel quel du Conseiller patrimoine - masqué s'il n'y a pas de données de charges.
        if (state.chargesMensuellesCents > 0) {
            item { FondsUrgenceCard(state) }
        }

        // Objectifs d'épargne (§3) : projets nommés avec progression et échéance projetée.
        // L'en-tête (titre + "Ajouter") est toujours affiché - même sans objectif, c'est le
        // seul point d'entrée (la section Comptes, elle, a la FAB).
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Objectifs", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onAddObjectif) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ajouter")
                }
            }
        }
        if (objectifs.isEmpty()) {
            item {
                Text("Aucun objectif d'épargne.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(objectifs, key = { "objectif_${it.goal.id}" }) { objectif ->
                ObjectifCard(
                    objectif = objectif,
                    compteLie = state.accounts.firstOrNull { it.id == objectif.goal.sourceAccountId },
                    onEdit = { onEditObjectif(objectif.goal) },
                    onDelete = { onDeleteObjectif(objectif.goal) },
                    onVerser = { n -> onVerserObjectif(objectif.goal, n) }
                )
            }
        }

        // Comptes par type
        if (state.accounts.isNotEmpty()) {
            item { Text("Comptes", style = MaterialTheme.typography.titleMedium) }
            items(state.accounts, key = { "compte_${it.id}" }) { account ->
                SavingsAccountCard(
                    account = account,
                    childName = state.children.find { it.id == account.childId }?.name,
                    versementEnAttente = account.id in state.comptesVersementEnAttente,
                    onEdit = { onEditAccount(account) },
                    onDelete = { onDeleteAccount(account) },
                    onVersement = { onAppliquerVersement(account) },
                    getTrend = getTrend
                )
            }
        }

        // Section enfants
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enfants", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onAddChild) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ajouter")
                }
            }
        }

        if (state.children.isEmpty()) {
            item {
                Text("Aucun enfant enregistré.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(state.children, key = { "enfant_${it.id}" }) { child ->
                ChildCard(
                    child = child,
                    savingsAccounts = state.accounts.filter { it.childId == child.id },
                    tousLesComptes = state.accounts,
                    totalCents = state.totauxParEnfantCents[child.id] ?: 0L,
                    summaryCurrency = state.summaryCurrency,
                    onDelete = { onDeleteChild(child) },
                    onAssocierComptes = { selectionnes -> onAssocierComptes(child, selectionnes) }
                )
            }
        }
    }
}

/**
 * Hero de l'écran Épargne (§8) : total + versements/mois en tête, puis un filet de
 * détails - versé ce mois, taux d'épargne réel vs cible, intérêts annuels estimés -
 * et une conversion secondaire sous le total. Fond neutre (HeroCard), pas d'aplat.
 */
@Composable
private fun SavingsHeroCard(state: SavingsUiState.Success) {
    HeroCard {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total épargne", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.totalEpargneCents.toCurrencyDisplay(state.summaryCurrency),
                        style = MaterialTheme.typography.titleLarge)
                    Text(
                        "≈ ${state.conversionSecondaireCents.toCurrencyDisplay(state.conversionSecondaireCurrency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Versements/mois", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.totalMensuelCents.toCurrencyDisplay(state.summaryCurrency),
                        style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            HeroDetailRow("Versé ce mois-ci", state.totalVerseMoisCents.toCurrencyDisplay(state.summaryCurrency))

            val tauxLabel = state.tauxEpargneReel
                ?.let { "${"%.1f".format(it * 100)} %  (cible ${state.tauxEpargneCiblePct} %)" }
                ?: "— (cible ${state.tauxEpargneCiblePct} %)"
            HeroDetailRow("Taux d'épargne", tauxLabel)

            if (state.interetsAnnuelsCents > 0) {
                HeroDetailRow(
                    "Intérêts / an",
                    "≈ ${state.interetsAnnuelsCents.toCurrencyDisplay(state.summaryCurrency)}",
                    valueColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun HeroDetailRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor)
    }
}

/**
 * Carte "Fonds d'urgence" (§8) : combien de mois de charges courantes l'épargne liquide
 * couvre, cible 6 mois. Valeurs reprises telles quelles du Conseiller patrimoine
 * (liquidités sûres = comptes courants + Livret A/LDDS/Compte courant ; charges =
 * besoins incompressibles + mensualités de dettes hors crédit immobilier).
 */
@Composable
private fun FondsUrgenceCard(state: SavingsUiState.Success) {
    val vertSage = Color(0xFFA8C7A0)
    val moisCouverts = state.liquiditesSuresCents.toFloat() / state.chargesMensuellesCents.toFloat()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Fonds d'urgence", style = MaterialTheme.typography.titleSmall)
            Text(
                "${"%.1f".format(moisCouverts)} mois de charges couverts",
                style = MaterialTheme.typography.titleMedium
            )
            LinearProgressIndicator(
                progress = { (moisCouverts / 6f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = vertSage,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                "cible 6 mois · ${state.liquiditesSuresCents.toCurrencyDisplay(state.summaryCurrency)} disponibles",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Carte d'un objectif d'épargne (§3). En-tête : icône teintée par l'accent + nom +
 * menu ⋮. Barre de progression colorée par l'accent, puis montants (masquables),
 * ratio en % (toujours visible, c'est un ratio), effort mensuel, ligne de projection
 * de date, échéance cible, et bouton « Verser » (mis en avant tant que le versement
 * du mois n'est pas enregistré - même code couleur que les comptes).
 */
@Composable
private fun ObjectifCard(
    objectif: ObjectifUi,
    // Compte source si l'objectif est en FundingMode.SOLDE_COMPTE - résolu par l'appelant
    // (a la liste des comptes), null si non lié ou si le compte a été supprimé entre-temps.
    compteLie: SavingsAccount?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onVerser: (Int) -> Unit
) {
    val goal = objectif.goal
    val projection = objectif.projection
    val accent = goal.colorKey.accent()
    val moisFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH) }
    var showMenu by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var showVersement by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)) {
                    Icon(goal.iconKey.icon(), contentDescription = null,
                        modifier = Modifier.size(24.dp), tint = accent)
                    Text(goal.name, style = MaterialTheme.typography.titleSmall)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; showConfirm = true }
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { goal.progression },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Montants : masquables comme partout ailleurs.
                Text(
                    "${goal.currentAmountCents.toCurrencyDisplay(goal.currency)} / " +
                        goal.targetAmountCents.toCurrencyDisplay(goal.currency),
                    style = MaterialTheme.typography.bodyMedium
                )
                // Ratio : toujours visible, ce n'est pas un montant.
                Text("${(goal.progression * 100).toInt()} %",
                    style = MaterialTheme.typography.bodyMedium, color = accent)
            }

            // Rappel visuel de la source en SOLDE_COMPTE : explique pourquoi il n'y a pas
            // de bouton "Verser" juste en dessous (le montant suit le compte tout seul).
            if (goal.fundingModeEffectif == FundingMode.SOLDE_COMPTE && compteLie != null) {
                Text(
                    "Lié à « ${compteLie.label} »",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (goal.monthlyContributionCents > 0) {
                Text(
                    "+ ${goal.monthlyContributionCents.toCurrencyDisplay(goal.currency)}/mois",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Ligne de projection : atteint / dans les temps / en retard / (rien si non calculable)
            when {
                goal.estAtteint -> Text("Objectif atteint ✓",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary)
                projection.tenable == true -> Text(
                    "Dans les temps · ${projection.dateProjetee!!.format(moisFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                projection.tenable == false -> Text(
                    // `ecartMois` est tronqué au mois : une échéance dépassée de quelques
                    // jours donne 0 → on n'écrit pas « En retard de 0 mois ».
                    if ((projection.ecartMois ?: 0) > 0) "En retard de ${projection.ecartMois} mois"
                    else "En retard sur l'échéance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                "Objectif : ${goal.targetDate.format(moisFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Bouton versement : visible si une mensualité est définie. Mis en avant
            // (puce + bordure or) tant que le versement du mois n'est pas enregistré.
            // Masqué en SOLDE_COMPTE (Q3 du cadrage) : le montant suit le compte lié tout
            // seul, ce bouton n'y aurait aucun effet - piège UX identique à celui déjà vu
            // sur le mouvement de capital CTO/immobilier (bouton qui ne fait rien).
            if (goal.monthlyContributionCents > 0 && goal.fundingModeEffectif != FundingMode.SOLDE_COMPTE) {
                val enAttente = objectif.versementEnAttente
                OutlinedButton(
                    onClick = { showVersement = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = if (enAttente)
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else ButtonDefaults.outlinedButtonBorder
                ) {
                    if (enAttente) {
                        Box(
                            Modifier.size(8.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Verser")
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Supprimer « ${goal.name} » ?") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Annuler") } }
        )
    }

    if (showVersement) {
        VersementObjectifDialog(
            goal = goal,
            onConfirm = { n -> onVerser(n); showVersement = false },
            onDismiss = { showVersement = false }
        )
    }
}

/**
 * Feuille « Verser sur un objectif » avec **rattrapage** : l'utilisateur choisit
 * combien de mensualités enregistrer (mois courant + mois manqués). Le total affiché
 * est indicatif - `AppliquerVersementsObjectifUseCase` saute les mois déjà couverts,
 * donc le crédit réel peut être inférieur (retour dans le Snackbar).
 */
@Composable
private fun VersementObjectifDialog(
    goal: SavingsGoal,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var nb by remember { mutableStateOf(1) }
    val total = goal.monthlyContributionCents * nb

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verser sur « ${goal.name} »") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Combien de mensualités enregistrer ? (mois courant + rattrapage des mois manqués)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (nb > 1) nb-- }, enabled = nb > 1) {
                        Icon(Icons.Filled.Remove, contentDescription = "Moins")
                    }
                    Text("$nb", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { if (nb < 12) nb++ }, enabled = nb < 12) {
                        Icon(Icons.Filled.Add, contentDescription = "Plus")
                    }
                }
                Text(
                    "Jusqu'à + ${total.formatCurrency(goal.currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Les mois déjà enregistrés sont ignorés.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(nb) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun SavingsAccountCard(
    account: SavingsAccount,
    childName: String?,
    versementEnAttente: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onVersement: () -> Unit,
    getTrend: suspend (Long) -> Float?
) {
    var showConfirm by remember { mutableStateOf(false) }
    var showVersementConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var trendPct by remember(account.id, account.updatedAt) { mutableStateOf<Float?>(null) }
    LaunchedEffect(account.id, account.updatedAt) { trendPct = getTrend(account.id) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            account.type.chartIcon(), contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SavingsTypeChip(
                            type       = account.type,
                            customName = if (account.type == SavingsType.AUTRE) account.label else null
                        )
                        // Quand type == AUTRE, le label EST le nom du type - pas de doublon
                        if (account.type != SavingsType.AUTRE) {
                            Text(account.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (childName != null) {
                        Text("Pour $childName", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Solde : ${account.currentBalanceCents.toCurrencyDisplay(account.currency)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (trendPct != null) TrendChip(trendPct!!)
                    }
                    if (account.monthlyContributionCents > 0) {
                        Text(
                            "+ ${account.monthlyContributionCents.toCurrencyDisplay(account.currency)}/mois",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Taux annuel + gain estimé (§4) : visible seulement si le taux est renseigné.
                    account.tauxAnnuelPct?.let { taux ->
                        val gainAnnuel = (account.currentBalanceCents * taux / 100.0).roundToLong()
                        Text(
                            "Taux ${"%.2f".format(taux)} %/an · gain estimé ≈ " +
                                "${gainAnnuel.toCurrencyDisplay(account.currency)}/an",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; showConfirm = true }
                        )
                    }
                }
            }

            // Barre de progression vers le plafond (visible uniquement si plafond configuré)
            account.plafondCents?.let { plafond ->
                if (plafond > 0) {
                    val progression = (account.currentBalanceCents.toFloat() / plafond).coerceIn(0f, 1f)
                    val couleur = if (progression >= 0.9f) MaterialTheme.colorScheme.error
                                  else MaterialTheme.colorScheme.primary
                    LinearProgressIndicator(
                        progress = { progression },
                        modifier = Modifier.fillMaxWidth(),
                        color = couleur,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        "${(progression * 100).toInt()}% du plafond" +
                            " • max ${plafond.toCurrencyDisplay(account.currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = couleur
                    )
                }
            }

            // Bouton versement : visible uniquement si un montant mensuel est configuré.
            // Rappel in-app (§ versement mensuel) : quand le versement du mois n'est pas
            // encore enregistré, on met le bouton en avant - puce + bordure dorées.
            // L'or (primary) en accent, pas le rouge (error) : c'est un rappel, pas une alerte.
            if (account.monthlyContributionCents > 0) {
                val montant = account.monthlyContributionCents.toCurrencyDisplay(account.currency)
                OutlinedButton(
                    onClick = { showVersementConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = if (versementEnAttente)
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else ButtonDefaults.outlinedButtonBorder
                ) {
                    if (versementEnAttente) {
                        Box(
                            Modifier.size(8.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Verser (+$montant)")
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Supprimer ce compte ?") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Annuler") } }
        )
    }

    if (showVersementConfirm) {
        val dateAujourdhui = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH))
        AlertDialog(
            onDismissRequest = { showVersementConfirm = false },
            title = { Text("Verser ce mois ?") },
            text = {
                Text(
                    "${account.monthlyContributionCents.toCurrencyDisplay(account.currency)} seront " +
                    "ajoutés au solde le $dateAujourdhui. Un seul versement par mois est autorisé."
                )
            },
            confirmButton = {
                TextButton(onClick = { onVersement(); showVersementConfirm = false }) { Text("Confirmer") }
            },
            dismissButton = {
                TextButton(onClick = { showVersementConfirm = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun SavingsTypeChip(type: SavingsType, customName: String? = null) {
    val label = if (type == SavingsType.AUTRE && !customName.isNullOrBlank()) customName
                else type.displayName
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * Ligne enfant repliable (§8). En-tête cliquable : prénom + total épargné pour lui +
 * chevron. Replié par défaut - avec 8 enfants ou plus, la liste des comptes de chacun
 * n'apparaît qu'à la demande. Déplié : comptes associés + actions (associer / supprimer).
 */
@Composable
private fun ChildCard(
    child: Child,
    savingsAccounts: List<SavingsAccount>,
    tousLesComptes: List<SavingsAccount>,
    totalCents: Long,
    summaryCurrency: Currency,
    onDelete: () -> Unit,
    onAssocierComptes: (Set<Long>) -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    var showAssocier by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            // En-tête cliquable (replie / déplie)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Person, contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary)
                    Text(child.name, style = MaterialTheme.typography.titleSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(totalCents.toCurrencyDisplay(summaryCurrency),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Replier" else "Déplier",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (savingsAccounts.isEmpty()) {
                        Text("Aucun compte épargne associé", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        savingsAccounts.forEach { acc ->
                            Text(
                                "• ${acc.type.displayName} - ${acc.currentBalanceCents.toCurrencyDisplay(acc.currency)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Row {
                        TextButton(onClick = { showAssocier = true }) {
                            Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Associer des comptes")
                        }
                        TextButton(onClick = { showConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp))
                            Text("Supprimer", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Supprimer ${child.name} ?") },
            text = { Text("Les comptes épargne associés ne seront pas supprimés.") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Annuler") } }
        )
    }

    if (showAssocier) {
        AssocierComptesDialog(
            childName = child.name,
            tousLesComptes = tousLesComptes,
            comptesDejaAssocies = savingsAccounts.map { it.id }.toSet(),
            onConfirm = { selectionnes -> onAssocierComptes(selectionnes); showAssocier = false },
            onDismiss = { showAssocier = false }
        )
    }
}

@Composable
private fun AssocierComptesDialog(
    childName: String,
    tousLesComptes: List<SavingsAccount>,
    comptesDejaAssocies: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    // État local : IDs des comptes cochés dans le dialog
    var selectionnes by remember { mutableStateOf(comptesDejaAssocies) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comptes de $childName") },
        text = {
            if (tousLesComptes.isEmpty()) {
                Text("Aucun compte épargne disponible.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    tousLesComptes.forEach { compte ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = compte.id in selectionnes,
                                onCheckedChange = { coche ->
                                    selectionnes = if (coche) selectionnes + compte.id
                                    else selectionnes - compte.id
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                val nomType = if (compte.type == SavingsType.AUTRE) compte.label
                                              else compte.type.displayName
                                Text(nomType, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    compte.currentBalanceCents.toCurrencyDisplay(compte.currency),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectionnes) }) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSavingsSheet(
    children: List<Child>,
    defaultCurrency: Currency = Currency.EUR,
    onSave: (SavingsType, String, String, String, Currency, Long?, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(SavingsType.LIVRET_A) }
    var label by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var contribution by remember { mutableStateOf("") }
    var taux by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(defaultCurrency) }
    var selectedChild by remember { mutableStateOf<Child?>(null) }
    var typeExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var childExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Plafond : pré-rempli automatiquement depuis PlafondDefaut quand un type connu est sélectionné
    var plafond by remember {
        mutableStateOf(
            PlafondDefaut.suggerer(SavingsType.LIVRET_A, defaultCurrency)
                ?.let { "%.2f".format(it / 100.0) } ?: ""
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Nouveau compte épargne", style = MaterialTheme.typography.titleLarge)

            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = selectedType.displayName, onValueChange = {},
                    readOnly = true, label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    SavingsType.entries.forEach { type ->
                        DropdownMenuItem(text = { Text(type.displayName) },
                            onClick = {
                                selectedType = type
                                if (type == SavingsType.AUTRE) label = ""
                                else if (label.isEmpty()) label = type.displayName
                                // Mise à jour automatique du plafond si un défaut est connu
                                plafond = PlafondDefaut.suggerer(type, selectedCurrency)
                                    ?.let { "%.2f".format(it / 100.0) } ?: ""
                                typeExpanded = false
                            })
                    }
                }
            }

            // Quand AUTRE : le libellé devient le nom du type personnalisé (obligatoire)
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = {
                    Text(
                        if (selectedType == SavingsType.AUTRE) "Nom du type (ex. Nalo, PEP, Crowdfunding...)"
                        else "Libellé"
                    )
                },
                isError = selectedType == SavingsType.AUTRE && label.isBlank(),
                supportingText = if (selectedType == SavingsType.AUTRE && label.isBlank()) {
                    { Text("Le nom du type est obligatoire") }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(value = balance, onValueChange = { balance = it },
                label = { Text("Solde actuel") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = contribution, onValueChange = { contribution = it },
                label = { Text("Versement mensuel (optionnel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = taux, onValueChange = { taux = it },
                label = { Text("Taux annuel % (optionnel)") },
                supportingText = { Text("Sert à estimer les intérêts annuels") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                OutlinedTextField(
                    value = "${selectedCurrency.name} (${selectedCurrency.symbol})", onValueChange = {},
                    readOnly = true, label = { Text("Devise") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                    Currency.entries.forEach { c ->
                        DropdownMenuItem(text = { Text("${c.name} (${c.symbol})") },
                            onClick = {
                                selectedCurrency = c
                                // Recalculer la suggestion de plafond avec la nouvelle devise
                                plafond = PlafondDefaut.suggerer(selectedType, c)
                                    ?.let { "%.2f".format(it / 100.0) } ?: ""
                                currencyExpanded = false
                            })
                    }
                }
            }

            OutlinedTextField(
                value = plafond,
                onValueChange = { plafond = it },
                label = { Text("Plafond (optionnel)") },
                supportingText = {
                    val suggestion = PlafondDefaut.suggerer(selectedType, selectedCurrency)
                    if (suggestion != null)
                        Text("Plafond légal : ${"%.2f".format(suggestion / 100.0)} ${selectedCurrency.symbol}")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (children.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = childExpanded, onExpandedChange = { childExpanded = it }) {
                    OutlinedTextField(
                        value = selectedChild?.name ?: "Aucun (personnel)", onValueChange = {},
                        readOnly = true, label = { Text("Associer à un enfant") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(childExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = childExpanded, onDismissRequest = { childExpanded = false }) {
                        DropdownMenuItem(text = { Text("Aucun (personnel)") },
                            onClick = { selectedChild = null; childExpanded = false })
                        children.forEach { child ->
                            DropdownMenuItem(text = { Text(child.name) },
                                onClick = { selectedChild = child; childExpanded = false })
                        }
                    }
                }
            }

            Button(
                onClick = { onSave(selectedType, label, balance, contribution, selectedCurrency, selectedChild?.id, plafond, taux) },
                enabled = label.isNotBlank() && balance.replace(',', '.').toDoubleOrNull()?.let { it >= 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ajouter le compte") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSavingsSheet(
    account: SavingsAccount,
    children: List<Child>,
    onSave: (SavingsType, String, String, String, Currency, Long?, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    // Pré-remplissage avec les valeurs actuelles du compte
    var selectedType by remember { mutableStateOf(account.type) }
    var label by remember { mutableStateOf(account.label) }
    var balance by remember { mutableStateOf("%.2f".format(account.currentBalanceCents / 100.0).replace(',', '.')) }
    var contribution by remember {
        mutableStateOf(
            if (account.monthlyContributionCents > 0) "%.2f".format(account.monthlyContributionCents / 100.0).replace(',', '.') else ""
        )
    }
    var taux by remember {
        mutableStateOf(account.tauxAnnuelPct?.let { "%.2f".format(it).replace(',', '.') } ?: "")
    }
    var selectedCurrency by remember { mutableStateOf(account.currency) }
    var selectedChild by remember { mutableStateOf(children.find { it.id == account.childId }) }
    var typeExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var childExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    // Pré-remplir le plafond existant ; sinon suggérer le plafond légal connu
    var plafond by remember {
        mutableStateOf(
            account.plafondCents?.let { "%.2f".format(it / 100.0) }
                ?: PlafondDefaut.suggerer(account.type, account.currency)?.let { "%.2f".format(it / 100.0) }
                ?: ""
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Modifier le compte épargne", style = MaterialTheme.typography.titleLarge)

            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = selectedType.displayName, onValueChange = {},
                    readOnly = true, label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    SavingsType.entries.forEach { type ->
                        DropdownMenuItem(text = { Text(type.displayName) },
                            onClick = {
                                if (type == SavingsType.AUTRE && selectedType != SavingsType.AUTRE) label = ""
                                selectedType = type
                                plafond = PlafondDefaut.suggerer(type, selectedCurrency)
                                    ?.let { "%.2f".format(it / 100.0) } ?: ""
                                typeExpanded = false
                            })
                    }
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = {
                    Text(
                        if (selectedType == SavingsType.AUTRE) "Nom du type (ex. Nalo, PEP, Crowdfunding...)"
                        else "Libellé"
                    )
                },
                isError = selectedType == SavingsType.AUTRE && label.isBlank(),
                supportingText = if (selectedType == SavingsType.AUTRE && label.isBlank()) {
                    { Text("Le nom du type est obligatoire") }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(value = balance, onValueChange = { balance = it },
                label = { Text("Solde actuel") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = contribution, onValueChange = { contribution = it },
                label = { Text("Versement mensuel (optionnel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = taux, onValueChange = { taux = it },
                label = { Text("Taux annuel % (optionnel)") },
                supportingText = { Text("Sert à estimer les intérêts annuels") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                OutlinedTextField(
                    value = "${selectedCurrency.name} (${selectedCurrency.symbol})", onValueChange = {},
                    readOnly = true, label = { Text("Devise") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                    Currency.entries.forEach { c ->
                        DropdownMenuItem(text = { Text("${c.name} (${c.symbol})") },
                            onClick = {
                                selectedCurrency = c
                                plafond = PlafondDefaut.suggerer(selectedType, c)
                                    ?.let { "%.2f".format(it / 100.0) } ?: ""
                                currencyExpanded = false
                            })
                    }
                }
            }

            OutlinedTextField(
                value = plafond,
                onValueChange = { plafond = it },
                label = { Text("Plafond (optionnel)") },
                supportingText = {
                    val suggestion = PlafondDefaut.suggerer(selectedType, selectedCurrency)
                    if (suggestion != null)
                        Text("Plafond légal : ${"%.2f".format(suggestion / 100.0)} ${selectedCurrency.symbol}")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (children.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = childExpanded, onExpandedChange = { childExpanded = it }) {
                    OutlinedTextField(
                        value = selectedChild?.name ?: "Aucun (personnel)", onValueChange = {},
                        readOnly = true, label = { Text("Associer à un enfant") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(childExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = childExpanded, onDismissRequest = { childExpanded = false }) {
                        DropdownMenuItem(text = { Text("Aucun (personnel)") },
                            onClick = { selectedChild = null; childExpanded = false })
                        children.forEach { child ->
                            DropdownMenuItem(text = { Text(child.name) },
                                onClick = { selectedChild = child; childExpanded = false })
                        }
                    }
                }
            }

            Button(
                onClick = { onSave(selectedType, label, balance, contribution, selectedCurrency, selectedChild?.id, plafond, taux) },
                enabled = label.isNotBlank() && balance.replace(',', '.').toDoubleOrNull()?.let { it >= 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer les modifications") }
        }
    }
}

/**
 * Feuille de création / édition d'un objectif d'épargne (§3). Une seule feuille pour
 * les deux cas : [existant] non-null = édition (champs pré-remplis), null = création.
 * Calquée sur [AddSavingsSheet] (ModalBottomSheet, dropdown devise, DatePicker comme
 * dans InvestmentsScreen).
 *
 * [comptesDisponibles] alimente le sélecteur de compte source en FundingMode.SOLDE_COMPTE
 * (§1 F1 du cadrage CADRAGE_OBJECTIFS_CONNECTES.md) - la liste complète des comptes
 * épargne, sans filtre : lier un compte à un objectif n'empêche pas de le garder par
 * ailleurs (une seule source par objectif - Q1 du cadrage - mais un compte peut être la
 * source de plusieurs objectifs).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ObjectifSheet(
    existant: SavingsGoal?,
    defaultCurrency: Currency,
    comptesDisponibles: List<SavingsAccount>,
    onSave: (String, String, String, String, Currency, LocalDate, GoalColor, GoalIcon, Long?, FundingMode) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    var name by remember { mutableStateOf(existant?.name ?: "") }
    var target by remember {
        mutableStateOf(existant?.let { "%.2f".format(it.targetAmountCents / 100.0).replace(',', '.') } ?: "")
    }
    var current by remember {
        mutableStateOf(existant?.let { "%.2f".format(it.currentAmountCents / 100.0).replace(',', '.') } ?: "")
    }
    var monthly by remember {
        mutableStateOf(
            existant?.takeIf { it.monthlyContributionCents > 0 }
                ?.let { "%.2f".format(it.monthlyContributionCents / 100.0).replace(',', '.') } ?: ""
        )
    }
    var selectedCurrency by remember { mutableStateOf(existant?.currency ?: defaultCurrency) }
    var targetDate by remember { mutableStateOf(existant?.targetDate ?: LocalDate.now().plusMonths(12)) }
    var selectedColor by remember { mutableStateOf(existant?.colorKey ?: GoalColor.OR) }
    var selectedIcon by remember { mutableStateOf(existant?.iconKey ?: GoalIcon.AUTRE) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedFundingMode by remember { mutableStateOf(existant?.fundingModeEffectif ?: FundingMode.MANUEL) }
    var selectedSourceAccountId by remember { mutableStateOf(existant?.sourceAccountId) }
    var sourceAccountExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val targetValide = target.replace(',', '.').toDoubleOrNull()?.let { it > 0.0 } == true
    val sourceValide = selectedFundingMode != FundingMode.SOLDE_COMPTE || selectedSourceAccountId != null

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (existant == null) "Nouvel objectif" else "Modifier l'objectif",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Nom (ex. Voiture, Vacances...)") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = target, onValueChange = { target = it },
                label = { Text("Montant objectif") },
                isError = target.isNotBlank() && !targetValide,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            // Masqué en SOLDE_COMPTE : le montant est dérivé du compte lié, pas saisi ici
            // (voir le sélecteur "Alimenté par" plus bas).
            if (selectedFundingMode != FundingMode.SOLDE_COMPTE) {
                OutlinedTextField(value = current, onValueChange = { current = it },
                    label = { Text("Montant déjà épargné (optionnel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(value = monthly, onValueChange = { monthly = it },
                label = { Text("Versement mensuel (optionnel)") },
                supportingText = { Text("Sert à projeter la date d'atteinte") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true, modifier = Modifier.fillMaxWidth())

            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                OutlinedTextField(
                    value = "${selectedCurrency.name} (${selectedCurrency.symbol})", onValueChange = {},
                    readOnly = true, label = { Text("Devise") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                    Currency.entries.forEach { c ->
                        DropdownMenuItem(text = { Text("${c.name} (${c.symbol})") },
                            onClick = { selectedCurrency = c; currencyExpanded = false })
                    }
                }
            }

            OutlinedTextField(
                value = targetDate.format(dateFormatter), onValueChange = {},
                readOnly = true, label = { Text("Échéance") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Choisir une date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Couleur : 3 pastilles, bord doré sur la sélection.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Couleur", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GoalColor.entries.forEach { couleur ->
                        val selectionne = couleur == selectedColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(couleur.accent())
                                .then(
                                    if (selectionne) Modifier.border(
                                        3.dp, MaterialTheme.colorScheme.primary, CircleShape
                                    ) else Modifier
                                )
                                .clickable { selectedColor = couleur }
                        )
                    }
                }
            }

            // Icône : jeu fixe de 7, surbrillance de la sélection.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Icône", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalIcon.entries.forEach { ic ->
                        val selectionne = ic == selectedIcon
                        IconButton(
                            onClick = { selectedIcon = ic },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selectionne) MaterialTheme.colorScheme.secondaryContainer
                                    else Color.Transparent
                                )
                        ) {
                            Icon(
                                ic.icon(), contentDescription = null,
                                tint = if (selectionne) MaterialTheme.colorScheme.onSecondaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Source de financement (§4 du cadrage) : MANUEL et VERSEMENTS se comportent
            // IDENTIQUEMENT aujourd'hui (même champ "Montant déjà épargné", même bouton
            // "Verser" s'il y a une mensualité) - VERSEMENTS ne fait qu'étiqueter l'usage
            // déjà existant du bouton. Seul SOLDE_COMPTE change l'écran : montant dérivé,
            // champ masqué, bouton "Verser" masqué sur la carte.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Alimenté par", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedFundingMode == FundingMode.MANUEL,
                        onClick = { selectedFundingMode = FundingMode.MANUEL },
                        label = { Text("Manuel") })
                    FilterChip(selected = selectedFundingMode == FundingMode.VERSEMENTS,
                        onClick = { selectedFundingMode = FundingMode.VERSEMENTS },
                        label = { Text("Versements") })
                    FilterChip(selected = selectedFundingMode == FundingMode.SOLDE_COMPTE,
                        onClick = { selectedFundingMode = FundingMode.SOLDE_COMPTE },
                        label = { Text("Solde d'un compte") })
                }
            }

            if (selectedFundingMode == FundingMode.SOLDE_COMPTE) {
                ExposedDropdownMenuBox(expanded = sourceAccountExpanded, onExpandedChange = { sourceAccountExpanded = it }) {
                    OutlinedTextField(
                        value = comptesDisponibles.firstOrNull { it.id == selectedSourceAccountId }?.label ?: "",
                        onValueChange = {}, readOnly = true, label = { Text("Compte lié") },
                        isError = !sourceValide,
                        supportingText = { Text("La progression suit le solde de ce compte") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sourceAccountExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = sourceAccountExpanded, onDismissRequest = { sourceAccountExpanded = false }) {
                        comptesDisponibles.forEach { compte ->
                            DropdownMenuItem(text = { Text(compte.label) },
                                onClick = { selectedSourceAccountId = compte.id; sourceAccountExpanded = false })
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(name, target, current, monthly, selectedCurrency, targetDate, selectedColor, selectedIcon, selectedSourceAccountId, selectedFundingMode)
                },
                enabled = name.isNotBlank() && targetValide && sourceValide,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (existant == null) "Créer l'objectif" else "Enregistrer") }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetDate.toEpochDay() * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        targetDate = LocalDate.ofEpochDay(millis / 86_400_000L)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annuler") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun AddChildDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un enfant") },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Prénom") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}


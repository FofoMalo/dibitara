package com.dibitara.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dibitara.app.domain.model.BankAccount
import com.dibitara.app.domain.model.BankAccountsSummary
import com.dibitara.app.domain.model.CashflowProjection
import com.dibitara.app.domain.model.Currency
import com.dibitara.app.domain.model.DashboardCard
import com.dibitara.app.domain.model.EnveloppeStatus
import com.dibitara.app.domain.model.MonthlyExpense
import com.dibitara.app.domain.model.MonthlyReport
import com.dibitara.app.domain.model.PatrimonyOverview
import com.dibitara.app.domain.model.RecategorizationSuggestion
import com.dibitara.app.domain.model.RecurrenceFrequency
import com.dibitara.app.domain.model.UpcomingPayment
import com.dibitara.app.presentation.common.HeroCard
import com.dibitara.app.presentation.common.ProjectionSparkline
import com.dibitara.app.presentation.common.TrendChip
import com.dibitara.app.presentation.common.LocalMontantsMasques
import com.dibitara.app.presentation.common.chartColor
import com.dibitara.app.presentation.common.chartIcon
import com.dibitara.app.presentation.common.toCurrencyDisplay
import com.dibitara.app.presentation.navigation.Screen
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.axis.formatter.DecimalFormatAxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

@Composable
fun DashboardScreen(
    onNavigateToDebts        : () -> Unit = {},
    onNavigateToReport       : () -> Unit = {},
    onNavigateToBudget       : () -> Unit = {},
    onNavigateToSavings      : () -> Unit = {},
    onNavigateToInvestments  : () -> Unit = {},
    onNavigateToPatrimoine   : () -> Unit = {},
    onNavigateToExpensesTransaction : (Long) -> Unit = {},
    navController            : NavHostController? = null,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val state = uiState) {
            is DashboardUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is DashboardUiState.Error ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            is DashboardUiState.Success -> {
                val isEditMode by viewModel.isEditMode.collectAsState()
                DashboardContent(
                    overview                    = state.overview,
                    spendingHistory             = state.spendingHistory,
                    upcomingPayments            = state.upcomingPayments,
                    onNavigateToDebts           = onNavigateToDebts,
                    onNavigateToReport          = onNavigateToReport,
                    onNavigateToBudget          = onNavigateToBudget,
                    onNavigateToSavings         = onNavigateToSavings,
                    onNavigateToInvestments     = onNavigateToInvestments,
                    onNavigateToPatrimoine      = onNavigateToPatrimoine,
                    onNavigateToExpensesTransaction = onNavigateToExpensesTransaction,
                    rapportMensuel              = state.rapportMensuel,
                    cashflowProjection          = state.cashflowProjection,
                    recategorizationSuggestions = state.recategorizationSuggestions,
                    enveloppesEnAlerte          = state.enveloppesEnAlerte,
                    comptesSummary              = state.comptesSummary,
                    patrimoineTrendPct          = state.patrimoineTrendPct,
                    cardOrder                   = state.cardOrder,
                    isEditMode                  = isEditMode,
                    onToggleEditMode            = { viewModel.toggleEditMode() },
                    onChangeDevise              = { viewModel.changerDevise(it) },
                    onMoveCard                  = { from, to -> viewModel.moveCard(from, to) },
                    onApplyRecategorization     = { viewModel.appliquerRecategorisation(it) },
                    onRefuseRecategorization    = { viewModel.refuserRecategorisation(it) },
                    onVoirDetailProjection      = { navController?.navigate(Screen.ProjectionDetail.route) },
                    onNavigateToBankAccounts    = { navController?.navigate(Screen.BankAccounts.route) },
                    onNavigateToScenarios       = { navController?.navigate(Screen.Scenarios.route) },
                    masquerMontants             = state.masquerMontants,
                    onToggleMasquerMontants     = { viewModel.toggleMasquerMontants() }
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    overview                    : PatrimonyOverview,
    spendingHistory             : List<MonthlyExpense>,
    upcomingPayments            : List<UpcomingPayment>             = emptyList(),
    onNavigateToDebts           : () -> Unit,
    onNavigateToReport          : () -> Unit,
    onNavigateToBudget          : () -> Unit,
    onNavigateToSavings         : () -> Unit,
    onNavigateToInvestments     : () -> Unit,
    onNavigateToPatrimoine      : () -> Unit,
    onNavigateToExpensesTransaction : (Long) -> Unit = {},
    rapportMensuel              : MonthlyReport?                    = null,
    cashflowProjection          : CashflowProjection?               = null,
    recategorizationSuggestions : List<RecategorizationSuggestion>  = emptyList(),
    enveloppesEnAlerte          : List<EnveloppeStatus>             = emptyList(),
    comptesSummary              : BankAccountsSummary                = BankAccountsSummary(emptyList(), 0L, Currency.EUR),
    patrimoineTrendPct          : Float?                            = null,
    cardOrder                   : List<DashboardCard>               = DashboardCard.entries.toList(),
    isEditMode                  : Boolean                           = false,
    onToggleEditMode            : () -> Unit                        = {},
    onChangeDevise              : (Currency) -> Unit                = {},
    onMoveCard                  : (fromKey: String, toKey: String) -> Unit = { _, _ -> },
    onApplyRecategorization     : (RecategorizationSuggestion) -> Unit,
    onRefuseRecategorization    : (RecategorizationSuggestion) -> Unit,
    onVoirDetailProjection      : () -> Unit                        = {},
    onNavigateToBankAccounts    : () -> Unit                        = {},
    onNavigateToScenarios       : () -> Unit                        = {},
    masquerMontants             : Boolean                           = false,
    onToggleMasquerMontants     : () -> Unit                        = {}
) {
    val lazyListState = rememberLazyListState()
    val reorderState  = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMoveCard(from.key as String, to.key as String)
    }

    LazyColumn(
        state   = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ─── En-tête fixe (non reordonnable) ──────────────────────────────────
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dibitara", style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DeviseChip(current = overview.currency, onSelect = onChangeDevise)
                    IconButton(onClick = onToggleMasquerMontants) {
                        Icon(
                            imageVector = if (masquerMontants) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (masquerMontants) "Afficher les montants" else "Masquer les montants",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleEditMode) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = if (isEditMode) "Terminer la réorganisation"
                                                 else "Réorganiser les cartes",
                            tint = if (isEditMode) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item(key = "patrimoine") {
            PatrimonyNetCard(overview = overview, trendPct = patrimoineTrendPct, onClick = onNavigateToPatrimoine)
        }
        item(key = "scenarios_entry") {
            TextButton(onClick = onNavigateToScenarios, modifier = Modifier.fillMaxWidth()) {
                Text("Scénarios →")
            }
        }

        // ─── Cartes reordonnables ──────────────────────────────────────────────
        items(cardOrder, key = { it.name }) { card ->
            ReorderableItem(reorderState, key = card.name) { isDragging ->
                val elevation by androidx.compose.animation.core.animateDpAsState(
                    if (isDragging) 8.dp else 0.dp, label = "drag_elevation"
                )
                Surface(shadowElevation = elevation, color = MaterialTheme.colorScheme.background) {
                    DashboardCardSlot(
                        card                    = card,
                        overview                = overview,
                        spendingHistory         = spendingHistory,
                        upcomingPayments        = upcomingPayments,
                        rapportMensuel          = rapportMensuel,
                        cashflowProjection      = cashflowProjection,
                        recategorizationSuggestions = recategorizationSuggestions,
                        enveloppesEnAlerte      = enveloppesEnAlerte,
                        comptesSummary          = comptesSummary,
                        isEditMode              = isEditMode,
                        dragHandleModifier      = Modifier.draggableHandle(),
                        onNavigateToDebts       = onNavigateToDebts,
                        onNavigateToReport      = onNavigateToReport,
                        onNavigateToBudget      = onNavigateToBudget,
                        onNavigateToSavings     = onNavigateToSavings,
                        onNavigateToInvestments = onNavigateToInvestments,
                        onNavigateToExpensesTransaction = onNavigateToExpensesTransaction,
                        onApplyRecategorization = onApplyRecategorization,
                        onRefuseRecategorization = onRefuseRecategorization,
                        onVoirDetailProjection  = onVoirDetailProjection,
                        onNavigateToBankAccounts = onNavigateToBankAccounts
                    )
                }
            }
        }
    }
}

/**
 * Dispatche chaque [DashboardCard] vers le composable correspondant.
 * [dragHandleModifier] est appliqué sur une icône de poignée visible en mode édition.
 */
@Composable
private fun DashboardCardSlot(
    card                        : DashboardCard,
    overview                    : PatrimonyOverview,
    spendingHistory             : List<MonthlyExpense>,
    upcomingPayments            : List<UpcomingPayment>,
    rapportMensuel              : MonthlyReport?,
    cashflowProjection          : CashflowProjection?,
    recategorizationSuggestions : List<RecategorizationSuggestion>,
    enveloppesEnAlerte          : List<EnveloppeStatus>,
    comptesSummary              : BankAccountsSummary,
    isEditMode                  : Boolean,
    dragHandleModifier          : Modifier,
    onNavigateToDebts           : () -> Unit,
    onNavigateToReport          : () -> Unit,
    onNavigateToBudget          : () -> Unit,
    onNavigateToSavings         : () -> Unit,
    onNavigateToInvestments     : () -> Unit,
    onNavigateToExpensesTransaction : (Long) -> Unit,
    onApplyRecategorization     : (RecategorizationSuggestion) -> Unit,
    onRefuseRecategorization    : (RecategorizationSuggestion) -> Unit,
    onVoirDetailProjection      : () -> Unit = {},
    onNavigateToBankAccounts    : () -> Unit = {}
) {
    // En mode édition, chaque carte affiche une poignée de déplacement à droite
    if (isEditMode) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                DashboardCardContent(
                    card, overview, spendingHistory, upcomingPayments, rapportMensuel,
                    cashflowProjection, recategorizationSuggestions, enveloppesEnAlerte, comptesSummary,
                    onNavigateToDebts, onNavigateToReport, onNavigateToBudget,
                    onNavigateToSavings, onNavigateToInvestments, onNavigateToExpensesTransaction,
                    onApplyRecategorization, onRefuseRecategorization,
                    onVoirDetailProjection, onNavigateToBankAccounts
                )
            }
            Icon(
                imageVector        = Icons.Filled.DragHandle,
                contentDescription = "Glisser pour déplacer",
                modifier           = dragHandleModifier.padding(start = 8.dp),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        DashboardCardContent(
            card, overview, spendingHistory, upcomingPayments, rapportMensuel,
            cashflowProjection, recategorizationSuggestions, enveloppesEnAlerte, comptesSummary,
            onNavigateToDebts, onNavigateToReport, onNavigateToBudget,
            onNavigateToSavings, onNavigateToInvestments, onNavigateToExpensesTransaction,
            onApplyRecategorization, onRefuseRecategorization,
            onVoirDetailProjection, onNavigateToBankAccounts
        )
    }
}

@Composable
private fun DashboardCardContent(
    card                        : DashboardCard,
    overview                    : PatrimonyOverview,
    spendingHistory             : List<MonthlyExpense>,
    upcomingPayments            : List<UpcomingPayment>,
    rapportMensuel              : MonthlyReport?,
    cashflowProjection          : CashflowProjection?,
    recategorizationSuggestions : List<RecategorizationSuggestion>,
    enveloppesEnAlerte          : List<EnveloppeStatus>,
    comptesSummary              : BankAccountsSummary,
    onNavigateToDebts           : () -> Unit,
    onNavigateToReport          : () -> Unit,
    onNavigateToBudget          : () -> Unit,
    onNavigateToSavings         : () -> Unit,
    onNavigateToInvestments     : () -> Unit,
    onNavigateToExpensesTransaction : (Long) -> Unit,
    onApplyRecategorization     : (RecategorizationSuggestion) -> Unit,
    onRefuseRecategorization    : (RecategorizationSuggestion) -> Unit,
    onVoirDetailProjection      : () -> Unit = {},
    onNavigateToBankAccounts    : () -> Unit = {}
) {
    when (card) {
        DashboardCard.METRIQUES_BUDGET_EPARGNE ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(Modifier.weight(1f), "Budget restant",  overview.liquiditesCents, overview.currency,
                    MaterialTheme.colorScheme.primary, onNavigateToBudget)
                MetricCard(Modifier.weight(1f), "Épargne", overview.epargneCents, overview.currency,
                    MaterialTheme.colorScheme.secondary, onNavigateToSavings)
            }
        DashboardCard.METRIQUES_INVESTISSEMENTS ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(Modifier.weight(1f), "Investissements", overview.investissementsCents,
                    overview.currency, MaterialTheme.colorScheme.tertiary, onNavigateToInvestments)
                MetricCard(Modifier.weight(1f), "Revenus locatifs (année)", overview.airbnbAnnualRevenueCents,
                    overview.currency, MaterialTheme.colorScheme.tertiary, onNavigateToInvestments)
            }
        DashboardCard.DETTES ->
            DebtsCard(totalCents = overview.dettesTotalCents, currency = overview.currency,
                onClick = onNavigateToDebts)
        DashboardCard.CASHFLOW_PROJECTION ->
            if (cashflowProjection != null)
                CashflowProjectionCard(projection = cashflowProjection, onVoirDetail = onVoirDetailProjection)
        DashboardCard.RAPPORT_GRAPHIQUE ->
            if (rapportMensuel != null)
                RapportSyntheseCard(rapport = rapportMensuel, onVoirDetail = onNavigateToReport)
            else if (spendingHistory.any { it.totalCents > 0 })
                SpendingHistoryCard(history = spendingHistory, currency = overview.currency)
        DashboardCard.SUGGESTIONS_RECATEGORISATION ->
            if (recategorizationSuggestions.isNotEmpty())
                RecategorizationCard(
                    suggestions = recategorizationSuggestions,
                    onApply     = onApplyRecategorization,
                    onRefuse    = onRefuseRecategorization
                )
        DashboardCard.ENVELOPPES_ALERTE ->
            if (enveloppesEnAlerte.isNotEmpty())
                EnveloppeAlerteCard(statuts = enveloppesEnAlerte, onClick = onNavigateToBudget)
        DashboardCard.PROCHAINS_PAIEMENTS ->
            if (upcomingPayments.isNotEmpty())
                UpcomingPaymentsCard(payments = upcomingPayments, onClick = onNavigateToExpensesTransaction)
        DashboardCard.COMPTES ->
            if (comptesSummary.comptes.isNotEmpty())
                ComptesCard(summary = comptesSummary, onClick = onNavigateToBankAccounts)
    }
}

/**
 * Enveloppes budgétaires (Sprint 40) dont le taux de dépense dépasse le seuil d'alerte
 * ce mois-ci. Même esprit que [RecategorizationCard] : carte conditionnelle, cap à 3
 * lignes, clic → écran Budget pour voir le détail et ajuster.
 */
@Composable
private fun EnveloppeAlerteCard(statuts: List<EnveloppeStatus>, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "${statuts.size} enveloppe${if (statuts.size > 1) "s" else ""} proche${if (statuts.size > 1) "s" else ""} du plafond",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            statuts.take(3).forEach { statut ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector        = statut.envelope.category.chartIcon(),
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp),
                            tint = if (statut.isDepasse) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.tertiary
                        )
                        Text(statut.envelope.category.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "${statut.depenseCents.toCurrencyDisplay(statut.envelope.currency)} / " +
                        statut.envelope.plafondCents.toCurrencyDisplay(statut.envelope.currency),
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (statut.isDepasse) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Solde par compte bancaire suivi (BRED, TradeRepublic...) - solde saisi/mis à jour
 * manuellement par l'utilisateur (voir [BankAccount], pas dérivé des transactions).
 */
@Composable
private fun ComptesCard(summary: BankAccountsSummary, onClick: () -> Unit) {
    HeroCard(onClick = onClick) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mes comptes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            summary.comptes.forEach { compte ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector        = compte.provider.chartIcon(),
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp),
                            tint               = compte.provider.chartColor()
                        )
                        Text(compte.label, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        compte.currentBalanceCents.toCurrencyDisplay(compte.currency),
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            // Total affiché uniquement si plusieurs comptes - sinon il duplique la seule ligne au-dessus
            if (summary.comptes.size > 1) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        summary.totalCents.toCurrencyDisplay(summary.currency),
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SpendingHistoryCard(history: List<MonthlyExpense>, currency: Currency) {
    // ChartEntryModelProducer gère les mises à jour asynchrones des données du graphique
    val producer = remember { ChartEntryModelProducer() }
    val labels = remember(history) { history.map { moisAbrege(it.month) } }
    val montantsMasques = LocalMontantsMasques.current

    LaunchedEffect(history) {
        // Conversion centimes → euros, x = index du mois dans la liste
        producer.setEntries(
            history.mapIndexed { i, expense ->
                entryOf(i.toFloat(), expense.totalCents.toFloat() / 100f)
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Transactions - 6 derniers mois", style = MaterialTheme.typography.titleMedium)
            ProvideChartStyle(m3ChartStyle()) {
                Chart(
                    chart = columnChart(),
                    chartModelProducer = producer,
                    startAxis = rememberStartAxis(
                        // Le formatter par défaut de Vico affiche les montants en clair sur l'axe,
                        // sans passer par toCurrencyDisplay() (non applicable ici : Vico exige un
                        // Float, pas les centimes Long attendus par l'extension) - d'où ce respect
                        // manuel du masquage global des montants.
                        valueFormatter = if (montantsMasques) {
                            AxisValueFormatter<AxisPosition.Vertical.Start> { _, _ -> "••••" }
                        } else {
                            DecimalFormatAxisValueFormatter()
                        }
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                            labels.getOrElse(value.toInt()) { "" }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
        }
    }
}

/** Chip de devise dans l'en-tête de l'Accueil - raccourci vers le sélecteur déjà présent dans Paramètres. */
@Composable
private fun DeviseChip(current: Currency, onSelect: (Currency) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text("${current.symbol} ${current.isoCode}") }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(Currency.EUR, Currency.USD, Currency.XOF).forEach { devise ->
                DropdownMenuItem(
                    text = { Text("${devise.symbol} ${devise.isoCode}") },
                    onClick = { expanded = false; onSelect(devise) },
                    leadingIcon = if (devise == current) {
                        { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun PatrimonyNetCard(overview: PatrimonyOverview, trendPct: Float?, onClick: () -> Unit) {
    HeroCard(onClick = onClick) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Patrimoine brut",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Voir le détail du patrimoine",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                overview.patrimoineBrutCents.toCurrencyDisplay(overview.currency),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (overview.dettesTotalCents > 0L) {
                Text(
                    "Dettes : -${overview.dettesTotalCents.toCurrencyDisplay(overview.currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Patrimoine net", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (trendPct != null) TrendChip(trendPct)
            }
            Text(
                overview.patrimoineNetCents.toCurrencyDisplay(overview.currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (overview.hasConvertedValues) {
                Text(
                    "≈ conversion appliquée (${overview.currency.isoCode})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            if (overview.patrimoineBrutCents > 0L) {
                Spacer(Modifier.height(16.dp))
                AllocationBar(overview)
            }
        }
    }
}

/**
 * Barre de répartition Épargne/Investissements sous le hero patrimoine - vue synthétique
 * "en un coup d'œil", sans scroll. Le budget restant (liquidités) n'y figure pas : c'est un
 * flux mensuel, pas un actif, au même titre qu'Airbnb/véhicule locatif (voir PatrimonyOverview).
 * Mêmes couleurs que les MetricCard ci-dessous (primary/secondary/tertiary) pour rester cohérent visuellement.
 */
@Composable
private fun AllocationBar(overview: PatrimonyOverview) {
    val total = overview.patrimoineBrutCents.toFloat()
    val segments = listOf(
        Triple("Épargne", overview.epargneCents, MaterialTheme.colorScheme.secondary),
        Triple("Investissements", overview.investissementsCents, MaterialTheme.colorScheme.tertiary)
    ).filter { it.second > 0L }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(5.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            segments.forEach { (_, cents, color) ->
                Box(
                    modifier = Modifier
                        .weight(cents.toFloat())
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            segments.forEach { (label, cents, color) ->
                val pct = (cents.toFloat() / total * 100).roundToInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(color, RoundedCornerShape(2.dp))
                    )
                    Text(
                        "$label $pct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier   : Modifier = Modifier,
    title      : String,
    valueCents : Long,
    currency   : Currency,
    color      : androidx.compose.ui.graphics.Color,
    onClick    : (() -> Unit)? = null
) {
    // Contenu commun extrait pour éviter la duplication entre les deux surcharges de Card
    @Composable
    fun content() {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (onClick != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Voir le détail",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            Text(
                valueCents.toCurrencyDisplay(currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }

    if (onClick != null) {
        Card(modifier = modifier, onClick = onClick) { content() }
    } else {
        Card(modifier = modifier) { content() }
    }
}

@Composable
private fun DebtsCard(totalCents: Long, currency: Currency, onClick: () -> Unit) {
    // tertiaryContainer : une dette en cours est une information neutre, pas une alerte
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (totalCents > 0) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Dettes & crédits", style = MaterialTheme.typography.titleSmall,
                    color = if (totalCents > 0) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onSurface)
                Text("Appuyez pour gérer", style = MaterialTheme.typography.bodySmall,
                    color = if (totalCents > 0)
                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                totalCents.toCurrencyDisplay(currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (totalCents > 0) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─── Carte rapport synthèse compacte ─────────────────────────────────────────

@Composable
private fun RapportSyntheseCard(rapport: MonthlyReport, onVoirDetail: () -> Unit) {
    val hausse = rapport.variationDepensesCents > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // En-tête : titre + lien "Voir le détail"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Rapport - ${moisComplet(rapport.month)} ${rapport.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = onVoirDetail,
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Voir le détail", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Bilan sur une ligne : Revenus / Dépenses / Solde
            // Dépenses en secondary (neutre) : dépenser est normal. Red réservé au solde négatif.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                BilanMini("Revenus",  rapport.revenusCents,  rapport.currency, MaterialTheme.colorScheme.primary)
                BilanMini("Dépenses", rapport.depensesCents, rapport.currency, MaterialTheme.colorScheme.secondary)
                BilanMini(
                    label      = "Solde",
                    valueCents = rapport.soldeCents,
                    currency   = rapport.currency,
                    color      = if (rapport.soldeCents >= 0) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.error
                )
            }

            // Variation vs mois précédent
            if (rapport.variationDepensesCents != 0L) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (hausse) Icons.AutoMirrored.Filled.TrendingUp
                                      else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (hausse) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    val delta = if (hausse) rapport.variationDepensesCents
                                else -rapport.variationDepensesCents
                    Text(
                        text = "${if (hausse) "+" else "-"}${delta.toCurrencyDisplay(rapport.currency)} vs mois précédent",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hausse) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun BilanMini(label: String, valueCents: Long, currency: Currency,
                      color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valueCents.toCurrencyDisplay(currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold, color = color)
    }
}

private fun moisComplet(month: Int): String = when (month) {
    1 -> "Janvier"; 2 -> "Février"; 3 -> "Mars"; 4 -> "Avril"
    5 -> "Mai"; 6 -> "Juin"; 7 -> "Juillet"; 8 -> "Août"
    9 -> "Septembre"; 10 -> "Octobre"; 11 -> "Novembre"; else -> "Décembre"
}

// ─── Carte "Prochains paiements" ──────────────────────────────────────────────

@Composable
private fun UpcomingPaymentsCard(payments: List<UpcomingPayment>, onClick: (Long) -> Unit = {}) {
    val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Prochains paiements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            payments.forEach { upcoming ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(upcoming.template.id) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = upcoming.template.note.ifBlank { upcoming.template.category.displayName },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${frequenceLabel(upcoming.template.recurrenceFrequency)} · ${upcoming.nextDate.format(dateFmt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        // Montant en neutre : un prélèvement planifié n'est pas une urgence
                        Text(
                            text = upcoming.template.amountCents.toCurrencyDisplay(upcoming.template.currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val label = when {
                            upcoming.daysUntil == 0L -> "Aujourd'hui"
                            upcoming.daysUntil == 1L -> "Demain"
                            else                     -> "Dans ${upcoming.daysUntil}j"
                        }
                        // Red uniquement si c'est aujourd'hui ou demain (urgence réelle)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (upcoming.daysUntil <= 1)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (upcoming != payments.last()) HorizontalDivider()
            }
        }
    }
}

private fun frequenceLabel(freq: RecurrenceFrequency?): String = when (freq) {
    RecurrenceFrequency.WEEKLY  -> "Hebdo"
    RecurrenceFrequency.YEARLY  -> "Annuel"
    else                        -> "Mensuel"
}

/** Convertit un numéro de mois (1–12) en abréviation française à 3 lettres. */
private fun moisAbrege(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Fév"; 3 -> "Mar"; 4 -> "Avr"; 5 -> "Mai"; 6 -> "Jun"
    7 -> "Jul"; 8 -> "Aoû"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Déc"
}

// ─── Carte projection de trésorerie ──────────────────────────────────────────

@Composable
private fun CashflowProjectionCard(projection: CashflowProjection, onVoirDetail: () -> Unit = {}) {
    val enDanger = projection.jourPassageSeuilNegatif != null
    // Dé-dramatisé (refonte 2026-08) : plus de fond rouge/orange plein - une carte
    // normale avec une bordure discrète, le rouge n'apparaît que sur les éléments
    // qui portent vraiment l'alerte (badge, valeur J+30, mini-courbe, date).
    val borderColor = if (enDanger) MaterialTheme.colorScheme.error.copy(alpha = 0.32f)
                       else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Projection 30 jours", style = MaterialTheme.typography.titleMedium)
                if (enDanger) {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f), RoundedCornerShape(7.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                        Text("Sous le seuil", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                BilanMini(
                    label      = "Aujourd'hui",
                    valueCents = projection.soldeActuelCents,
                    currency   = projection.currency,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                BilanMini(
                    label      = "Dans 30 jours",
                    valueCents = projection.soldeProjecte30jCents,
                    currency   = projection.currency,
                    color      = if (enDanger) MaterialTheme.colorScheme.error
                                 else MaterialTheme.colorScheme.tertiary
                )
            }

            if (projection.pointsTimeline.size >= 2) {
                ProjectionSparkline(
                    points = projection.pointsTimeline,
                    color  = if (enDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                )
            }

            if (enDanger) {
                val dateFmt = DateTimeFormatter.ofPattern("dd/MM")
                Row {
                    Text(
                        text  = "Solde sous le seuil à partir du ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text  = projection.jourPassageSeuilNegatif!!.format(dateFmt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            TextButton(
                onClick = onVoirDetail,
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Voir le détail", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ─── Carte suggestions de recatégorisation ───────────────────────────────────

@Composable
private fun RecategorizationCard(
    suggestions : List<RecategorizationSuggestion>,
    onApply     : (RecategorizationSuggestion) -> Unit,
    onRefuse    : (RecategorizationSuggestion) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "${suggestions.size} transaction${if (suggestions.size > 1) "s" else ""} à mieux catégoriser",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Afficher au maximum 3 suggestions pour ne pas surcharger le dashboard
            suggestions.take(3).forEach { suggestion ->
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text       = suggestion.transaction.note.ifBlank { "—" },
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text  = if (suggestion.suggestedSubCategory != null)
                            "Autre → ${suggestion.suggestedSubCategory.displayName}"
                        else
                            "Autre → ${suggestion.suggestedCategory.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Refuser - outline, confirme "Autre / Divers" et retire la suggestion
                        OutlinedButton(
                            onClick  = { onRefuse(suggestion) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Refuser", style = MaterialTheme.typography.labelMedium)
                        }
                        // Appliquer - filled, change la catégorie
                        Button(
                            onClick  = { onApply(suggestion) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Appliquer", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                if (suggestion != suggestions.take(3).last()) HorizontalDivider()
            }

            if (suggestions.size > 3) {
                Text(
                    text  = "+ ${suggestions.size - 3} autre${if (suggestions.size - 3 > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

package com.dibitara.app.presentation.investments

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.usecase.PerformanceActif
import com.dibitara.app.presentation.common.toCurrencyDisplay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal val etfDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
internal fun CustomAssetCard(
    asset: CustomAsset,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    getTrend: suspend (AssetValuationType, Long) -> Float?,
    getPerformance: suspend (Long, Long, Long, LocalDate, CompteType?) -> PerformanceActif?,
    getHistorique: suspend (AssetValuationType, Long) -> List<AssetValuationSnapshot>,
    vm: EtfViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val error by vm.error.collectAsState()
    var configure by remember { mutableStateOf(false) }
    var purchases by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(false) }
    val plan = state?.data?.plans?.find { it.assetId == asset.id }
    val summary = state?.summaries?.get(asset.id)
    if (state == null) {
        Text(error ?: "Chargement du suivi…")
        return
    }
    if (plan == null || summary == null) {
        LegacyCustomAssetCard(asset, onEdit, onDelete, getTrend, getPerformance, getHistorique,
            onConfigureEtf = { vm.clearError(); configure = true })
    } else {
        var menu by remember { mutableStateOf(false) }
        var delete by remember { mutableStateOf(false) }
        var history by remember { mutableStateOf(false) }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ETF · ${plan.name}", style = MaterialTheme.typography.titleMedium)
                        Text(asset.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Actions ETF") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Configurer l’ETF") }, onClick = { menu = false; vm.clearError(); configure = true })
                            DropdownMenuItem(text = { Text("Supprimer l’actif") }, onClick = { menu = false; delete = true })
                        }
                    }
                }
                Text(asset.totalValueCents.toCurrencyDisplay(asset.currency), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)
                // Deux lignes restent lisibles même sur téléphone étroit et avec une grande police.
                Text("Investi ${summary.investedCents.toCurrencyDisplay(asset.currency)}")
                Text("Gain ${if (summary.gainCents > 0) "+" else ""}${summary.gainCents.toCurrencyDisplay(asset.currency)}",
                    color = if (summary.gainCents >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
                Text("${plan.weeklyAmountCents.toCurrencyDisplay(asset.currency)} / semaine · ${state!!.data.accounts.find { it.id == plan.sourceAccountId }?.label ?: "Compte source supprimé"}",
                    style = MaterialTheme.typography.bodySmall)
                Text("Prochain achat prévu : ${summary.nextPlannedDate.format(etfDateFormat)}", style = MaterialTheme.typography.bodySmall)
                Text("Valeur au ${asset.updatedAt.format(etfDateFormat)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (summary.valueIsOlderThanPurchases) Text("Valeur à actualiser après les derniers achats", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { vm.clearError(); value = true }, modifier = Modifier.fillMaxWidth()) { Text("Actualiser la valeur") }
                Button(onClick = { vm.clearError(); purchases = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (summary.candidates.isEmpty()) "Valider les achats" else "Valider les achats (${summary.candidates.size})")
                }
                TextButton(onClick = { history = !history }) { Text(if (history) "Masquer l’historique" else "Historique") }
                if (history) EtfHistory(summary, asset.currency)
            }
        }
        if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text("Supprimer cet ETF ?") },
            text = { Text("Le suivi et les achats validés seront supprimés. Les opérations bancaires seront conservées.") },
            confirmButton = { TextButton(onClick = { delete = false; onDelete() }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { delete = false }) { Text("Annuler") } })
    }
    if (configure) EtfConfigurationSheet(asset, plan, state!!.data.accounts, vm) { configure = false }
    if (purchases && plan != null && summary != null) EtfPurchasesSheet(plan, summary, state!!.data.purchases, vm) { purchases = false }
    if (value && plan != null) EtfValueDialog(asset, vm) { value = false }
}

@Composable
private fun EtfHistory(summary: EtfSummary, currency: Currency) {
    val valueColor = MaterialTheme.colorScheme.primary
    val investedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val all = summary.investedHistory + summary.valueHistory
    if (all.isEmpty()) return
    val first = all.minOf { it.date }
    val last = all.maxOf { it.date }
    val min = all.minOf { it.cents }.toDouble()
    val max = all.maxOf { it.cents }.toDouble()
    Text("Valeur", color = valueColor, style = MaterialTheme.typography.labelMedium)
    Text("Investi", color = investedColor, style = MaterialTheme.typography.labelMedium)
    Text("${max.toLong().toCurrencyDisplay(currency)}", style = MaterialTheme.typography.labelSmall)
    val accessibleInvested = summary.investedCents.toCurrencyDisplay(currency)
    Canvas(Modifier.fillMaxWidth().height(130.dp).semantics {
        contentDescription = "Historique ETF du ${first.format(etfDateFormat)} au ${last.format(etfDateFormat)}. Capital investi ${accessibleInvested}."
    }) {
        fun point(p: EtfPoint): Offset = Offset(
            6.dp.toPx() + ((p.date.toEpochDay() - first.toEpochDay()).toDouble() / (last.toEpochDay() - first.toEpochDay()).coerceAtLeast(1)).toFloat() * (size.width - 12.dp.toPx()),
            size.height - 6.dp.toPx() - ((p.cents - min) / (max - min).coerceAtLeast(1.0)).toFloat() * (size.height - 12.dp.toPx()))
        fun series(points: List<EtfPoint>, step: Boolean, color: androidx.compose.ui.graphics.Color) {
            val path = Path()
            points.forEachIndexed { i, p ->
                val q = point(p)
                if (i == 0) path.moveTo(q.x, q.y) else {
                    if (step) path.lineTo(q.x, point(points[i - 1]).y)
                    path.lineTo(q.x, q.y)
                }
                drawCircle(color, 3.dp.toPx(), q)
            }
            // Le capital connu reste constant entre deux achats ; aucun cours n'est prolongé.
            if (step && points.isNotEmpty()) path.lineTo(point(EtfPoint(last, points.last().cents)).x, point(points.last()).y)
            drawPath(path, color, style = Stroke(2.dp.toPx()))
        }
        series(summary.investedHistory, true, investedColor)
        series(summary.valueHistory, false, valueColor)
    }
    Text(min.toLong().toCurrencyDisplay(currency), style = MaterialTheme.typography.labelSmall)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(first.format(etfDateFormat), style = MaterialTheme.typography.labelSmall)
        if (first != last) Text(last.format(etfDateFormat), style = MaterialTheme.typography.labelSmall)
    }
}

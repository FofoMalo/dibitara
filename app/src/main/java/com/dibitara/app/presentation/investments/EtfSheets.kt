package com.dibitara.app.presentation.investments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dibitara.app.domain.model.*
import com.dibitara.app.presentation.common.toCurrencyDisplay
import java.math.BigDecimal
import java.time.LocalDate

// Conversion décimale exacte : aucune approximation flottante pour des centimes.
internal fun etfCents(text: String): Long? = runCatching {
    BigDecimal(text.trim().replace(',', '.')).movePointRight(2).longValueExact()
}.getOrNull()
private fun amountText(cents: Long) = BigDecimal.valueOf(cents, 2).toPlainString()
private fun dateValue(text: String): LocalDate? = runCatching {
    LocalDate.parse(text, java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT))
}.getOrNull()

@Composable
private fun MoneyField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}
@Composable
private fun DateField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onChange, label = { Text(label) }, placeholder = { Text("JJ/MM/AAAA") },
        singleLine = true, isError = value.isNotBlank() && dateValue(value) == null, modifier = Modifier.fillMaxWidth())
}
@Composable
private fun EtfError(vm: EtfViewModel) {
    val error by vm.error.collectAsState()
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EtfConfigurationSheet(asset: CustomAsset, plan: EtfPlan?, accounts: List<BankAccount>, vm: EtfViewModel, close: () -> Unit) {
    var name by remember { mutableStateOf(plan?.name ?: "") }
    var invested by remember { mutableStateOf(plan?.initialInvestedCents?.let(::amountText) ?: "") }
    var reference by remember { mutableStateOf((plan?.referenceDate ?: LocalDate.now()).format(etfDateFormat)) }
    var weekly by remember { mutableStateOf(plan?.weeklyAmountCents?.let(::amountText) ?: "") }
    var next by remember { mutableStateOf((plan?.nextPurchaseDate ?: LocalDate.now()).format(etfDateFormat)) }
    val sources = accounts.filter { it.provider == BankProvider.TRADE_REPUBLIC && it.currency == asset.currency }
    var account by remember { mutableStateOf(plan?.sourceAccountId ?: sources.singleOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }
    val busy by vm.busy.collectAsState()
    ModalBottomSheet(onDismissRequest = { if (!busy) close() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 650.dp).verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (plan == null) "Suivre un ETF" else "Configurer l’ETF", style = MaterialTheme.typography.titleLarge)
            Text("${asset.label} · ${asset.currency.symbol}", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it }, label = { Text("Nom de l’ETF") }, modifier = Modifier.fillMaxWidth())
            MoneyField(invested, { invested = it }, "Total investi à la référence (${asset.currency.symbol})")
            DateField(reference, { reference = it }, "Date de référence (jour inclus)")
            Text("Inclure tous les achats et frais jusqu’à cette date. Les anciens mouvements sont conservés, sans être ajoutés une seconde fois.", style = MaterialTheme.typography.bodySmall)
            MoneyField(weekly, { weekly = it }, "Montant par semaine (${asset.currency.symbol})")
            DateField(next, { next = it }, "Prochain achat prévu")
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(sources.find { it.id == account }?.label ?: "Choisir le compte Trade Republic")
                }
                DropdownMenu(expanded, { expanded = false }) {
                    sources.forEach { source -> DropdownMenuItem(text = { Text(source.label) }, onClick = { account = source.id; expanded = false }) }
                }
            }
            if (sources.isEmpty()) Text("Ajoute un compte Trade Republic dans cette devise depuis Paramètres → Comptes bancaires.")
            EtfError(vm)
            val valid = name.isNotBlank() && etfCents(invested)?.let { it >= 0 } == true &&
                etfCents(weekly)?.let { it > 0 } == true && dateValue(reference) != null && dateValue(next) != null && account != null
            Button(onClick = { vm.configure(EtfPlan(asset.id, name, account!!, etfCents(invested)!!,
                dateValue(reference)!!, etfCents(weekly)!!, dateValue(next)!!, asset.currency), close) },
                enabled = valid && !busy, modifier = Modifier.fillMaxWidth()) { Text("Enregistrer") }
        }
    }
}

@Composable
internal fun EtfValueDialog(asset: CustomAsset, vm: EtfViewModel, close: () -> Unit) {
    var value by remember { mutableStateOf(amountText(asset.totalValueCents)) }
    var date by remember { mutableStateOf(LocalDate.now().format(etfDateFormat)) }
    val busy by vm.busy.collectAsState()
    AlertDialog(onDismissRequest = { if (!busy) close() }, title = { Text("Actualiser la valeur") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MoneyField(value, { value = it }, "Valeur totale (${asset.currency.symbol})")
            DateField(date, { date = it }, "Date de valorisation")
            EtfError(vm)
        } }, confirmButton = {
            TextButton(onClick = { vm.value(asset.id, etfCents(value)!!, dateValue(date)!!, close) },
                enabled = !busy && etfCents(value)?.let { it >= 0 } == true && dateValue(date) != null) { Text("Enregistrer") }
        }, dismissButton = { TextButton(onClick = close, enabled = !busy) { Text("Annuler") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EtfPurchasesSheet(plan: EtfPlan, summary: EtfSummary, all: List<EtfPurchase>, vm: EtfViewModel, close: () -> Unit) {
    var selected by remember { mutableStateOf<EtfCandidate?>(null) }
    var manual by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf<EtfPurchase?>(null) }
    val busy by vm.busy.collectAsState()
    val purchases = all.filter { it.assetId == plan.assetId }.sortedByDescending { it.date }
    ModalBottomSheet(onDismissRequest = { if (!busy) close() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Achats · ${plan.name}", style = MaterialTheme.typography.titleLarge)
                Text("Après le ${plan.referenceDate.format(etfDateFormat)}", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { vm.clearError(); manual = true }, enabled = !busy) { Text("Ajouter un achat manuel") }
                EtfError(vm)
                Text("À valider", style = MaterialTheme.typography.titleMedium)
                if (summary.candidates.isEmpty()) Text("Aucun achat importé à valider.", style = MaterialTheme.typography.bodySmall)
            }
            items(summary.candidates, key = { "tx:${it.transaction.id}" }) { candidate ->
                Column {
                    Text(candidate.transaction.note.ifBlank { "Opération Trade Republic" })
                    Text("${candidate.transaction.date.format(etfDateFormat)} · ${candidate.transaction.amountCents.toCurrencyDisplay(plan.currency)}")
                    Text(if (candidate.matchesName) "ETF reconnu · à confirmer" else "ETF à vérifier", style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { vm.clearError(); selected = candidate }, enabled = !busy) { Text("Vérifier cet achat") }
                }
                HorizontalDivider()
            }
            item { Text("Achats validés", style = MaterialTheme.typography.titleMedium) }
            items(purchases, key = { "buy:${it.id}" }) { purchase ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${purchase.date.format(etfDateFormat)} · ${purchase.totalCents.toCurrencyDisplay(plan.currency)}")
                        Text(if (purchase.transactionId == null) "Saisie manuelle" else "Lié à une opération", style = MaterialTheme.typography.labelSmall)
                        if (purchase.feesCents > 0) Text("Dont frais ajoutés : ${purchase.feesCents.toCurrencyDisplay(plan.currency)}", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { vm.clearError(); remove = purchase }, enabled = !busy) { Text("Retirer") }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    selected?.let { candidate ->
        EtfImportedPurchaseDialog(plan, candidate, vm) { selected = null }
    }
    if (manual) EtfManualPurchaseDialog(plan, purchases, vm) { manual = false }
    remove?.let { purchase -> AlertDialog(onDismissRequest = { remove = null }, title = { Text("Retirer cet achat du suivi ?") },
        text = { Column { Text("L’opération bancaire sera conservée. L’achat pourra être validé de nouveau."); EtfError(vm) } },
        confirmButton = { TextButton(onClick = { vm.remove(plan.assetId, purchase.id) { remove = null } }, enabled = !busy) { Text("Retirer") } },
        dismissButton = { TextButton(onClick = { remove = null }, enabled = !busy) { Text("Annuler") } }) }
}

@Composable
private fun EtfImportedPurchaseDialog(plan: EtfPlan, candidate: EtfCandidate, vm: EtfViewModel, close: () -> Unit) {
    var fees by remember(candidate.transaction.id) { mutableStateOf("0") }
    var distinct by remember(candidate.transaction.id) { mutableStateOf(false) }
    val busy by vm.busy.collectAsState()
    val tx = candidate.transaction
    AlertDialog(onDismissRequest = { if (!busy) close() }, title = { Text("Achat de ${plan.name} ?") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tx.note)
            Text("${tx.date.format(etfDateFormat)} · ${tx.amountCents.toCurrencyDisplay(plan.currency)}")
            Text("Confirme qu’il s’agit bien d’un achat de cet ETF.", style = MaterialTheme.typography.bodySmall)
            if (candidate.manualMatches.isNotEmpty()) {
                Text("Un achat manuel peut correspondre :")
                candidate.manualMatches.forEach { purchase ->
                    OutlinedButton(onClick = { vm.link(plan.assetId, purchase.id, tx.id, close) }, enabled = !busy) {
                        Text("Associer au ${purchase.date.format(etfDateFormat)} · ${purchase.totalCents.toCurrencyDisplay(plan.currency)}")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(distinct, { distinct = it }); Text("C’est un autre achat")
                }
            }
            MoneyField(fees, { fees = it }, "Frais non inclus dans le montant (${plan.currency.symbol})")
            EtfError(vm)
        } }, confirmButton = { TextButton(onClick = {
            vm.confirm(EtfPurchase(assetId = plan.assetId, date = tx.date, amountCents = tx.amountCents,
                feesCents = etfCents(fees)!!, transactionId = tx.id), distinct, close)
        }, enabled = !busy && etfCents(fees)?.let { it >= 0 } == true && (candidate.manualMatches.isEmpty() || distinct)) { Text("Valider l’achat") } },
        dismissButton = { TextButton(onClick = close, enabled = !busy) { Text("Annuler") } })
}

@Composable
private fun EtfManualPurchaseDialog(plan: EtfPlan, purchases: List<EtfPurchase>, vm: EtfViewModel, close: () -> Unit) {
    var amount by remember { mutableStateOf(amountText(plan.weeklyAmountCents)) }
    var fees by remember { mutableStateOf("0") }
    var date by remember { mutableStateOf(LocalDate.now().format(etfDateFormat)) }
    var distinct by remember { mutableStateOf(false) }
    val busy by vm.busy.collectAsState()
    val duplicate = purchases.any { it.date == dateValue(date) && (it.amountCents == etfCents(amount) ||
        (etfCents(amount) != null && etfCents(fees) != null && runCatching { Math.addExact(etfCents(amount)!!, etfCents(fees)!!) }.getOrNull() == it.totalCents)) }
    AlertDialog(onDismissRequest = { if (!busy) close() }, title = { Text("Achat exécuté") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MoneyField(amount, { amount = it; distinct = false }, "Montant (${plan.currency.symbol})")
            MoneyField(fees, { fees = it; distinct = false }, "Frais non inclus (${plan.currency.symbol})")
            DateField(date, { date = it; distinct = false }, "Date d’achat")
            if (duplicate) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(distinct, { distinct = it }); Text("Un achat similaire existe : c’est un autre achat")
            }
            EtfError(vm)
        } }, confirmButton = { TextButton(onClick = {
            vm.confirm(EtfPurchase(assetId = plan.assetId, date = dateValue(date)!!, amountCents = etfCents(amount)!!, feesCents = etfCents(fees)!!), distinct, close)
        }, enabled = !busy && dateValue(date) != null && etfCents(amount)?.let { it > 0 } == true &&
            etfCents(fees)?.let { it >= 0 } == true && (!duplicate || distinct)) { Text("Valider l’achat") } },
        dismissButton = { TextButton(onClick = close, enabled = !busy) { Text("Annuler") } })
}

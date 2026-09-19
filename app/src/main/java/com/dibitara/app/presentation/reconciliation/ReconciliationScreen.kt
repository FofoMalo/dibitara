package com.dibitara.app.presentation.reconciliation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.presentation.common.toCurrencyDisplay

@Composable
fun ReconciliationScreen(onBack: () -> Unit, onTransactions: (Long, Int, Int) -> Unit,
    viewModel: ReconciliationViewModel = hiltViewModel()) {
    val form by viewModel.input.collectAsState()
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("Retour") }
        Text("Rapprocher un relevé", style = MaterialTheme.typography.headlineSmall)
        Text("Comparez les soldes du relevé avec les opérations enregistrées. Cette vérification ne modifie aucune donnée.")
        Text("1. Choisir le compte", style = MaterialTheme.typography.titleMedium)
        if (state.accounts.isEmpty()) Text("Ajoutez d’abord un compte bancaire depuis Plus.")
        state.accounts.forEach { account ->
            FilterChip(selected = form.accountId == account.id, onClick = { viewModel.change(form.copy(accountId = account.id)) }, label = { Text("${account.label} · ${account.currency.isoCode}") })
        }
        Text("2. Mois du relevé", style = MaterialTheme.typography.titleMedium)
        Text(form.month.toString(), style = MaterialTheme.typography.titleLarge)
        Row {
            TextButton(onClick = { viewModel.change(form.copy(month = form.month.minusMonths(1), opening = "", closing = "")) }) { Text("Précédent") }
            TextButton(onClick = { viewModel.change(form.copy(month = form.month.plusMonths(1), opening = "", closing = "")) }) { Text("Suivant") }
        }
        Text("3. Reporter les deux soldes", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(form.opening, { viewModel.change(form.copy(opening = it)) }, label = { Text("Solde au début du mois") },
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(form.closing, { viewModel.change(form.copy(closing = it)) }, label = { Text("Solde à la fin du mois") },
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val account = state.accounts.firstOrNull { it.id == form.accountId }
        state.result?.let { result ->
            Text("${result.nombreOperations} opérations dans la devise du compte")
            if (account != null) {
                Text("Solde calculé : ${result.soldeCalculeCents.toCurrencyDisplay(account.currency)}")
                Text("Écart avec le relevé : ${result.ecartCents.toCurrencyDisplay(account.currency)}", style = MaterialTheme.typography.titleLarge)
            }
            Text("Calcul : solde initial + entrées − sorties. Les virements internes sont inclus pour ce compte.")
            if (result.devisesNonComparees > 0) Text("${result.devisesNonComparees} opérations dans une autre devise ne sont pas comparées : vérification manuelle nécessaire.")
            if (result.investissementsAVerifier > 0) Text("Les investissements sont traités comme des sorties. Vérifiez leur sens sur le relevé.")
            Text(if (result.ecartCents == 0L && result.devisesNonComparees == 0 && result.investissementsAVerifier == 0)
                "Les soldes concordent. Vérifiez aussi le détail des opérations : des erreurs peuvent se compenser."
                else "Vérifiez les opérations manquantes, les doublons, les dates et les soldes saisis.")
        }
        if (account != null) OutlinedButton(onClick = { onTransactions(account.id, form.month.monthValue, form.month.year) }) { Text("Vérifier les opérations du mois") }
    }
}

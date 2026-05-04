package com.dibitara.app.presentation.investments

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// TODO Sprint 3 : portefeuille d'investissements + graphiques
@Composable
fun InvestmentsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Investissements — Sprint 3", style = MaterialTheme.typography.titleLarge)
    }
}

package com.dibitara.app.presentation.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// TODO Sprint 2 : liste des dépenses + ajout rapide
@Composable
fun ExpensesScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Dépenses — Sprint 2", style = MaterialTheme.typography.titleLarge)
    }
}

package com.dibitara.app.presentation.budget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// TODO Sprint 2 : implémenter la saisie et le suivi du budget mensuel
@Composable
fun BudgetScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Budget — Sprint 2", style = MaterialTheme.typography.titleLarge)
    }
}

package com.dibitara.app.presentation.common

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier

/** L'explication reste accessible sans occuper la place du montant dans la synthèse. */
@Composable
fun MetricExplanation(title: String, explanation: String) {
    var opened by remember { mutableStateOf(false) }
    TextButton(onClick = { opened = true }) { Text("Comprendre ce montant") }
    if (opened) AlertDialog(
        onDismissRequest = { opened = false },
        title = { Text(title) },
        text = { Text(explanation, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { opened = false }) { Text("Fermer") } }
    )
}

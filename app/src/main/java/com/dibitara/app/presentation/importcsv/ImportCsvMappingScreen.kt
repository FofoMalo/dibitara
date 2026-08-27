package com.dibitara.app.presentation.importcsv

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dibitara.app.data.importcsv.CsvColumnDetector
import com.dibitara.app.domain.model.CsvColumnMapping
import com.dibitara.app.domain.model.CsvImportPreview
import com.dibitara.app.domain.model.ModeMontant

/**
 * Écran de mapping manuel des colonnes, présenté quand l'auto-détection n'a pas
 * suffi (ou sur demande depuis l'aperçu). Pré-rempli avec ce qui a été deviné.
 *
 * L'utilisateur associe chaque champ Dibitara (date, montant, libellé…) à une
 * colonne du fichier, en s'appuyant sur l'aperçu des premières lignes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCsvMappingScreen(
    preview: CsvImportPreview,
    onValider: (CsvColumnMapping) -> Unit,
    onAnnuler: () -> Unit,
) {
    val base = preview.mapping
    val nbColonnes = maxOf(
        preview.enTetes.size,
        preview.echantillon.maxOfOrNull { it.size } ?: 0,
    )

    var aEnTete by remember { mutableStateOf(base.aEnTete) }
    var colDate by remember { mutableIntStateOf(base.colonneDate.coerceAtLeast(0)) }
    var formatDate by remember { mutableStateOf(base.formatDate) }
    var mode by remember { mutableStateOf(base.modeMontant) }
    var colMontant by remember { mutableIntStateOf(base.colonneMontant ?: 0) }
    var colDebit by remember { mutableIntStateOf(base.colonneDebit ?: 0) }
    var colCredit by remember { mutableIntStateOf(base.colonneCredit ?: 0) }
    var sepDecimal by remember { mutableStateOf(base.separateurDecimal) }
    var colLibelle by remember { mutableIntStateOf(base.colonnesLibelle.firstOrNull() ?: 0) }
    var colDevise by remember { mutableStateOf(base.colonneDevise) }

    val libellesColonnes = (0 until nbColonnes).map { i ->
        preview.enTetes.getOrNull(i)?.takeIf { it.isNotBlank() } ?: "Colonne ${i + 1}"
    }

    fun construire() = CsvColumnMapping(
        delimiteur = base.delimiteur,
        aEnTete = aEnTete,
        colonneDate = colDate,
        formatDate = formatDate,
        modeMontant = mode,
        colonneMontant = colMontant.takeIf { mode == ModeMontant.COLONNE_SIGNEE },
        colonneDebit = colDebit.takeIf { mode == ModeMontant.DEBIT_CREDIT },
        colonneCredit = colCredit.takeIf { mode == ModeMontant.DEBIT_CREDIT },
        separateurDecimal = sepDecimal,
        colonnesLibelle = listOf(colLibelle),
        colonneDevise = colDevise,
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Associer les colonnes", style = MaterialTheme.typography.titleMedium)
        Text(
            "Les colonnes de votre fichier n'ont pas toutes été reconnues. " +
                "Indiquez ci-dessous où trouver chaque information.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ApercuTableau(preview.enTetes, preview.echantillon, aEnTete)

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = aEnTete, onCheckedChange = { aEnTete = it })
            Text("La première ligne est un en-tête")
        }

        ColonneSelector("Date", libellesColonnes, colDate) { colDate = it }
        ChoixParmi("Format de date", CsvColumnDetector.FORMATS_DATE, formatDate) { formatDate = it }

        ChoixParmi(
            "Montant",
            listOf(ModeMontant.COLONNE_SIGNEE, ModeMontant.DEBIT_CREDIT),
            mode,
            libelle = { if (it == ModeMontant.COLONNE_SIGNEE) "Une colonne signée" else "Débit / Crédit séparés" },
        ) { mode = it }

        if (mode == ModeMontant.COLONNE_SIGNEE) {
            ColonneSelector("Colonne montant", libellesColonnes, colMontant) { colMontant = it }
        } else {
            ColonneSelector("Colonne débit", libellesColonnes, colDebit) { colDebit = it }
            ColonneSelector("Colonne crédit", libellesColonnes, colCredit) { colCredit = it }
        }

        ChoixParmi(
            "Séparateur décimal",
            listOf(',', '.'),
            sepDecimal,
            libelle = { if (it == ',') "Virgule (1 234,56)" else "Point (1,234.56)" },
        ) { sepDecimal = it }

        ColonneSelector("Libellé", libellesColonnes, colLibelle) { colLibelle = it }

        ColonneSelectorOptionnel("Devise (facultatif)", libellesColonnes, colDevise) { colDevise = it }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onAnnuler, modifier = Modifier.weight(1f)) { Text("Annuler") }
            Button(
                onClick = { onValider(construire()) },
                enabled = construire().estComplet,
                modifier = Modifier.weight(1f),
            ) { Text("Continuer") }
        }
    }
}

// ─── Aperçu tabulaire ───────────────────────────────────────────────────────

@Composable
private fun ApercuTableau(enTetes: List<String>, echantillon: List<List<String>>, aEnTete: Boolean) {
    val nbColonnes = maxOf(enTetes.size, echantillon.maxOfOrNull { it.size } ?: 0)
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.horizontalScroll(rememberScrollState()).padding(8.dp)) {
            if (aEnTete && enTetes.any { it.isNotBlank() }) {
                CelluleLigne((0 until nbColonnes).map { enTetes.getOrNull(it).orEmpty() }, gras = true)
                HorizontalDivider()
            }
            echantillon.take(5).forEach { ligne ->
                CelluleLigne((0 until nbColonnes).map { ligne.getOrNull(it).orEmpty() }, gras = false)
            }
        }
    }
}

@Composable
private fun CelluleLigne(valeurs: List<String>, gras: Boolean) {
    Row {
        valeurs.forEach { v ->
            Text(
                text = v.ifBlank { "—" },
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (gras) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.widthIn(min = 90.dp).padding(horizontal = 4.dp, vertical = 2.dp),
                maxLines = 1,
            )
        }
    }
}

// ─── Sélecteurs ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColonneSelector(
    label: String,
    colonnes: List<String>,
    selection: Int,
    onSelect: (Int) -> Unit,
) {
    ChoixParmi(label, colonnes.indices.toList(), selection, libelle = { colonnes.getOrNull(it) ?: "—" }, onSelect = onSelect)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColonneSelectorOptionnel(
    label: String,
    colonnes: List<String>,
    selection: Int?,
    onSelect: (Int?) -> Unit,
) {
    val options: List<Int?> = listOf(null) + colonnes.indices.toList()
    ChoixParmi(
        label,
        options,
        selection,
        libelle = { if (it == null) "Aucune" else colonnes.getOrNull(it) ?: "—" },
        onSelect = onSelect,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoixParmi(
    label: String,
    options: List<T>,
    selection: T,
    libelle: (T) -> String = { it.toString() },
    onSelect: (T) -> Unit,
) {
    var ouvert by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = ouvert, onExpandedChange = { ouvert = it }) {
        OutlinedTextField(
            value = libelle(selection),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ouvert) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(libelle(option)) },
                    onClick = { onSelect(option); ouvert = false },
                )
            }
        }
    }
}

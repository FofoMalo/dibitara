package com.dibitara.app.domain.model

/**
 * Résultat de l'analyse d'un fichier CSV, **avant** toute écriture en base.
 *
 * Deux cas :
 *  - [mapping] est [CsvColumnMapping.estComplet] → [transactions] est renseignée,
 *    l'écran d'aperçu peut être affiché directement ;
 *  - sinon → [transactions] est vide, l'écran de mapping manuel doit être présenté
 *    (pré-rempli avec [mapping], en s'appuyant sur [enTetes] et [echantillon]).
 */
data class CsvImportPreview(
    val mapping: CsvColumnMapping,
    /** Noms de colonnes : l'en-tête réel, ou "Colonne 1", "Colonne 2"… si absent. */
    val enTetes: List<String>,
    /** 5 premières lignes de données (hors en-tête), pour l'aperçu de l'écran de mapping. */
    val echantillon: List<List<String>>,
    val transactions: List<ImportedTransaction>,
    /** Nombre de lignes de données écartées (date ou montant non parseable, montant nul). */
    val lignesIgnorees: Int,
)

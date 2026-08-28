package com.dibitara.app.domain.usecase

import com.dibitara.app.data.importcsv.CsvColumnDetector
import com.dibitara.app.data.importcsv.CsvParser
import com.dibitara.app.data.importcsv.CsvRowParser
import com.dibitara.app.domain.model.CsvColumnMapping
import com.dibitara.app.domain.model.CsvImportPreview
import com.dibitara.app.domain.model.Currency
import javax.inject.Inject

/**
 * Analyse un fichier CSV (lignes déjà lues depuis le flux par le ViewModel) et
 * produit un [CsvImportPreview]. **Ne touche pas la base** : la persistance est
 * le rôle de [ImporterTransactionsCsvUseCase], après validation par l'utilisateur.
 *
 * Étapes :
 *  1. détecter le délimiteur (ou reprendre celui du mapping imposé) ;
 *  2. découper les lignes ([CsvParser]) ;
 *  3. deviner le mapping ([CsvColumnDetector]) — sauf si [mappingImpose] fourni
 *     (l'utilisateur est passé par l'écran de mapping manuel) ;
 *  4. si le mapping est incomplet, s'arrêter là (l'écran de mapping s'ouvrira) ;
 *  5. sinon parser les lignes ([CsvRowParser]) et suggérer une catégorie par ligne.
 *
 * Note d'architecture : les objets `data.importcsv.*` référencés ici sont du
 * Kotlin pur, sans dépendance Android ni base — les faire orchestrer par un
 * UseCase plutôt que par le ViewModel respecte la règle « aucune logique métier
 * dans presentation/ ». Même compromis que `SupprimerToutesDonneesUseCase`.
 *
 * @param lignesBrutes contenu du fichier, une entrée par ligne
 * @param deviseParDefaut devise appliquée quand le CSV n'a pas de colonne devise
 * @param mappingImpose mapping ajusté par l'utilisateur, ou null pour auto-détection
 */
class AnalyserCsvUseCase @Inject constructor(
    private val suggererCategorie: SuggererCategorieImportUseCase,
) {
    suspend operator fun invoke(
        lignesBrutes: List<String>,
        deviseParDefaut: Currency,
        mappingImpose: CsvColumnMapping? = null,
    ): CsvImportPreview {
        val lignes = lignesBrutes.filter { it.isNotBlank() }
        val delimiteur = mappingImpose?.delimiteur ?: CsvParser.detecterDelimiteur(lignes.take(15))
        val rows = lignes.map { CsvParser.parseLigne(it, delimiteur) }
        val mapping = mappingImpose ?: CsvColumnDetector.detecter(rows, delimiteur)

        val enTetes = enTetes(rows, mapping)
        val donnees = if (mapping.aEnTete) rows.drop(1) else rows
        val echantillon = donnees.take(5)

        if (!mapping.estComplet) {
            return CsvImportPreview(mapping, enTetes, echantillon, transactions = emptyList(), lignesIgnorees = 0)
        }

        val resultat = CsvRowParser.parser(donnees, mapping, deviseParDefaut)
        val transactions = resultat.transactions.map {
            val suggestion = suggererCategorie(it.note)
            it.copy(
                category = suggestion.category,
                subCategory = suggestion.subCategory,
                customSubCategoryId = suggestion.customSubCategoryId,
            )
        }

        return CsvImportPreview(mapping, enTetes, echantillon, transactions, resultat.lignesIgnorees)
    }

    /** En-tête réel si présent, sinon "Colonne 1", "Colonne 2"… sur le nombre max de colonnes. */
    private fun enTetes(rows: List<List<String>>, mapping: CsvColumnMapping): List<String> {
        if (rows.isEmpty()) return emptyList()
        val nbColonnes = rows.maxOf { it.size }
        return if (mapping.aEnTete) {
            rows.first().let { entete -> (0 until nbColonnes).map { entete.getOrNull(it)?.trim().orEmpty() } }
        } else {
            (1..nbColonnes).map { "Colonne $it" }
        }
    }
}

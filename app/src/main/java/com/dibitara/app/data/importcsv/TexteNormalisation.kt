package com.dibitara.app.data.importcsv

import java.text.Normalizer

/**
 * Normalisation de texte partagée par l'import CSV.
 *
 * Reprend la logique de l'ancien `BredCategoriseur.normaliser` (retiré au
 * commit 01d468d), utile à deux endroits :
 *  - [CsvColumnDetector] compare des noms d'en-tête ("Libellé" ≈ "libelle")
 *  - la génération de l'`externalId` a besoin d'un libellé stable pour le hash
 */
object TexteNormalisation {

    /**
     * Minuscules, sans accents (décomposition NFD puis retrait des non-ASCII),
     * espaces multiples réduits à un seul, extrémités tronquées.
     *
     * "  Paiement   CB  Épicerie " → "paiement cb epicerie"
     */
    fun cle(texte: String): String =
        Normalizer.normalize(texte.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("[^\\p{ASCII}]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}

package com.dibitara.app.data.importcsv

/**
 * Parsing CSV bas niveau : découpage en champs et détection du délimiteur.
 * Objet Kotlin pur, sans dépendance Android ni connaissance métier bancaire.
 *
 * La détection d'encodage (UTF-8 puis repli ISO-8859-1) appartient à l'appelant
 * qui a accès au flux — voir `AnalyserCsvUseCase`.
 *
 * [parseLigne] est conforme RFC 4180 : guillemets doubles, guillemets doublés
 * à l'intérieur d'une valeur, délimiteur neutralisé entre guillemets, champ
 * vide final ("a;b;" → 3 champs).
 */
object CsvParser {

    /** Délimiteurs testés par [detecterDelimiteur], du plus au moins courant en Europe. */
    private val DELIMITEURS_CANDIDATS = listOf(';', ',', '\t')

    /**
     * Devine le délimiteur à partir d'un échantillon de lignes (en-tête inclus).
     *
     * Heuristique : une vraie colonne CSV produit le **même nombre de champs**
     * sur chaque ligne. On retient le candidat qui découpe en au moins 2 colonnes
     * de façon la plus régulière possible ; à régularité égale, celui qui donne
     * le plus de colonnes. Une virgule décimale, elle, fait varier le compte
     * d'une ligne à l'autre et sera écartée.
     */
    fun detecterDelimiteur(echantillon: List<String>): Char {
        val lignes = echantillon.filter { it.isNotBlank() }.take(10)
        if (lignes.isEmpty()) return ';'

        return DELIMITEURS_CANDIDATS.maxByOrNull { delim -> scoreDelimiteur(lignes, delim) }
            ?: ';'
    }

    private fun scoreDelimiteur(lignes: List<String>, delim: Char): Int {
        val comptes = lignes.map { parseLigne(it, delim).size }
        val minColonnes = comptes.min()
        if (minColonnes <= 1) return -1   // ce délimiteur ne découpe rien d'utile

        val regulier = comptes.all { it == comptes.first() }
        // Le bonus de régularité domine le simple nombre de colonnes.
        return minColonnes + if (regulier) 100 else 0
    }

    /**
     * Découpe [ligne] en champs selon [delimiteur], en respectant les guillemets.
     * Ne gère pas les valeurs multi-lignes (guillemet non refermé en fin de ligne) —
     * les exports bancaires n'en produisent pas.
     */
    fun parseLigne(ligne: String, delimiteur: Char): List<String> {
        val champs = mutableListOf<String>()
        var i = 0
        while (i < ligne.length) {
            if (ligne[i] == '"') {
                val sb = StringBuilder()
                i++
                while (i < ligne.length) {
                    when {
                        ligne[i] == '"' && i + 1 < ligne.length && ligne[i + 1] == '"' -> {
                            sb.append('"'); i += 2
                        }
                        ligne[i] == '"' -> { i++; break }
                        else -> { sb.append(ligne[i]); i++ }
                    }
                }
                champs.add(sb.toString())
                if (i < ligne.length && ligne[i] == delimiteur) i++
            } else {
                val fin = ligne.indexOf(delimiteur, i).takeIf { it >= 0 } ?: ligne.length
                champs.add(ligne.substring(i, fin))
                i = if (fin < ligne.length) fin + 1 else fin
            }
        }
        // "a;b;" se termine sur un délimiteur → un dernier champ vide (RFC 4180).
        if (ligne.isNotEmpty() && ligne.last() == delimiteur) champs.add("")
        return champs
    }
}

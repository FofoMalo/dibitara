package com.dibitara.app.data.importcsv

/**
 * Parse un montant tel qu'il apparaît dans un CSV bancaire, quel que soit le
 * style régional. Objet Kotlin pur, aucune dépendance Android.
 *
 * Gère :
 *  - décimale virgule (FR)      : "1 234,56"   "-45,99"
 *  - décimale point   (EN)      : "1,234.56"   "-45.99"
 *  - séparateur de milliers     : espace, espace insécable, point (FR) ou virgule (EN)
 *  - signe négatif              : "-", tiret long unicode, ou parenthèses
 *                                 comptables "(45,99)"
 *  - symboles/mentions parasites : "EUR", symbole euro/dollar, espaces, guillemets
 *
 * Le séparateur décimal doit être fourni : "1.234" vaut mille deux-cent-trente-quatre
 * en notation FR mais un-virgule-deux-trois-quatre en notation EN. La détection du
 * séparateur se fait en amont, sur l'ensemble d'une colonne (voir [CsvColumnDetector]),
 * pas valeur par valeur — d'où [detecterSeparateurDecimal] exposé séparément.
 */
object MontantParser {

    // Espaces servant de séparateur de milliers : ordinaire, insécable (U+00A0),
    // fine insécable (U+202F).
    private val ESPACES = Regex("[   ]")

    // Tiret long "moins" U+2212, parfois utilisé par les exports à la place de '-'.
    private const val MOINS_UNICODE = '−'

    /**
     * Parse [brut] en connaissant le [separateurDecimal] (',' ou '.').
     * Retourne null si la chaîne ne contient aucun nombre exploitable.
     */
    fun parse(brut: String, separateurDecimal: Char): Double? {
        var s = brut.trim().removeSurrounding("\"").trim()
        if (s.isEmpty()) return null

        // Parenthèses comptables : "(123,45)" signifie -123,45
        var negatif = false
        if (s.startsWith("(") && s.endsWith(")")) {
            negatif = true
            s = s.substring(1, s.length - 1)
        }

        s = s.replace(ESPACES, "")
            .replace(MOINS_UNICODE, '-')
            .replace(Regex("[^0-9.,\\-]"), "")   // retire symboles de devise et lettres

        if (s.isEmpty() || s == "-") return null

        // Le séparateur des milliers est l'autre caractère : on le supprime,
        // puis on ramène le séparateur décimal à un point pour toDoubleOrNull().
        val separateurMilliers = if (separateurDecimal == ',') '.' else ','
        s = s.replace(separateurMilliers.toString(), "")
        if (separateurDecimal == ',') s = s.replace(',', '.')

        val valeur = s.toDoubleOrNull() ?: return null
        return if (negatif) -valeur else valeur
    }

    /**
     * Devine le séparateur décimal d'une seule valeur :
     *  - les deux séparateurs présents  → le dernier rencontré est le décimal
     *  - virgule seule                  → décimale si suivie de 1 à 2 chiffres en fin
     *  - point seul                     → décimale si suivi de 1 à 2 chiffres en fin
     *  - aucun                          → '.' par défaut (sans effet, pas de décimale)
     *
     * [CsvColumnDetector] agrège ce résultat sur plusieurs lignes de la colonne
     * montant pour trancher les cas ambigus (ex. "1.234").
     */
    fun detecterSeparateurDecimal(brut: String): Char {
        val nettoye = brut.trim().replace(Regex("[^0-9.,]"), "")
        val dernierPoint = nettoye.lastIndexOf('.')
        val derniereVirgule = nettoye.lastIndexOf(',')

        return when {
            dernierPoint >= 0 && derniereVirgule >= 0 ->
                if (dernierPoint > derniereVirgule) '.' else ','

            derniereVirgule >= 0 ->
                if (nettoye.length - derniereVirgule - 1 in 1..2) ',' else '.'

            dernierPoint >= 0 ->
                if (nettoye.length - dernierPoint - 1 in 1..2) '.' else ','

            else -> '.'
        }
    }
}

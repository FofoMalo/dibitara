package com.dibitara.app.domain.model

/**
 * Comment le montant est représenté dans le fichier CSV source.
 */
enum class ModeMontant {
    /** Une seule colonne signée : négatif = dépense, positif = revenu. */
    COLONNE_SIGNEE,

    /** Deux colonnes distinctes : l'une porte les débits, l'autre les crédits. */
    DEBIT_CREDIT
}

/**
 * Indique, pour un fichier CSV donné, quelle colonne porte quelle information.
 *
 * Produit par `CsvColumnDetector` (deviné à partir des en-têtes et du contenu),
 * puis éventuellement ajusté par l'utilisateur dans l'écran de mapping.
 * Data class pure, sans dépendance Android.
 *
 * Tous les index sont 0-based et pointent dans les champs d'une ligne découpée
 * par [delimiteur].
 */
data class CsvColumnMapping(
    val delimiteur: Char,
    val aEnTete: Boolean,
    val colonneDate: Int,
    val formatDate: String,                 // pattern DateTimeFormatter, ex. "dd/MM/yyyy"
    val modeMontant: ModeMontant,
    val colonneMontant: Int? = null,        // renseigné si modeMontant == COLONNE_SIGNEE
    val colonneDebit: Int? = null,          // renseigné si modeMontant == DEBIT_CREDIT
    val colonneCredit: Int? = null,         // renseigné si modeMontant == DEBIT_CREDIT
    val separateurDecimal: Char,            // ',' ou '.'
    val colonnesLibelle: List<Int>,         // concaténées avec un espace si plusieurs
    val colonneDevise: Int? = null,         // sinon devise par défaut de l'utilisateur
) {

    /**
     * true si le mapping contient le minimum requis pour parser une transaction :
     * une date, un montant (selon le mode) et au moins une colonne de libellé.
     * Quand c'est false, l'écran de mapping doit être présenté à l'utilisateur.
     */
    val estComplet: Boolean
        get() {
            val montantOk = when (modeMontant) {
                ModeMontant.COLONNE_SIGNEE -> colonneMontant != null
                ModeMontant.DEBIT_CREDIT -> colonneDebit != null && colonneCredit != null
            }
            return colonneDate >= 0 && montantOk && colonnesLibelle.isNotEmpty()
        }
}

package com.dibitara.app.domain.model

import java.time.LocalDate

/**
 * Entité métier centrale : représente une transaction financière.
 *
 * IMPORTANT : [amountCents] est toujours en centimes de la devise [currency].
 * Ne jamais stocker de Double pour les montants financiers (risque d'arrondi).
 * Exemple : 12,50 € → amountCents = 1250, currency = Currency.EUR
 */
data class Transaction(
    val id: Long = 0,
    val amountCents: Long,
    val currency: Currency,
    val category: Category,
    val type: TransactionType,
    val date: LocalDate,
    val note: String = "",
    val childId: Long? = null,                      // Identifiant de l'enfant associé (null si pas d'enfant)
    val isRecurring: Boolean = false,               // true = cette transaction est un modèle récurrent
    val recurrenceDay: Int? = null,                 // Jour du mois (1-28) - utilisé pour MONTHLY uniquement
    val sourceRecurringId: Long? = null,            // ID du modèle qui a généré cette occurrence
    val subCategory: SubCategory? = null,           // Non-null uniquement si category == AUTRE (enum fixe)
    val customSubCategoryId: Long? = null,          // Référence à une CustomSubCategory créée par l'utilisateur
    val recurrenceFrequency: RecurrenceFrequency? = null, // null si non-récurrent ou MONTHLY (rétrocompat)
    val firstPaymentDate: LocalDate? = null,        // Date de la première occurrence (détermine le jour pour WEEKLY/YEARLY)
    val endDate: LocalDate? = null,                 // Date de fin de récurrence (null = indéfini)
    val importSource: String? = null,               // Ajouté en v11 : source de l'import ("trade_republic"), null si saisie manuelle
    val externalId: String? = null,                 // Ajouté en v11 : UUID externe pour la déduplication à l'import
    val bankAccountId: Long? = null,                // Ajouté en v22 : référence au BankAccount rattaché (null si non déterminé)
    val notificationExternalId: String? = null,     // Alias conservé après rapprochement avec un CSV
    val categoryConfirmed: Boolean = false,
    @Transient val categoryPath: String? = null,
    val reconciliationKey: String? = null          // Nature + support/marchand, indépendante de la catégorie modifiable
)

enum class TransactionType { EXPENSE, INCOME, INVESTMENT }

enum class Currency(val symbol: String, val isoCode: String) {
    EUR("€", "EUR"),
    USD("$", "USD"),
    XOF("FCFA", "XOF"),
    XAF("FCFA", "XAF"),
    // "CA$" plutôt que "$" seul pour ne pas se confondre avec USD à l'affichage.
    CAD("CA$", "CAD")
}

/** Identité stable : les noms historiques restent inchangés en base. Les catégories
 * personnelles ont un identifiant USER_UUID ; le catalogue porte leur présentation.
 * L’égalité ignore le libellé pour préserver les regroupements après renommage. */
class Category(val name: String, val displayName: String = "Catégorie personnelle", val icon: String = "DEFAULT", val color: String = "DEFAULT") {
    override fun equals(other: Any?) = other is Category && name == other.name
    override fun hashCode() = name.hashCode()
    override fun toString() = name
    companion object {
        val ALIMENTATION = Category("ALIMENTATION", "Alimentation")
        val LOGEMENT = Category("LOGEMENT", "Logement")
        val TRANSPORT = Category("TRANSPORT", "Transport")
        val SANTE = Category("SANTE", "Santé")
        val LOISIRS = Category("LOISIRS", "Loisirs")
        val ABONNEMENTS = Category("ABONNEMENTS", "Abonnements")
        val INVESTISSEMENT = Category("INVESTISSEMENT", "Investissement")
        val EPARGNE = Category("EPARGNE", "Épargne")
        val ENFANT = Category("ENFANT", "Enfant")
        val EDUCATION = Category("EDUCATION", "Éducation")
        val HABILLEMENT = Category("HABILLEMENT", "Habillement")
        val IMPOTS_CHARGES = Category("IMPOTS_CHARGES", "Impôts & charges")
        val ASSURANCES = Category("ASSURANCES", "Assurances")
        val TRANSFERTS = Category("TRANSFERTS", "Transferts")
        val TRANSFERTS_FAMILIAUX = Category("TRANSFERTS_FAMILIAUX", "Transferts famille")
        val TABAC = Category("TABAC", "Tabac")
        val AUTRE = Category("AUTRE", "Autre")
        val entries: List<Category> = listOf(ALIMENTATION,LOGEMENT,TRANSPORT,SANTE,LOISIRS,ABONNEMENTS,INVESTISSEMENT,EPARGNE,ENFANT,EDUCATION,HABILLEMENT,IMPOTS_CHARGES,ASSURANCES,TRANSFERTS,TRANSFERTS_FAMILIAUX,TABAC,AUTRE)
        fun valueOf(name: String): Category = entries.firstOrNull { it.name == name }
            ?: if (name.matches(Regex("USER_[a-f0-9-]{36}"))) Category(name)
            else throw IllegalArgumentException("Catégorie inconnue : $name")
        fun values(): Array<Category> = entries.toTypedArray()
    }
}

// Sous-catégories prédéfinies, utilisées uniquement quand category == AUTRE
enum class SubCategory(val displayName: String) {
    CADEAUX          ("Cadeaux"),
    FRAIS_BANCAIRES  ("Frais bancaires"),
    BAR_ET_RESTAURANT("Bar & restaurant"),
    DIVERS           ("Divers")          // toujours en dernier - fallback de safeValueOf
}

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
    val recurrenceDay: Int? = null,                 // Jour du mois (1-28) — utilisé pour MONTHLY uniquement
    val sourceRecurringId: Long? = null,            // ID du modèle qui a généré cette occurrence
    val subCategory: SubCategory? = null,           // Non-null uniquement si category == AUTRE (enum fixe)
    val customSubCategoryId: Long? = null,          // Référence à une CustomSubCategory créée par l'utilisateur
    val recurrenceFrequency: RecurrenceFrequency? = null, // null si non-récurrent ou MONTHLY (rétrocompat)
    val firstPaymentDate: LocalDate? = null,        // Date de la première occurrence (détermine le jour pour WEEKLY/YEARLY)
    val endDate: LocalDate? = null                  // Date de fin de récurrence (null = indéfini)
)

enum class TransactionType { EXPENSE, INCOME, INVESTMENT }

enum class Currency(val symbol: String, val isoCode: String) {
    EUR("€", "EUR"),
    USD("$", "USD"),
    XOF("FCFA", "XOF"),
    XAF("FCFA", "XAF")
}

enum class Category(val displayName: String) {
    ALIMENTATION  ("Alimentation"),
    LOGEMENT      ("Logement"),
    TRANSPORT     ("Transport"),
    SANTE         ("Santé"),
    LOISIRS       ("Loisirs"),         // inclut les vacances
    ABONNEMENTS   ("Abonnements"),     // téléphonie, streaming, internet, logiciels
    INVESTISSEMENT("Investissement"),
    EPARGNE       ("Épargne"),
    ENFANT        ("Enfant"),
    HABILLEMENT   ("Habillement"),
    IMPOTS_CHARGES("Impôts & charges"),
    ASSURANCES    ("Assurances"),
    TRANSFERTS    ("Transferts"),
    AUTRE         ("Autre")            // toujours en dernier — fallback de safeValueOf
}

// Sous-catégories prédéfinies, utilisées uniquement quand category == AUTRE
enum class SubCategory(val displayName: String) {
    CADEAUX        ("Cadeaux"),
    FRAIS_BANCAIRES("Frais bancaires"),
    DIVERS         ("Divers")          // toujours en dernier — fallback de safeValueOf
}

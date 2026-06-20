package com.dibitara.app.domain.model

import java.time.LocalDate

/**
 * Type de métal précieux - détermine l'unité d'affichage dans l'interface.
 * La valeur de chaque métal est calculée : quantité (g) × prix unitaire (centimes/g).
 */
enum class MetalType(val displayName: String) {
    OR      ("Or"),
    ARGENT  ("Argent"),
    PLATINE ("Platine")
}

/**
 * Type de plan d'épargne salariale.
 */
enum class EmployeeSavingsType(val displayName: String) {
    PEE  ("Plan d'Épargne Entreprise"),
    PERCO("Plan d'Épargne Retraite Collectif")
}

/**
 * Actif en métaux précieux.
 * [totalValueCents] = quantityGrams × pricePerGramCents (calculé, non stocké).
 */
data class PreciousMetalAsset(
    val id                : Long = 0,
    val metalType         : MetalType,
    val label             : String,
    val quantityGrams     : Double,
    val pricePerGramCents : Long,
    val currency          : Currency,
    val updatedAt         : LocalDate
) {
    val totalValueCents: Long get() = (quantityGrams * pricePerGramCents).toLong()
}

/**
 * Actif à libellé libre - cryptos, actions hors PEA, œuvres d'art, etc.
 * On stocke directement la valeur totale, mise à jour manuellement.
 */
data class CustomAsset(
    val id              : Long = 0,
    val label           : String,
    val totalValueCents : Long,
    val currency        : Currency,
    val updatedAt       : LocalDate
)

/**
 * Épargne salariale (PEE ou PERCO).
 * [employerContributionCents] = montant de l'abondement employeur mensuel.
 */
data class EmployeeSavings(
    val id                        : Long = 0,
    val type                      : EmployeeSavingsType,
    val label                     : String,
    val currentBalanceCents       : Long,
    val employerContributionCents : Long,
    val currency                  : Currency,
    val updatedAt                 : LocalDate
)

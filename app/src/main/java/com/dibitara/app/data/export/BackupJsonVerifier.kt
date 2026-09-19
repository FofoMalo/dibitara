package com.dibitara.app.data.export

import com.google.gson.JsonParser

/** Validation structurelle avant toute écriture. Un objet JSON quelconque n'est pas une sauvegarde. */
object BackupJsonVerifier {
    private val collections = listOf("enfants", "transactions", "budgets", "epargne", "immobilier", "scpi",
        "airbnb", "vehicule_locatif", "dettes", "actifs_libres", "epargne_salariale", "sous_categories_perso",
        "comptes_bancaires", "enveloppes_budget", "regles_categorisation", "versements_mensuels", "objectifs_epargne")

    fun verifier(json: String): Int = inspecter(json, true).second

    fun inspecter(json: String, exigerToutesCollections: Boolean = false): Pair<String, Int> {
        val root = JsonParser.parseString(json).asJsonObject
        require(root.get("version")?.asString?.isNotBlank() == true) { "Version de sauvegarde absente" }
        val date = requireNotNull(root.get("exportDate")?.asString) { "Date de sauvegarde absente" }
        java.time.LocalDate.parse(date)
        val format = root.get("backupFormat")?.asInt ?: 1
        require(format in 1..4) { "Format de sauvegarde plus récent que cette application" }
        val required = if (exigerToutesCollections || format >= 2) collections else listOf("transactions", "budgets", "enfants")
        val extra = if (format >= 2) listOf("patrimoine_snapshots", "asset_valuation_snapshots", "transaction_trash") else emptyList()
        val categoryCollections = (if (format >= 3) listOf("category_definitions") else emptyList()) +
            (if (format >= 4) listOf("etf_plans", "etf_purchases") else emptyList())
        (required + extra + categoryCollections).forEach { require(root.get(it)?.isJsonArray == true) { "Sauvegarde incomplète : $it" } }
        (collections + extra + categoryCollections).filter { root.has(it) }.forEach { require(root.get(it).isJsonArray) { "Collection invalide : $it" } }
        if (format >= 2) require(root.get("preferences")?.isJsonObject == true) { "Préférences absentes" }
        val count = (collections + extra + categoryCollections).sumOf { root.get(it)?.asJsonArray?.size() ?: 0 }
        val labels = mapOf("enfants" to "Enfants", "transactions" to "Transactions", "budgets" to "Budgets", "epargne" to "Comptes d’épargne",
            "immobilier" to "Biens immobiliers", "scpi" to "SCPI", "airbnb" to "Locations", "vehicule_locatif" to "Locations de véhicule",
            "dettes" to "Dettes", "actifs_libres" to "Actifs libres", "epargne_salariale" to "Épargne salariale",
            "sous_categories_perso" to "Sous-catégories", "comptes_bancaires" to "Comptes bancaires", "enveloppes_budget" to "Enveloppes",
            "regles_categorisation" to "Règles de classement", "versements_mensuels" to "Versements", "objectifs_epargne" to "Objectifs",
            "patrimoine_snapshots" to "Historique du patrimoine", "asset_valuation_snapshots" to "Historique des valorisations", "transaction_trash" to "Corbeille", "category_definitions" to "Catalogue des catégories", "etf_plans" to "Suivis ETF", "etf_purchases" to "Achats ETF")
        val detail = (collections + extra + categoryCollections).filter { root.has(it) }.joinToString("\n") { "${labels[it] ?: it} : ${root.getAsJsonArray(it).size()}" }
        return "Sauvegarde du $date · ${root.get("version").asString}\n$count éléments\n\n$detail\n\n".plus(
            if (format >= 2) "Historiques et réglages inclus. Les accès de sécurité restent ceux de cet appareil."
            else "Ancien format : les historiques et réglages absents ne peuvent pas être récupérés depuis ce fichier."
        ) to count
    }
}

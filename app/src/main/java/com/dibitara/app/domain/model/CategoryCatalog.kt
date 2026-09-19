package com.dibitara.app.domain.model

/** Une seule présentation pour les catégories et les deux anciens types de sous-catégories. */
data class CategoryNode(
    val key: String,
    val name: String,
    val parentKey: String? = null,
    val archived: Boolean = false,
    val icon: String = "DEFAULT",
    val color: String = "DEFAULT"
)
data class CategoryChoice(val category: Category, val subCategory: SubCategory? = null, val customSubCategoryId: Long? = null) {
    val key: String get() = customSubCategoryId?.let { "u:$it" } ?: subCategory?.let { "s:${it.name}" } ?: "c:${category.name}"
    fun matches(t: Transaction): Boolean = if (customSubCategoryId != null) t.customSubCategoryId == customSubCategoryId
        else if (subCategory != null) t.subCategory == subCategory && t.customSubCategoryId == null
        else t.category == category
}
data class CategoryCatalog(val nodes: List<CategoryNode> = defaults()) {
    fun node(key: String) = nodes.firstOrNull { it.key == key }
    fun category(value: Category): Category = node("c:${value.name}")?.let { Category(value.name, it.name, it.icon, it.color) } ?: value
    fun choice(key: String): CategoryChoice {
        val n = requireNotNull(node(key)) { "Catégorie introuvable" }
        val parent = n.parentKey?.let { requireNotNull(node(it)) } ?: n
        val cat = category(Category.valueOf(parent.key.removePrefix("c:")))
        return when {
            key.startsWith("u:") -> CategoryChoice(cat, customSubCategoryId = key.removePrefix("u:").toLong())
            key.startsWith("s:") -> CategoryChoice(cat, subCategory = SubCategory.valueOf(key.removePrefix("s:")))
            else -> CategoryChoice(cat)
        }
    }
    fun label(choice: CategoryChoice): String {
        val n = node(choice.key) ?: return choice.category.displayName
        return n.parentKey?.let { "${node(it)?.name ?: choice.category.displayName} › ${n.name}" } ?: n.name
    }
    fun selectable(n: CategoryNode) = !n.archived && (n.parentKey == null || node(n.parentKey)?.archived == false)
    companion object {
        fun defaults() = Category.entries.map { CategoryNode("c:${it.name}", it.displayName) } +
            SubCategory.entries.map { CategoryNode("s:${it.name}", it.displayName, "c:AUTRE") }
    }
}
data class CategoryImpact(val operations: Int, val rules: Int, val envelopes: Int, val trash: Int, val subcategories: Int) {
    val isUnused get() = operations + rules + envelopes + trash + subcategories == 0
    fun description() = "$operations opérations, $rules règles, $envelopes enveloppes, $trash opérations en corbeille et $subcategories sous-catégories."
}

/** Vérifier les liens avant une restauration, jamais après le remplacement de la base. */
fun CategoryCatalog.validate() {
    require(nodes.map { it.key }.distinct().size == nodes.size) { "Identifiants de catégories dupliqués" }
    nodes.forEach { n ->
        require(n.name.isNotBlank()) { "Nom de catégorie invalide" }
        if(n.parentKey == null) {
            require(n.key.startsWith("c:")) { "Identifiant de catégorie invalide" }
            Category.valueOf(n.key.removePrefix("c:"))
        } else {
            require(node(n.parentKey)?.let { it.parentKey==null } == true) { "Parent de sous-catégorie absent" }
            when {
                n.key.startsWith("u:") -> require((n.key.removePrefix("u:").toLongOrNull() ?: 0)>0) { "Identifiant de sous-catégorie invalide" }
                n.key.startsWith("s:") -> SubCategory.valueOf(n.key.removePrefix("s:"))
                else -> error("Identifiant de sous-catégorie invalide")
            }
        }
        require(n.icon in setOf("DEFAULT","SHOP","HOME","CAR","HEART","GIFT","STAR","BOOK")) { "Icône inconnue" }
        require(n.color in setOf("DEFAULT","GOLD","GREEN","BLUE","PURPLE","TEAL","ROSE")) { "Couleur inconnue" }
    }
}

/** Recherche tolérante aux accents, à la casse et à l’ordre des mots. */
fun String.matchesCategorySearch(query: String): Boolean {
    fun normalize(value: String) = java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase()
    val text=normalize(this)
    return normalize(query).trim().split(Regex("\\s+")).all { it in text }
}

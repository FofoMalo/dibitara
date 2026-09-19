package com.dibitara.app.data.repository

import androidx.room.withTransaction
import com.dibitara.app.data.local.database.DibitaraDatabase
import com.dibitara.app.data.local.entity.*
import com.dibitara.app.domain.model.*
import com.dibitara.app.domain.repository.CategoryCatalogRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class CategoryCatalogRepositoryImpl @Inject constructor(private val db: DibitaraDatabase) : CategoryCatalogRepository {
    private val gson = Gson()
    override fun observe(): Flow<CategoryCatalog> = combine(db.categoryDefinitionDao().observe(), db.customSubCategoryDao().getAll()) { definitions, subs ->
        val nodes = (CategoryCatalog.defaults() + subs.map { CategoryNode("u:${it.id}", it.name, "c:${it.parentCategory}") }).associateBy { it.key }.toMutableMap()
        definitions.forEach { nodes[it.key] = it.toDomain() }
        CategoryCatalog(nodes.values.toList())
    }
    private suspend fun current() = observe().first()
    private suspend fun store(n: CategoryNode) = db.categoryDefinitionDao().save(CategoryDefinitionEntity.fromDomain(n))
    override suspend fun save(node: CategoryNode) = db.withTransaction {
        val catalog = current()
        val old = requireNotNull(catalog.node(node.key)) { "Catégorie introuvable" }
        require(old.parentKey == node.parentKey) { "Utilisez Déplacer pour changer le parent." }
        val name = node.name.trim()
        require(name.isNotBlank() && name.length <= 60) { "Le nom doit contenir entre 1 et 60 caractères." }
        require(catalog.nodes.none { it.key != node.key && it.parentKey == node.parentKey && it.name.equals(name, true) }) { "Ce nom existe déjà dans ce groupe." }
        require(node.icon in ICONS && node.color in COLORS) { "Présentation invalide" }
        store(node.copy(name = name))
        if (node.key.startsWith("u:")) {
            val sub = db.customSubCategoryDao().getAll().first().first { "u:${it.id}" == node.key }
            db.customSubCategoryDao().upsert(sub.copy(name = name))
        }
    }
    override suspend fun create(name: String, parentKey: String?): String = db.withTransaction {
        val c = current(); val trimmed = name.trim()
        require(trimmed.isNotBlank() && trimmed.length <= 60) { "Le nom doit contenir entre 1 et 60 caractères." }
        require(c.nodes.none { it.parentKey == parentKey && it.name.equals(trimmed,true) }) { "Ce nom existe déjà dans ce groupe." }
        val key = if (parentKey == null) "c:USER_${java.util.UUID.randomUUID()}" else {
            val parent = requireNotNull(c.node(parentKey))
            require(parent.parentKey == null && !parent.archived) { "Choisissez une catégorie active." }
            val id = db.customSubCategoryDao().insertNew(CustomSubCategoryEntity(name = trimmed, parentCategory = parentKey.removePrefix("c:")))
            "u:$id"
        }
        store(CategoryNode(key, trimmed, parentKey)); key
    }
    override suspend fun impact(key: String): CategoryImpact = db.withTransaction {
        val c = current(); val choice = c.choice(key)
        val rows = db.transactionDao().getAll().first()
        val rules = db.categorizationRuleDao().getAll()
        val trash = db.transactionTrashDao().getAll().first()
        CategoryImpact(rows.count { choice.matches(it.toDomain()) }, rules.count { matches(key,it.category,it.subCategory,it.customSubCategoryId) },
            db.categoryEnvelopeDao().getAll().first().count { key == "c:${it.category}" },
            trash.count { choice.matches(gson.fromJson(it.payload,TransactionEntity::class.java).toDomain()) }, c.nodes.count { it.parentKey == key })
    }
    override suspend fun deleteUnused(key: String) = db.withTransaction {
        require(key.startsWith("u:") || key.startsWith("c:USER_")) { "Archivez les catégories prédéfinies." }
        require(impact(key).isUnused) { "Cette catégorie est utilisée. Archivez-la pour conserver l’historique." }
        if (key.startsWith("u:")) db.customSubCategoryDao().getAll().first().firstOrNull { "u:${it.id}" == key }?.let { db.customSubCategoryDao().delete(it) }
        db.categoryDefinitionDao().delete(key)
    }
    private fun matches(key: String, category: String, sub: String?, custom: Long?) = when {
        key.startsWith("c:") -> key == "c:$category"
        key.startsWith("u:") -> key == "u:$custom"
        else -> custom == null && key == "s:$sub"
    }
    override suspend fun move(source: String, parent: String, expected: CategoryImpact) = db.withTransaction {
        val c = current(); val n = requireNotNull(c.node(source)); val p = requireNotNull(c.node(parent))
        require(n.parentKey != null && p.parentKey == null && !p.archived && n.parentKey != parent) { "Déplacement invalide" }
        require(c.nodes.none { it.key != source && it.parentKey == parent && it.name.equals(n.name,true) }) { "Une sous-catégorie porte déjà ce nom. Utilisez Fusionner." }
        require(impact(source) == expected) { "Les données ont changé. Consultez un nouvel aperçu." }
        val old = c.choice(source); val target = old.copy(category = c.category(Category.valueOf(parent.removePrefix("c:"))))
        rewrite(source, target, false)
        store(n.copy(parentKey = parent))
        if (source.startsWith("u:")) db.customSubCategoryDao().getAll().first().first { "u:${it.id}" == source }.let { db.customSubCategoryDao().upsert(it.copy(parentCategory = target.category.name)) }
    }
    override suspend fun merge(source: String, target: String, expected: CategoryImpact) = db.withTransaction {
        val c = current(); val a = requireNotNull(c.node(source)); val b = requireNotNull(c.node(target))
        require(source != target && !a.archived && c.selectable(b)) { "Fusion invalide" }
        require((a.parentKey == null) == (b.parentKey == null)) { "Fusionnez deux catégories ou deux sous-catégories." }
        require(source !in PROTECTED && target !in PROTECTED) { "Les catégories de transfert, d’épargne et d’investissement conservent leur rôle financier." }
        require(impact(source) == expected) { "Les données ont changé. Consultez un nouvel aperçu." }
        if (a.parentKey == null) {
            require(c.nodes.none { it.parentKey == source && c.nodes.any { other -> other.parentKey == target && other.name.equals(it.name,true) } }) { "Des sous-catégories portent le même nom. Fusionnez-les d’abord." }
            val envelopes = db.categoryEnvelopeDao().getAll().first()
            require(!(envelopes.any { "c:${it.category}" == source } && envelopes.any { "c:${it.category}" == target })) { "Les deux catégories ont une enveloppe. Ajustez les enveloppes avant de fusionner." }
        }
        val choice = c.choice(target)
        rewrite(source, choice, a.parentKey == null)
        if (a.parentKey == null) {
            c.nodes.filter { it.parentKey == source }.forEach { store(it.copy(parentKey = target)) }
            db.customSubCategoryDao().getAll().first().filter { "c:${it.parentCategory}" == source }.forEach { db.customSubCategoryDao().upsert(it.copy(parentCategory = choice.category.name)) }
            db.categoryEnvelopeDao().getAll().first().filter { "c:${it.category}" == source }.forEach { db.categoryEnvelopeDao().upsert(it.copy(category = choice.category.name)) }
        }
        // L’entrée source reste archivée : aucun identifiant historique n’est réutilisé.
        store(a.copy(archived = true))
    }
    private suspend fun rewrite(source: String, target: CategoryChoice, keepChildren: Boolean) {
        fun changed(t: TransactionEntity) = t.copy(category = target.category.name,
            subCategory = if (keepChildren) t.subCategory else target.subCategory?.name,
            customSubCategoryId = if (keepChildren) t.customSubCategoryId else target.customSubCategoryId)
        db.transactionDao().getAll().first().filter { matches(source,it.category,it.subCategory,it.customSubCategoryId) }.forEach { db.transactionDao().update(changed(it)) }
        db.categorizationRuleDao().getAll().filter { matches(source,it.category,it.subCategory,it.customSubCategoryId) }.forEach {
            db.categorizationRuleDao().upsert(it.copy(category=target.category.name, subCategory=if(keepChildren) it.subCategory else target.subCategory?.name, customSubCategoryId=if(keepChildren) it.customSubCategoryId else target.customSubCategoryId))
        }
        db.transactionTrashDao().getAll().first().forEach { entry ->
            val t=gson.fromJson(entry.payload,TransactionEntity::class.java)
            if(matches(source,t.category,t.subCategory,t.customSubCategoryId)) db.transactionTrashDao().update(entry.copy(payload=gson.toJson(changed(t))))
        }
    }
    override suspend fun classify(ids: Set<Long>, choice: CategoryChoice): Int = db.withTransaction {
        val c=current(); val node=requireNotNull(c.node(choice.key)); require(c.selectable(node)) { "Cette catégorie est archivée." }
        val resolved=c.choice(choice.key); require(resolved==choice) { "La catégorie a changé. Sélectionnez-la à nouveau." }
        val entries=ids.map { requireNotNull(db.transactionDao().getById(it)) { "Une opération n’existe plus." } }
        require(entries.all { it.type==TransactionType.EXPENSE.name }) { "Sélectionnez uniquement des dépenses." }
        entries.forEach { db.transactionDao().update(it.copy(category=choice.category.name,subCategory=choice.subCategory?.name,customSubCategoryId=choice.customSubCategoryId,categoryConfirmed=true)) }
        entries.size
    }
    override suspend fun rememberRule(note: String, choice: CategoryChoice) = db.withTransaction {
        require(note.isNotBlank()) { "Un libellé est nécessaire pour créer une règle." }
        val c=current(); require(c.node(choice.key)?.let { c.selectable(it) } == true) { "Catégorie indisponible" }
        db.categorizationRuleDao().upsert(CategorizationRuleEntity(noteExact=note.trim().lowercase(),category=choice.category.name,subCategory=choice.subCategory?.name,customSubCategoryId=choice.customSubCategoryId))
    }
    override suspend fun matching(note: String, choice: CategoryChoice) = db.transactionDao().getAll().first().map { it.toDomain() }.filter {
        it.type==TransactionType.EXPENSE && it.note.trim().equals(note.trim(),true) &&
            (it.category!=choice.category || it.subCategory!=choice.subCategory || it.customSubCategoryId!=choice.customSubCategoryId)
    }
    companion object {
        val PROTECTED = setOf("c:TRANSFERTS","c:EPARGNE","c:INVESTISSEMENT")
        val ICONS = setOf("DEFAULT","SHOP","HOME","CAR","HEART","GIFT","STAR","BOOK")
        val COLORS = setOf("DEFAULT","GOLD","GREEN","BLUE","PURPLE","TEAL","ROSE")
    }
}

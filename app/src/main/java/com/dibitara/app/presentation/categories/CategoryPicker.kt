package com.dibitara.app.presentation.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.*
import com.dibitara.app.presentation.common.chartColor
import com.dibitara.app.presentation.common.chartIcon

/** Le même choix est utilisé en saisie, filtre, classement multiple et gestion. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPicker(
    onDismiss: () -> Unit,
    onChoose: (CategoryChoice) -> Unit,
    allowSubcategories: Boolean = true,
    includeArchived: Boolean = false,
    suggestion: CategoryChoice? = null,
    allowManage: Boolean = true,
    title: String = "Choisir une catégorie",
    viewModel: CategoryCatalogViewModel = hiltViewModel()
) {
    val catalog by viewModel.catalog.collectAsState()
    val recent by viewModel.recent.collectAsState()
    var query by remember { mutableStateOf("") }
    var parent by remember { mutableStateOf<String?>(null) }
    var manage by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest=onDismiss, sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        if(manage) CategoryManagementScreen(onBack={manage=false},viewModel=viewModel) else {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal=16.dp)) {
                Text(title,style=MaterialTheme.typography.titleLarge)
                OutlinedTextField(query,{query=it},label={Text("Rechercher une catégorie")},singleLine=true,modifier=Modifier.fillMaxWidth().padding(vertical=12.dp))
                if(parent!=null && query.isBlank()) TextButton(onClick={parent=null}) { Icon(Icons.AutoMirrored.Filled.ArrowBack,null); Text("Toutes les catégories") }
                val available = catalog.nodes.filter { (includeArchived || catalog.selectable(it)) && (allowSubcategories || it.parentKey==null) }
                val shown = if(query.isNotBlank()) available.filter { catalog.label(catalog.choice(it.key)).matchesCategorySearch(query) }
                    else available.filter { it.parentKey==parent }
                LazyColumn(Modifier.weight(1f)) {
                    if(query.isBlank() && parent==null) {
                        suggestion?.let { s -> catalog.node(s.key)?.takeIf { it in available }?.let { n ->
                            item { Text("Suggestion",style=MaterialTheme.typography.labelLarge); CategoryChoiceRow(n,catalog,true,{onChoose(catalog.choice(n.key))}) }
                        } }
                        val last=recent.mapNotNull { key -> available.firstOrNull { it.key==key } }
                        if(last.isNotEmpty()) {
                            item { Text("Récemment utilisées",style=MaterialTheme.typography.labelLarge,modifier=Modifier.padding(top=8.dp)) }
                            items(last,key={"recent:${it.key}"}) { n -> CategoryChoiceRow(n,catalog,true,{onChoose(catalog.choice(n.key))}) }
                        }
                        item { Text("Toutes les catégories",style=MaterialTheme.typography.labelLarge,modifier=Modifier.padding(top=12.dp)) }
                    }
                    if(parent!=null && query.isBlank()) {
                        catalog.node(parent!!)?.let { n -> item { TextButton(onClick={onChoose(catalog.choice(n.key))}) { Text("Choisir ${n.name} sans précision") } } }
                    }
                    items(shown.sortedBy { it.name.lowercase() },key={it.key}) { n ->
                        CategoryChoiceRow(n,catalog,query.isNotBlank(),{onChoose(catalog.choice(n.key))},
                            if(allowSubcategories && n.parentKey==null && available.any { it.parentKey==n.key }) {{parent=n.key}} else null)
                    }
                    if(shown.isEmpty()) item { Text("Aucun résultat",modifier=Modifier.padding(16.dp)) }
                }
                if(allowManage) TextButton(onClick={manage=true},modifier=Modifier.fillMaxWidth()) { Text("Gérer les catégories") }
            }
        }
    }
}

@Composable
internal fun CategoryChoiceRow(n: CategoryNode,c: CategoryCatalog,path: Boolean,onClick: () -> Unit,onExplore: (() -> Unit)? = null) {
    val category = c.choice(n.key).category
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).heightIn(min=56.dp).padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(category.chartIcon(),null,tint=category.chartColor(),modifier=Modifier.padding(end=12.dp).size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(if(path)c.label(c.choice(n.key)) else n.name)
            if(n.parentKey==null) {
                val children=c.nodes.filter { it.parentKey==n.key && !it.archived }.take(3).joinToString(" · "){it.name}
                if(children.isNotEmpty())Text(children,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(n.archived)Text("Archivée",style=MaterialTheme.typography.labelSmall)
        }
        if(onExplore!=null) IconButton(onClick=onExplore) { Icon(Icons.Filled.ChevronRight,"Sous-catégories de ${n.name}") }
    }
    HorizontalDivider()
}

package com.dibitara.app.presentation.categories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryManagementScreen(onBack: () -> Unit,viewModel: CategoryCatalogViewModel=hiltViewModel()) {
    val catalog by viewModel.catalog.collectAsState()
    val error by viewModel.error.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val preview by viewModel.preview.collectAsState()
    var parent by remember { mutableStateOf<String?>(null) }
    var archived by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<CategoryNode?>(null) }
    var creating by remember { mutableStateOf(false) }
    var choosingTarget by remember { mutableStateOf<String?>(null) }
    var moving by remember { mutableStateOf(false) }
    val back = { if(parent!=null)parent=null else onBack() }
    BackHandler(onBack=back)
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick=back) { Text("Retour") }
        Text(parent?.let { catalog.node(it)?.name } ?: "Catégories",style=MaterialTheme.typography.headlineSmall)
        Text("Organisez vos dépenses à votre façon.",style=MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={creating=true;viewModel.error.value=null},enabled=!busy) { Text(if(parent==null)"+ Catégorie" else "+ Sous-catégorie") }
            FilterChip(selected=archived,onClick={archived=!archived},label={Text("Archivées")})
            if(parent!=null) TextButton(onClick={editing=catalog.node(parent!!);viewModel.error.value=null}) { Text("Modifier cette catégorie") }
        }
        OutlinedTextField(query,{query=it},label={Text("Rechercher")},modifier=Modifier.fillMaxWidth())
        error?.let { Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(vertical=8.dp)) }
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        val shown=catalog.nodes.filter { it.archived==archived && (if(query.isBlank())it.parentKey==parent else catalog.label(catalog.choice(it.key)).matchesCategorySearch(query)) }
        LazyColumn(Modifier.weight(1f)) {
            items(shown.sortedBy { it.name.lowercase() },key={it.key}) { n ->
                CategoryChoiceRow(n,catalog,query.isNotBlank(),{
                    if(n.parentKey==null){parent=n.key;query="";archived=false}else{editing=n;viewModel.error.value=null}
                },if(n.parentKey==null){{parent=n.key;query="";archived=false}}else null)
            }
            if(shown.isEmpty())item { Text("Aucune catégorie ici",modifier=Modifier.padding(16.dp)) }
        }
    }
    val node=editing
    if(node!=null || creating) {
        var name by remember(node?.key,creating) { mutableStateOf(node?.name ?: "") }
        var icon by remember(node?.key) { mutableStateOf(node?.icon ?: "DEFAULT") }
        var color by remember(node?.key) { mutableStateOf(node?.color ?: "DEFAULT") }
        AlertDialog(onDismissRequest={if(!busy){editing=null;creating=false}},title={Text(if(creating)"Créer" else "Modifier ${node?.name}")},text={
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name,{name=it},label={Text("Nom")},modifier=Modifier.fillMaxWidth())
                if(node?.parentKey==null && !creating) {
                    Text("Icône",modifier=Modifier.padding(top=12.dp))
                    FlowRow { listOf("DEFAULT" to "Par défaut","SHOP" to "Courses","HOME" to "Maison","CAR" to "Voiture","HEART" to "Santé","GIFT" to "Cadeau","STAR" to "Étoile","BOOK" to "Études").forEach { (id,label) -> FilterChip(selected=icon==id,onClick={icon=id},label={Text(label)}) } }
                    Text("Couleur")
                    FlowRow { listOf("DEFAULT" to "Par défaut","GOLD" to "Or","GREEN" to "Vert","BLUE" to "Bleu","PURPLE" to "Violet","TEAL" to "Turquoise","ROSE" to "Rose").forEach { (id,label) -> FilterChip(selected=color==id,onClick={color=id},label={Text(label)}) } }
                }
                if(node!=null) {
                    TextButton(onClick={viewModel.save(node.copy(archived=!node.archived)){editing=null}},enabled=!busy) { Text(if(node.archived)"Réactiver" else "Archiver") }
                    if(!node.archived) {
                        TextButton(onClick={choosingTarget=node.key;moving=false;editing=null}) { Text("Fusionner avec…") }
                        if(node.parentKey!=null)TextButton(onClick={choosingTarget=node.key;moving=true;editing=null}) { Text("Déplacer vers…") }
                    }
                    if(node.key.startsWith("u:") || node.key.startsWith("c:USER_"))TextButton(onClick={viewModel.delete(node.key){editing=null}},enabled=!busy) { Text("Supprimer si inutilisée") }
                    Text("L’archivage conserve l’historique. Une catégorie utilisée ne peut pas être supprimée.",style=MaterialTheme.typography.bodySmall)
                }
                error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            }
        },confirmButton={TextButton(onClick={if(creating)viewModel.create(name,parent){creating=false}else viewModel.save(node!!.copy(name=name,icon=icon,color=color)){editing=null}},enabled=!busy && name.isNotBlank()){Text("Enregistrer")}},dismissButton={TextButton(onClick={editing=null;creating=false},enabled=!busy){Text("Annuler")}})
    }
    choosingTarget?.let { source ->
        val sourceNode=catalog.node(source)
        val targets=catalog.nodes.filter { it.key!=source && catalog.selectable(it) && if(moving)it.parentKey==null && it.key!=sourceNode?.parentKey else (it.parentKey==null)==(sourceNode?.parentKey==null) }
        AlertDialog(onDismissRequest={choosingTarget=null},title={Text(if(moving)"Déplacer vers" else "Fusionner avec")},text={
            LazyColumn { items(targets,key={it.key}) { n -> TextButton(onClick={viewModel.preview(source,n.key,moving);choosingTarget=null}) { Text(catalog.label(catalog.choice(n.key))) } } }
        },confirmButton={},dismissButton={TextButton(onClick={choosingTarget=null}){Text("Annuler")}})
    }
    preview?.let { p -> AlertDialog(onDismissRequest={if(!busy)viewModel.preview.value=null},title={Text("Vérifier les changements")},text={Column {
        Text("${catalog.node(p.source)?.name} → ${catalog.node(p.target)?.name}")
        Text(p.impact.description(),modifier=Modifier.padding(vertical=12.dp))
        Text(if(p.move)"Le parent change aussi dans les anciennes opérations et les règles." else "L’historique sera regroupé sous la destination. La source sera archivée. Les enveloppes en conflit doivent être ajustées avant la fusion.")
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
    }},confirmButton={TextButton(onClick={viewModel.confirm{parent=null}},enabled=!busy){Text("Confirmer")}},dismissButton={TextButton(onClick={viewModel.preview.value=null},enabled=!busy){Text("Annuler")}}) }
}

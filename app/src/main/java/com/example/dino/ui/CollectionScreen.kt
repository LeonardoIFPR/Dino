package com.example.dino.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dino.data.Catalog
import com.example.dino.data.ExpeditionStore
import com.example.dino.data.Fossil
import com.example.dino.ui.theme.*

internal val CollectionPaper = Color(0xFFFAF8F4)
private val CollectionLine = Color(0xFFE4DFD6)
private val CollectionMuted = Color(0xFF847D72)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollectionScreen(
    store: ExpeditionStore,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onLab: () -> Unit
) {
    // Tab order: owned specimens, the full catalog, completed reconstructions.
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val owned = Catalog.fossils.filter { store.count(it) > 0 }
    val assembled = Catalog.fossils.filter { store.complete(it) }
    val fossils = when (filter) { 1 -> Catalog.fossils; 2 -> assembled; else -> owned }
    Column(Modifier.fillMaxSize().background(CollectionPaper)) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(start=8.dp,end=22.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack,modifier=Modifier.semantics { contentDescription="Voltar ao mapa" }) {
                Canvas(Modifier.size(22.dp)) {
                    val stroke=2.dp.toPx()
                    drawLine(Ink,Offset(size.width*.65f,size.height*.2f),Offset(size.width*.35f,size.height*.5f),stroke,StrokeCap.Round)
                    drawLine(Ink,Offset(size.width*.35f,size.height*.5f),Offset(size.width*.65f,size.height*.8f),stroke,StrokeCap.Round)
                }
            }
            Text("Coleção",Modifier.weight(1f),fontSize=23.sp,fontWeight=FontWeight.SemiBold,color=Ink,fontFamily=FontFamily.SansSerif)
            Text("${owned.size} / ${Catalog.fossils.size}",fontSize=14.sp,color=CollectionMuted,
                modifier=Modifier.semantics { contentDescription="${owned.size} de ${Catalog.fossils.size} espécies encontradas" })
        }
        PrimaryTabRow(selectedTabIndex=filter,containerColor=CollectionPaper,contentColor=Earth,
            divider={HorizontalDivider(color=CollectionLine,thickness=1.dp)}) {
            listOf("Coletados" to owned.size,"Catálogo" to Catalog.fossils.size,"Montados" to assembled.size).forEachIndexed { index,(label,count) ->
                Tab(selected=filter==index,onClick={filter=index},text={
                    Text("$label  $count",fontSize=12.sp,fontWeight=if(filter==index) FontWeight.SemiBold else FontWeight.Normal)
                },selectedContentColor=Ink,unselectedContentColor=CollectionMuted)
            }
        }
        if(fossils.isEmpty()) {
            CollectionEmpty(filter,onBack,onLab,Modifier.weight(1f))
        } else {
            LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.weight(1f),
                contentPadding=PaddingValues(start=20.dp,end=20.dp,top=22.dp,bottom=32.dp),
                horizontalArrangement=Arrangement.spacedBy(22.dp),verticalArrangement=Arrangement.spacedBy(28.dp)) {
                items(fossils,key={it.id}) { fossil -> CollectionSpecimen(fossil,store,onSelect) }
            }
        }
    }
}

@Composable
private fun CollectionSpecimen(fossil:Fossil,store:ExpeditionStore,onSelect:(String)->Unit) {
    val found=store.count(fossil)>0
    val complete=store.complete(fossil)
    val status=when {
        complete -> if(store.quantity(fossil)>1) "Montado · ${store.quantity(fossil)} achados" else "Montado"
        !found -> "Não encontrado"
        fossil.museumDisplay -> if(store.quantity(fossil)>1) "${store.quantity(fossil)} crânios coletados" else "Crânio coletado"
        else -> "${store.count(fossil)} de ${fossil.fieldParts.size} fragmentos"
    }
    Column(Modifier.fillMaxWidth().clickable { onSelect(fossil.id) }
        .semantics { contentDescription="${fossil.name}, $status. Abrir fóssil." }) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.08f),contentAlignment=Alignment.Center) {
            FossilArt(fossil.id,Modifier.fillMaxSize().padding(8.dp),
                color=if(found) Earth else Color(0xFFC8C0B5),
                partId=if(fossil.museumDisplay && !complete) "head" else null,
                colorFilter=if(found) null else ColorFilter.tint(Color(0xFFC8C0B5)))
            Text("%03d".format(Catalog.fossils.indexOf(fossil)+1),
                Modifier.align(Alignment.TopStart),fontSize=10.sp,fontFamily=FontFamily.Monospace,color=CollectionMuted)
        }
        Text(fossil.name,Modifier.padding(top=10.dp),fontSize=16.sp,fontWeight=FontWeight.SemiBold,
            color=if(found) Ink else CollectionMuted,fontFamily=FontFamily.SansSerif)
        Row(Modifier.padding(top=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            if(found) Box(Modifier.size(5.dp).background(if(complete) Earth else Amber,CircleShape))
            Text(status,fontSize=11.sp,color=if(found) Earth else CollectionMuted)
        }
    }
}

@Composable
private fun CollectionEmpty(filter:Int,onBack:()->Unit,onLab:()->Unit,modifier:Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
        FossilArt("ammonite",Modifier.size(64.dp),Color(0xFFC8C0B5))
        Text(if(filter==2) "Nenhum fóssil montado" else "Nenhum fóssil coletado",
            Modifier.padding(top=24.dp),fontSize=19.sp,fontWeight=FontWeight.SemiBold,color=Ink)
        Text(if(filter==2) "Leve seus achados ao laboratório." else "Encontre um fóssil no mapa para começar.",
            Modifier.padding(top=8.dp,bottom=24.dp),fontSize=13.sp,color=CollectionMuted)
        OutlinedButton(onClick=if(filter==2) onLab else onBack) {
            Text(if(filter==2) "Abrir laboratório" else "Explorar mapa")
        }
    }
}

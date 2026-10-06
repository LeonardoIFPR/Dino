package com.example.dino.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dino.data.Catalog
import com.example.dino.data.ExpeditionStore
import com.example.dino.ui.theme.*

@Composable
internal fun LabScreen(store:ExpeditionStore,onExplore:()->Unit,onAR:(String)->Unit,
                       initialFossilId:String=Catalog.fossils.first().id,onFind:(String)->Unit) {
    var fossilId by rememberSaveable(initialFossilId) { mutableStateOf(initialFossilId) }
    val fossil=Catalog.fossils.firstOrNull { it.id==fossilId } ?: Catalog.fossils.first()
    val owned=fossil.fieldParts.filter { store.hasPart(fossil.id,it.id) }
    val complete=store.complete(fossil)
    val ready=store.canAssemble(fossil)
    var selectedPart by rememberSaveable(fossil.id) { mutableStateOf<String?>(null) }
    var reset by remember(fossil.id) { mutableIntStateOf(0) }
    val preview=owned.firstOrNull { it.id==selectedPart } ?: owned.firstOrNull()
    val line=Color(0xFFE1DBD1)

    Column(Modifier.fillMaxSize().background(FossilStage)) {
        Row(Modifier.fillMaxWidth().height(60.dp).background(CollectionPaper).padding(horizontal=8.dp),
            verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onExplore,modifier=Modifier.semantics { contentDescription="Voltar ao mapa" }) {
                Canvas(Modifier.size(22.dp)) {
                    drawLine(Ink,Offset(size.width*.65f,size.height*.2f),Offset(size.width*.35f,size.height*.5f),2.dp.toPx(),StrokeCap.Round)
                    drawLine(Ink,Offset(size.width*.35f,size.height*.5f),Offset(size.width*.65f,size.height*.8f),2.dp.toPx(),StrokeCap.Round)
                }
            }
            Text("Laboratório",Modifier.weight(1f),fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=Ink)
            IconButton(onClick={reset++},enabled=complete || owned.isNotEmpty(),
                modifier=Modifier.semantics { contentDescription="Reenquadrar modelo 3D" }) {
                Text("↺",fontSize=29.sp,color=if(complete || owned.isNotEmpty()) Earth else Muted)
            }
        }
        LazyRow(Modifier.fillMaxWidth().background(CollectionPaper),
            contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
            items(Catalog.fossils,key={it.id}) { choice ->
                val active=choice.id==fossil.id
                Column(Modifier.width(IntrinsicSize.Min).clickable { fossilId=choice.id }.padding(top=12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(choice.name,Modifier.padding(bottom=12.dp),fontSize=13.sp,
                        fontWeight=if(active) FontWeight.SemiBold else FontWeight.Normal,color=if(active) Ink else Muted)
                    Box(Modifier.fillMaxWidth().height(2.dp).background(if(active) Amber else Color.Transparent))
                }
            }
        }
        HorizontalDivider(color=line)
        Row(Modifier.fillMaxWidth().padding(start=22.dp,end=22.dp,top=20.dp,bottom=4.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Text(fossil.name,Modifier.weight(1f),fontSize=19.sp,fontWeight=FontWeight.SemiBold,color=Ink)
            Text(if(complete) "✓ Montado" else "${owned.size} / ${fossil.fieldParts.size} peças",
                fontSize=12.sp,color=Earth)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if(complete || preview!=null) {
                FossilViewer(fossil,Modifier.fillMaxSize(),
                    previewPart=if(complete && fossil.museumDisplay) "body" else if(complete) null else preview?.id,
                    visibleParts=if(complete || fossil.museumDisplay) null else setOfNotNull(preview?.id),
                    immersive=true,resetKey=reset,stageColor=FossilStage)
                Text(if(complete && fossil.museumDisplay) "Esqueleto completo" else if(complete) "Fóssil completo" else preview?.name.orEmpty(),
                    Modifier.align(Alignment.TopStart).padding(start=22.dp,top=4.dp),fontSize=12.sp,color=Muted)
                Text("Arraste para girar · use dois dedos para aproximar",
                    Modifier.align(Alignment.BottomCenter).padding(16.dp),fontSize=10.sp,color=Muted)
            } else {
                Column(Modifier.align(Alignment.Center).padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    FossilArt(fossil.id,Modifier.size(112.dp),color=Color(0xFFC8C0B5),
                        partId=if(fossil.museumDisplay) "head" else null,colorFilter=ColorFilter.tint(Color(0xFFC8C0B5)))
                    Text("Nenhuma peça coletada",Modifier.padding(top=20.dp),fontSize=16.sp,fontWeight=FontWeight.Medium,color=Ink)
                    Text("Seus achados vão aparecer aqui.",Modifier.padding(top=6.dp),fontSize=12.sp,color=Muted)
                }
            }
        }
        Surface(shape=RoundedCornerShape(topStart=22.dp,topEnd=22.dp),color=CollectionPaper,shadowElevation=4.dp) {
            Column(Modifier.fillMaxWidth().padding(top=18.dp,bottom=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal=22.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("Suas peças",Modifier.weight(1f),fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                    Text("${owned.size} de ${fossil.fieldParts.size} coletadas",fontSize=11.sp,color=Muted)
                }
                LazyRow(contentPadding=PaddingValues(horizontal=22.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    items(fossil.fieldParts,key={it.id}) { part ->
                        val found=part in owned
                        val active=!complete && preview?.id==part.id
                        Column(Modifier.width(if(fossil.fieldParts.size==1) 160.dp else 112.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if(active) Color(0xFFEEE7DC) else Color(0xFFF3F0EA))
                            .clickable(enabled=found && !complete) { selectedPart=part.id }
                            .semantics { contentDescription="${part.name}, ${if(found) "coletada" else "não encontrada"}" }
                            .padding(12.dp)) {
                            if(fossil.museumDisplay) Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                FossilArt(fossil.id,Modifier.size(44.dp),partId=part.id,
                                    colorFilter=if(found) null else ColorFilter.tint(Color(0xFFC8C0B5)))
                                Text(part.name,fontSize=13.sp,fontWeight=FontWeight.Medium,color=if(found) Ink else Muted)
                            } else Text(part.name,fontSize=12.sp,fontWeight=FontWeight.Medium,color=if(found) Ink else Muted)
                            Text(if(found) "✓ Coletados: ${store.quantity(fossil.id,part.id)}" else "Não encontrada",Modifier.padding(top=6.dp),fontSize=10.sp,color=if(found) Earth else Muted)
                        }
                    }
                }
                Column(Modifier.padding(horizontal=22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(when {
                        complete -> "Montagem salva na sua coleção."
                        ready && fossil.museumDisplay -> "Crânio recuperado. Você já pode montar o esqueleto."
                        ready -> "Todas as peças estão aqui. Pronto para montar."
                        fossil.museumDisplay -> "Encontre e limpe o crânio para montar o esqueleto."
                        else -> "Encontre as ${fossil.fieldParts.size-owned.size} peças restantes no mapa."
                    },fontSize=12.sp,lineHeight=18.sp,color=Muted)
                    Button(onClick={if(complete) onAR(fossil.id) else store.assembleFossil(fossil.id)},
                        enabled=complete || ready,modifier=Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(12.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=Earth,contentColor=CollectionPaper)) {
                        Text(if(complete) "Ver no meu ambiente · RA" else if(fossil.museumDisplay) "Montar esqueleto" else "Montar fóssil",fontSize=14.sp)
                    }
                    if(!ready && !complete) TextButton(onClick={onFind(fossil.id)},modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(0.dp)) {
                        Text("Encontrar peças no mapa",fontSize=12.sp,color=Earth)
                    }
                }
            }
        }
    }
}

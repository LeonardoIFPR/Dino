package com.example.dino.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dino.data.ExpeditionStore
import com.example.dino.data.Fossil
import com.example.dino.ui.theme.*

internal val FossilStage = Color(0xFFEDE9E1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FossilDetail(fossil:Fossil,store:ExpeditionStore,onBack:()->Unit,onAR:(String?)->Unit,onLab:()->Unit,onExplore:()->Unit) {
    val uri=LocalUriHandler.current
    var information by rememberSaveable(fossil.id) { mutableStateOf(false) }
    var selectedPart by rememberSaveable(fossil.id) { mutableStateOf<String?>(null) }
    var pieces by rememberSaveable(fossil.id) { mutableStateOf(false) }
    var inspectedPartId by rememberSaveable(fossil.id) { mutableStateOf<String?>(null) }
    var reset by remember(fossil.id) { mutableIntStateOf(0) }
    val complete=store.complete(fossil)
    val found=store.count(fossil)>0
    val ownedParts=fossil.fieldParts.filter {store.hasPart(fossil.id,it.id)}
    val inspectedPart=ownedParts.firstOrNull {it.id==inspectedPartId}
    val skullOnly=fossil.museumDisplay && !complete
    val previewPart=inspectedPart?.id ?: if(skullOnly) "head" else if(fossil.museumDisplay) "body" else null
    val modelTitle=inspectedPart?.name ?: if(skullOnly) "Crânio" else if(fossil.museumDisplay) "Esqueleto completo" else "Fóssil"
    Column(Modifier.fillMaxSize().background(FossilStage)) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack,modifier=Modifier.semantics { contentDescription="Voltar à coleção" }) {
                Canvas(Modifier.size(22.dp)) {
                    drawLine(Ink,Offset(size.width*.65f,size.height*.2f),Offset(size.width*.35f,size.height*.5f),2.dp.toPx(),StrokeCap.Round)
                    drawLine(Ink,Offset(size.width*.35f,size.height*.5f),Offset(size.width*.65f,size.height*.8f),2.dp.toPx(),StrokeCap.Round)
                }
            }
            Text(fossil.name,Modifier.weight(1f),fontSize=21.sp,fontWeight=FontWeight.SemiBold,color=Ink)
            if(found) IconButton(onClick={onAR(previewPart)},modifier=Modifier.semantics {contentDescription="Ver $modelTitle na câmera"}) {CameraGlyph()}
            IconButton(onClick={reset++},modifier=Modifier.semantics { contentDescription="Reenquadrar modelo 3D" }) {
                Text("↺",fontSize=29.sp,color=Earth)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            FossilViewer(fossil,Modifier.fillMaxSize(),previewPart=previewPart,
                visibleParts=if(inspectedPart!=null && inspectedPart.modelPath==null) setOf(inspectedPart.id) else null,
                onPart={selectedPart=it;information=true},immersive=true,resetKey=reset,stageColor=FossilStage)
            Text("Arraste para girar · use dois dedos para aproximar",
                Modifier.align(Alignment.BottomCenter).padding(bottom=18.dp,start=16.dp,end=16.dp),fontSize=10.sp,color=Muted)
        }
        Surface(shape=RoundedCornerShape(topStart=22.dp,topEnd=22.dp),color=CollectionPaper,shadowElevation=4.dp) {
            Column(Modifier.fillMaxWidth().padding(start=22.dp,end=22.dp,top=16.dp,bottom=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(modelTitle,Modifier.weight(1f),fontSize=18.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                    TextButton(onClick={selectedPart=inspectedPart?.id;information=true},contentPadding=PaddingValues(horizontal=6.dp)) {
                        Text("Informações",fontSize=12.sp,color=Earth)
                    }
                }
                Text("${fossil.period} · ${fossil.age}",fontSize=12.sp,lineHeight=18.sp,color=Muted)
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(5.dp).background(if(found) Amber else Muted,CircleShape))
                    Text(if(inspectedPart!=null) "${store.quantity(fossil.id,inspectedPart.id)} na coleção" else if(complete) "Montado no laboratório" else if(found) "Coletado" else "Ainda não coletado",Modifier.weight(1f),fontSize=11.sp,color=Earth)
                    if(inspectedPart!=null && complete) TextButton(onClick={inspectedPartId=null;reset++},contentPadding=PaddingValues(horizontal=6.dp)) {Text("Esqueleto",fontSize=12.sp,color=Earth)}
                    if(ownedParts.isNotEmpty()) TextButton(onClick={pieces=true},contentPadding=PaddingValues(horizontal=6.dp),modifier=Modifier.semantics {contentDescription="Abrir peças coletadas de ${fossil.name}"}) {Text("Peças",fontSize=12.sp,color=Earth)}
                }
                Button(onClick=when {complete->({onAR(previewPart)});found->onLab;else->onExplore},modifier=Modifier.fillMaxWidth().height(48.dp),
                    shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Earth,contentColor=CollectionPaper)) {
                    Text(when {complete->"Ver no meu ambiente · RA";!found->"Encontrar no mapa";fossil.museumDisplay->"Montar esqueleto";else->"Montar fóssil"},fontSize=14.sp)
                }
            }
        }
    }
    if(pieces) ModalBottomSheet(onDismissRequest={pieces=false},containerColor=CollectionPaper) {
        LazyColumn(contentPadding=PaddingValues(start=24.dp,end=24.dp,bottom=32.dp)) {
            item {
                Text("Peças de ${fossil.name}",fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                Text("Toque em uma peça para ver só ela em 3D.",Modifier.padding(top=6.dp,bottom=18.dp),fontSize=12.sp,color=Muted)
            }
            items(ownedParts,key={it.id}) {part ->
                Surface(onClick={inspectedPartId=part.id;pieces=false;reset++},color=CollectionPaper) {
                    Row(Modifier.fillMaxWidth().padding(vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        FossilArt(fossil.id,Modifier.size(64.dp),partId=part.id)
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text(part.name,fontSize=16.sp,fontWeight=FontWeight.Medium,color=Ink)
                            Text("${store.quantity(fossil.id,part.id)} na coleção",fontSize=12.sp,color=Muted)
                        }
                        Text(if(inspectedPart?.id==part.id) "✓" else "›",fontSize=21.sp,color=Earth)
                    }
                }
                HorizontalDivider(color=BoneWhite.copy(alpha=.5f))
            }
            if(complete) item {
                Surface(onClick={inspectedPartId=null;pieces=false;reset++},color=CollectionPaper) {
                    Row(Modifier.fillMaxWidth().padding(top=20.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        FossilArt(fossil.id,Modifier.size(64.dp),partId="body")
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text("Esqueleto completo",fontSize=16.sp,fontWeight=FontWeight.Medium,color=Ink)
                            Text("Montado no laboratório",fontSize=12.sp,color=Muted)
                        }
                        Text(if(inspectedPart==null) "✓" else "›",fontSize=21.sp,color=Earth)
                    }
                }
            }
        }
    }
    if(information) ModalBottomSheet(onDismissRequest={information=false},containerColor=CollectionPaper) {
        val part=fossil.parts.firstOrNull { it.id==selectedPart }
        LazyColumn(contentPadding=PaddingValues(start=24.dp,end=24.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item {
                Text(part?.name ?: fossil.name,fontSize=23.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                Text(if(part!=null) fossil.name else "${fossil.period} · ${fossil.age}",Modifier.padding(top=6.dp),fontSize=12.sp,color=Muted)
            }
            if(part!=null) item {Text(part.description,fontSize=14.sp,lineHeight=22.sp,color=Ink);HorizontalDivider(Modifier.padding(top=20.dp),color=BoneWhite)}
            item {Text("Sobre a espécie",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Ink);Text(fossil.description,Modifier.padding(top=8.dp),fontSize=14.sp,lineHeight=22.sp,color=Muted)}
            item {Text("Onde viveu",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Ink);Text(fossil.habitat,Modifier.padding(top=8.dp),fontSize=14.sp,lineHeight=22.sp,color=Muted)}
            item {Text("Partes",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Ink)}
            items(fossil.parts,key={it.id}) { component ->
                Column {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(component.name,Modifier.weight(1f),fontSize=14.sp,color=Ink)
                        TextButton(onClick={selectedPart=if(selectedPart==component.id) null else component.id}) {Text(if(selectedPart==component.id) "Fechar" else "Detalhes",fontSize=12.sp)}
                    }
                    Text(if(!component.collectible) if(store.isAssembled(fossil.id,component.id)) "Montado" else "Montar no laboratório" else if(store.hasPart(fossil.id,component.id)) "Coletado" else "Encontrar no mapa",fontSize=11.sp,color=Muted)
                    if(selectedPart==component.id) Text(component.description,Modifier.padding(top=8.dp),fontSize=13.sp,lineHeight=21.sp,color=Muted)
                    HorizontalDivider(Modifier.padding(top=12.dp),color=BoneWhite)
                }
            }
            item {
                Text("Reconstrução ilustrativa. O modelo não representa um espécime científico identificado.",fontSize=11.sp,lineHeight=17.sp,color=Muted)
                TextButton(onClick={uri.openUri(fossil.sourceUrl)},contentPadding=PaddingValues(0.dp)) {Text("Fonte · ${fossil.sourceName} ↗",fontSize=12.sp)}
                if(complete) TextButton(onClick={information=false;onLab()},contentPadding=PaddingValues(0.dp)) {Text("Abrir no laboratório",fontSize=12.sp)}
            }
        }
    }
}

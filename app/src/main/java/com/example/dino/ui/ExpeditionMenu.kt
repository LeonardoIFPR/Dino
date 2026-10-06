package com.example.dino.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dino.R
import com.example.dino.data.*
import com.example.dino.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpeditionMenu(store:ExpeditionStore,region:Region,markers:List<Encounter>,player:GeoPoint?,
    locationMessage:String,onGps:()->Unit,onRelocate:()->Unit,onSelect:(Encounter)->Unit,
    onOpenPage:(Int)->Unit,onCamera:()->Unit,onDismiss:()->Unit) {
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=CollectionPaper) {
        LazyColumn(contentPadding=PaddingValues(start=22.dp,end=22.dp,bottom=28.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(bottom=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    Portrait(store.explorerId,Modifier.size(56.dp))
                    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text(if(store.explorerId=="female") "Exploradora" else "Explorador",fontSize=21.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                        Text("Nível ${1+store.totalRecoveries/3} · ${store.collectedCount} achados",fontSize=12.sp,color=Muted)
                    }
                }
                listOf("Coleção","Laboratório","Diário").forEachIndexed {index,label ->
                    MenuLink(label,onClick={onDismiss();onOpenPage(index+1)}) {NavigationGlyph(index+1,Modifier.size(21.dp),Earth)}
                }
                MenuLink("Ver na câmera",onClick={onDismiss();onCamera()}) {CameraGlyph()}
                HorizontalDivider(Modifier.padding(top=8.dp,bottom=20.dp),color=BoneWhite.copy(alpha=.5f))
            }
            item {
                Text("Exploração",fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=Earth)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top=10.dp)) {
                    listOf(if(store.isEmulator) "Modo PC" else "Virtual","Meu GPS").forEachIndexed {index,label ->
                        SegmentedButton(selected=if(index==0) store.demo else !store.demo,
                            onClick={if(index==0) store.changeDemoMode(true) else onGps()},shape=SegmentedButtonDefaults.itemShape(index,2)) {Text(label,fontSize=12.sp)}
                    }
                }
                Text(if(store.demo && store.isEmulator) "Toque no mapa para caminhar. A localização é simulada." else if(store.demo) "${region.name} · ${region.place}" else locationMessage,
                    Modifier.padding(top=10.dp,bottom=16.dp),fontSize=11.sp,color=Muted)
                if(!store.demo) TextButton(onClick={onRelocate();onDismiss()},contentPadding=PaddingValues(0.dp)) {Text("Centralizar na minha localização",fontSize=12.sp,color=Earth)}
            }
            item {
                Text("Personagem",fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=Earth)
                Row(Modifier.fillMaxWidth().padding(top=12.dp,bottom=22.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    listOf("female" to "Exploradora","male" to "Explorador").forEach {(id,label) ->
                        Surface(onClick={store.selectExplorer(id)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(12.dp),
                            color=if(store.explorerId==id) Sand else CollectionPaper,
                            border=BorderStroke(1.dp,if(store.explorerId==id) Amber else BoneWhite.copy(alpha=.5f))) {
                            Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                Portrait(id,Modifier.size(40.dp))
                                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                    Text(label,fontSize=11.sp,fontWeight=FontWeight.Medium,color=Ink)
                                    Text(if(store.explorerId==id) "Selecionado" else "Selecionar",fontSize=9.sp,color=Muted)
                                }
                            }
                        }
                    }
                }
            }
            if(store.demo && Catalog.regions.size>1) items(Catalog.regions,key={it.id}) {choice ->
                MenuLink(choice.name,onClick={store.selectRegion(choice.id);onDismiss()}) {FossilArt(choice.fossilIds.first(),Modifier.size(24.dp))}
            }
            item {
                HorizontalDivider(color=BoneWhite.copy(alpha=.5f))
                Row(Modifier.fillMaxWidth().padding(top=20.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("Achados por perto",Modifier.weight(1f),fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=Earth)
                    Text("${markers.size}",fontSize=12.sp,color=Muted)
                }
                if(markers.isEmpty()) Text(if(!store.demo && player==null) "Aguarde um sinal de GPS para ver os achados." else "Nenhum novo achado nesta área.",Modifier.padding(vertical=12.dp),fontSize=12.sp,color=Muted)
            }
            items(markers,key={it.id}) {encounter ->
                Surface(onClick={onDismiss();onSelect(encounter)},color=CollectionPaper) {
                    Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        FossilArt(encounter.fossilId,Modifier.size(44.dp),partId=encounter.partId)
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {Text(encounter.fossil.name,fontSize=14.sp,color=Ink);Text(encounter.part.name,fontSize=11.sp,color=Muted)}
                        Text(player?.let {"${distance(it,encounter.point).toInt()} m"} ?: "—",fontSize=11.sp,color=Earth)
                    }
                }
            }
            item {Text("Expedição virtual; os pontos não indicam sítios reais.",Modifier.padding(top=18.dp),fontSize=10.sp,color=Muted)}
        }
    }
}

@Composable
private fun MenuLink(label:String,onClick:()->Unit,icon:@Composable ()->Unit) {
    Surface(onClick=onClick,color=CollectionPaper) {
        Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            icon()
            Text(label,Modifier.weight(1f),fontSize=15.sp,color=Ink)
            Text("›",fontSize=23.sp,color=Muted)
        }
    }
}

@Composable
private fun Portrait(id:String,modifier:Modifier) {
    Surface(modifier,shape=CircleShape,color=BoneWhite.copy(alpha=.65f)) {
        Image(painterResource(if(id=="female") R.drawable.explorer_female_portrait else R.drawable.explorer_male_portrait),contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CameraFossilPicker(store:ExpeditionStore,onSelect:(String)->Unit,onDismiss:()->Unit) {
    val owned=Catalog.fossils.filter {store.count(it)>0}
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=CollectionPaper) {
        LazyColumn(contentPadding=PaddingValues(start=24.dp,end=24.dp,bottom=30.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {Text("Ver na câmera",fontSize=23.sp,fontWeight=FontWeight.SemiBold,color=Ink);Text("Escolha um fóssil da sua coleção.",Modifier.padding(top=6.dp),fontSize=12.sp,color=Muted)}
            if(owned.isEmpty()) item {Text("Colete um crânio no mapa. Você também pode abrir a câmera durante a descoberta de um achado.",fontSize=13.sp,lineHeight=21.sp,color=Muted)}
            items(owned,key={it.id}) {fossil ->
                Surface(onClick={onSelect(fossil.id)},color=CollectionPaper) {
                    Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        FossilArt(fossil.id,Modifier.size(64.dp),partId=if(store.complete(fossil)) "body" else "head")
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {Text(fossil.name,fontSize=16.sp,fontWeight=FontWeight.Medium,color=Ink);Text(if(store.complete(fossil)) "Esqueleto completo" else "Crânio",fontSize=12.sp,color=Muted)}
                        CameraGlyph()
                    }
                }
            }
        }
    }
}

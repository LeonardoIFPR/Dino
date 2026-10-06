package com.example.dino.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dino.data.*
import com.example.dino.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalScreen(store:ExpeditionStore,onSelect:(String)->Unit,onBack:()->Unit,onReset:()->Unit) {
    val owned=Catalog.fossils.filter {store.count(it)>0}
    val mounted=owned.count {store.complete(it)}
    var panel by rememberSaveable {mutableStateOf<String?>(null)}
    var resetDialog by rememberSaveable {mutableStateOf(false)}
    val uri=LocalUriHandler.current
    Column(Modifier.fillMaxSize().background(CollectionPaper)) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack,modifier=Modifier.semantics {contentDescription="Voltar ao mapa"}) {
                Canvas(Modifier.size(22.dp)) {
                    drawLine(Ink,Offset(size.width*.65f,size.height*.2f),Offset(size.width*.35f,size.height*.5f),2.dp.toPx(),StrokeCap.Round)
                    drawLine(Ink,Offset(size.width*.35f,size.height*.5f),Offset(size.width*.65f,size.height*.8f),2.dp.toPx(),StrokeCap.Round)
                }
            }
            Text("Diário",Modifier.weight(1f),fontSize=23.sp,fontWeight=FontWeight.SemiBold,color=Ink)
            TextButton(onClick={panel="about"}) {Text("Sobre",fontSize=12.sp,color=Earth)}
        }
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=22.dp,vertical=16.dp)) {
            item {
                Text("${store.collectedCount} achados · $mounted montagens",fontSize=15.sp,fontWeight=FontWeight.Medium,color=Ink)
                Text("${owned.size} de ${Catalog.fossils.size} espécies registradas",Modifier.padding(top=6.dp,bottom=28.dp),fontSize=12.sp,color=Muted)
                Text("Registros",fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=Earth)
                HorizontalDivider(Modifier.padding(top=12.dp),color=BoneWhite.copy(alpha=.5f))
            }
            if(owned.isEmpty()) item {
                Text("Seu primeiro achado vai aparecer aqui.",Modifier.padding(vertical=28.dp),fontSize=14.sp,color=Muted)
                OutlinedButton(onClick=onBack) {Text("Explorar mapa")}
            }
            items(owned,key={it.id}) {fossil ->
                Surface(onClick={onSelect(fossil.id)},color=CollectionPaper) {
                    Row(Modifier.fillMaxWidth().padding(vertical=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                        FossilArt(fossil.id,Modifier.size(72.dp),partId=if(store.complete(fossil)) "body" else "head")
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text(fossil.name,fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                            Text(if(store.complete(fossil)) "Esqueleto montado" else "Crânio recuperado",fontSize=12.sp,color=Earth)
                            Text("${store.quantity(fossil)} achados · ${fossil.period}",fontSize=11.sp,color=Muted)
                        }
                        Text("›",fontSize=23.sp,color=Muted)
                    }
                }
                HorizontalDivider(color=BoneWhite.copy(alpha=.5f))
            }
            item {
                Spacer(Modifier.height(24.dp))
                Surface(onClick={panel="help"},color=CollectionPaper) {
                    Row(Modifier.fillMaxWidth().padding(vertical=16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("Como jogar",Modifier.weight(1f),fontSize=14.sp,color=Earth)
                        Text("›",fontSize=22.sp,color=Muted)
                    }
                }
                Text("Progresso salvo neste aparelho.",Modifier.padding(top=10.dp),fontSize=11.sp,color=Muted)
            }
        }
    }
    if(panel!=null) ModalBottomSheet(onDismissRequest={panel=null},containerColor=CollectionPaper) {
        LazyColumn(contentPadding=PaddingValues(start=24.dp,end=24.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item {Text(if(panel=="help") "Como jogar" else "Sobre o aplicativo",fontSize=23.sp,fontWeight=FontWeight.SemiBold,color=Ink)}
            if(panel=="help") {
                items(listOf("Explore" to "Use o GPS ou caminhe pelo mapa no modo virtual.","Escave" to "Abra um achado e remova a terra. O pincel leve preserva a peça; a espátula exige cuidado.","Monte" to "Leve todas as peças necessárias ao laboratório para liberar o esqueleto completo.","Veja na câmera" to "Use o botão de câmera no achado ou na coleção. A montagem salva define se você verá o crânio ou o esqueleto.")) {(title,description) ->
                    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {Text(title,fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Ink);Text(description,fontSize=13.sp,lineHeight=21.sp,color=Muted)}
                }
            } else {
                item {Text("Fósseis pelo Mapa · 0.3",fontSize=15.sp,fontWeight=FontWeight.Medium,color=Ink);Text("Achados e locais virtuais, com conteúdo educativo. A coleção, o laboratório e os modelos funcionam offline; o mapa precisa de internet.",Modifier.padding(top=8.dp),fontSize=13.sp,lineHeight=21.sp,color=Muted)}
                item {Text("Modelos e arte",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Ink);Text("Dinossauros e personagens fornecidos pelo responsável pelo projeto. A origem e a licença dos arquivos estão registradas no projeto como não informadas. Modelos ilustrativos antigos: CC0. Textura de escavação criada com imagegen.",Modifier.padding(top=8.dp),fontSize=12.sp,lineHeight=20.sp,color=Muted)}
                item {
                    Text("Fotografia do botão",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                    Text("Ammonite Asteroceras · Dlloyd / Wikimedia Commons · CC BY-SA 3.0. Foto original enquadrada em círculo na interface.",Modifier.padding(top=8.dp),fontSize=12.sp,lineHeight=20.sp,color=Muted)
                    TextButton(onClick={uri.openUri("https://commons.wikimedia.org/wiki/File:Ammonite_Asteroceras.jpg")},contentPadding=PaddingValues(0.dp)) {Text("Fonte da fotografia ↗",fontSize=12.sp)}
                    TextButton(onClick={uri.openUri("https://creativecommons.org/licenses/by-sa/3.0/")},contentPadding=PaddingValues(0.dp)) {Text("Licença da fotografia ↗",fontSize=12.sp)}
                }
                item {HorizontalDivider(color=BoneWhite);TextButton(onClick={resetDialog=true},contentPadding=PaddingValues(0.dp)) {Text("Reiniciar minha coleção",fontSize=12.sp,color=Clay)}}
            }
        }
    }
    if(resetDialog) AlertDialog(onDismissRequest={resetDialog=false},title={Text("Reiniciar a coleção?")},text={Text("Os achados e as montagens salvos neste aparelho serão apagados.")},
        confirmButton={TextButton(onClick={onReset();resetDialog=false;panel=null}) {Text("Reiniciar",color=Clay)}},dismissButton={TextButton(onClick={resetDialog=false}) {Text("Cancelar")}})
}

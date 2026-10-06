package com.example.dino.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dino.data.*
import com.example.dino.R
import com.example.dino.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldScreen(store:ExpeditionStore,region:Region,origin:GeoPoint,player:GeoPoint?,available:List<Encounter>,
                locationMessage:String,liveReady:Boolean,onGps:()->Unit,onSelect:(Encounter)->Unit,
                onWalk:(GeoPoint)->Unit,onRelocate:()->Unit,onOpenPage:(Int)->Unit,onCamera:()->Unit,gpsWalking:Boolean=false) {
    var recenter by remember { mutableIntStateOf(0) }
    var retry by remember { mutableIntStateOf(0) }
    var mapStatus by remember { mutableStateOf("Conectando ao mapa…") }
    var settings by remember { mutableStateOf(false) }
    val uri=LocalUriHandler.current
    val markers=if(store.demo || liveReady) available else emptyList()
    Box(Modifier.fillMaxSize()) {
        if(store.demo || player!=null) {
        ExpeditionWorldMap(origin,player,markers,if(store.demo) "demo:${region.id}" else "world-gps",recenter,retry,
            Modifier.fillMaxSize(),onSelect,if(store.demo) onWalk else null,onStatus={mapStatus=it},explorerId=store.explorerId,
            gpsWalking=if(store.demo) null else gpsWalking)
        } else Column(Modifier.align(Alignment.Center).padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Sua expedição começa aqui",fontSize=20.sp,fontWeight=FontWeight.SemiBold,color=Ink)
            Text(locationMessage,fontSize=13.sp,color=Muted)
            OutlinedButton(onClick=onGps) {Text("Ativar GPS")}
            TextButton(onClick={store.changeDemoMode(true)}) {Text("Explorar sem GPS",color=Earth)}
        }
        if(store.demo && store.isEmulator) Surface(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),shape=CircleShape,color=Parchment.copy(alpha=.92f)) {
            Text("PC · simulação",Modifier.padding(12.dp,8.dp),fontSize=10.sp,color=Earth)
        }
        Column(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top=12.dp,end=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            MapControl("⌖", "Centralizar",onClick={recenter++})
            MapControl("⋯", "Expedição",onClick={settings=true})
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=118.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)) {
            if(mapStatus.isNotEmpty()) Surface(onClick={retry++},shape=CircleShape,color=Parchment.copy(alpha=.94f)) {
                Text(mapStatus,Modifier.padding(12.dp,6.dp),fontSize=9.sp,color=Muted)
            }
            if ((!store.demo && !liveReady) || markers.isEmpty()) Surface(onClick={if(!store.demo && !liveReady) onGps() else if(store.demo) onOpenPage(1) else recenter++},shape=CircleShape,color=Earth.copy(alpha=.87f)) {
                Text(if(!store.demo && !liveReady) locationMessage else if(store.demo) "Área explorada · confira sua coleção" else "Caminhe para descobrir novos achados",Modifier.padding(13.dp,8.dp),fontSize=9.sp,color=BoneWhite)
            }
        }
        ExplorerProfileButton(store.explorerId,store.totalRecoveries,onClick={settings=true},
            modifier=Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start=16.dp,bottom=20.dp))
        FossilCollectionButton(store.collectedCount,onClick={onOpenPage(1)},
            modifier=Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=24.dp))
        TextButton(onClick={uri.openUri("https://www.openstreetmap.org/copyright")},modifier=Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end=8.dp,bottom=2.dp),contentPadding=PaddingValues(4.dp)) {
            Text("© OpenStreetMap contributors",color=Earth.copy(alpha=.7f),fontSize=8.sp)
        }
    }
    if(settings) ExpeditionMenu(store,region,markers,player,locationMessage,onGps,{recenter++;onRelocate()},onSelect,onOpenPage,onCamera,onDismiss={settings=false})
}

@Composable
private fun MapControl(glyph:String,label:String,onClick:()->Unit) {
    Surface(onClick=onClick,shape=CircleShape,color=Parchment.copy(alpha=.92f),shadowElevation=4.dp,modifier=Modifier.size(45.dp).semantics { contentDescription=label }) {
        Box(contentAlignment=Alignment.Center) {Text(glyph,color=Earth,fontSize=26.sp)}
    }
}

@Composable
private fun ExplorerProfileButton(explorerId:String,fragments:Int,onClick:()->Unit,modifier:Modifier=Modifier) {
    val level=1+fragments/3
    Surface(onClick=onClick,modifier=modifier.width(86.dp).semantics { contentDescription="Abrir perfil, nível $level" },
        shape=RoundedCornerShape(16.dp),color=Color.Transparent) {
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(70.dp)) {
                Surface(Modifier.fillMaxSize(),shape=CircleShape,color=BoneWhite,shadowElevation=6.dp,border=BorderStroke(2.dp,Parchment)) {
                    Image(painterResource(if(explorerId=="female") R.drawable.explorer_female_portrait else R.drawable.explorer_male_portrait),
                        contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize())
                }
                Surface(Modifier.align(Alignment.BottomStart).size(25.dp),shape=CircleShape,color=Earth,border=BorderStroke(1.5.dp,Parchment)) {
                    Box(contentAlignment=Alignment.Center) {Text("$level",color=Parchment,fontSize=11.sp,fontWeight=FontWeight.Bold)}
                }
            }
            LinearProgressIndicator(progress={fragments%3/3f},modifier=Modifier.width(65.dp).height(3.dp).clip(CircleShape),
                color=Amber,trackColor=Earth.copy(alpha=.25f))
            Text(if(explorerId=="female") "Exploradora" else "Explorador",fontSize=10.sp,fontWeight=FontWeight.SemiBold,color=Earth)
        }
    }
}

@Composable
private fun FossilCollectionButton(fragments:Int,onClick:()->Unit,modifier:Modifier=Modifier) {
    Box(modifier.size(80.dp),contentAlignment=Alignment.Center) {
        Surface(onClick=onClick,modifier=Modifier.size(76.dp).semantics { contentDescription="Abrir meus fósseis, $fragments fragmentos coletados" },
            shape=CircleShape,color=Parchment,shadowElevation=12.dp,border=BorderStroke(2.dp,Parchment)) {
            Box(Modifier.padding(5.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFE8BA65),Amber,Clay)))
                .border(1.dp,Earth.copy(alpha=.2f),CircleShape),contentAlignment=Alignment.Center) {
                Image(painterResource(R.drawable.ammonite_photo),contentDescription=null,
                    modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            }
        }
        if(fragments>0) Surface(Modifier.align(Alignment.TopEnd).size(23.dp),shape=CircleShape,color=Earth,border=BorderStroke(1.5.dp,Parchment)) {
            Box(contentAlignment=Alignment.Center) {Text(if(fragments>99) "99+" else "$fragments",fontSize=9.sp,fontWeight=FontWeight.Bold,color=Parchment)}
        }
    }
}


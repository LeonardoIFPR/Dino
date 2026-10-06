package com.example.dino.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dino.data.*
import com.example.dino.location.rememberPlayerLocation
import com.example.dino.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot

@Composable
fun FossilApp() {
    val context=LocalContext.current
    val store=remember {ExpeditionStore(context.applicationContext)}
    var tab by rememberSaveable {mutableIntStateOf(0)}
    var detailId by rememberSaveable {mutableStateOf<String?>(null)}
    var arId by rememberSaveable {mutableStateOf<String?>(null)}
    var arPartId by rememberSaveable {mutableStateOf<String?>(null)}
    var cameraPicker by rememberSaveable {mutableStateOf(false)}
    var encounterEpoch by rememberSaveable {mutableIntStateOf(0)}
    val encounterStates=key(encounterEpoch) {rememberSaveableStateHolder()}
    var encounterId by rememberSaveable {mutableStateOf<String?>(null)}
    var encounterFossilId by rememberSaveable {mutableStateOf<String?>(null)}
    var encounterPartId by rememberSaveable {mutableStateOf<String?>(null)}
    var encounterLat by rememberSaveable {mutableDoubleStateOf(0.0)}
    var encounterLng by rememberSaveable {mutableDoubleStateOf(0.0)}
    fun selectEncounter(encounter:Encounter) {
        encounterFossilId=encounter.fossilId;encounterPartId=encounter.partId
        encounterLat=encounter.point.latitude;encounterLng=encounter.point.longitude
        encounterId=encounter.id
    }
    var labId by rememberSaveable {mutableStateOf(Catalog.fossils.firstOrNull {store.count(it)>0}?.id ?: Catalog.fossils.first().id)}
    val detailFossil=Catalog.fossils.firstOrNull {it.id==detailId}
    val arFossil=Catalog.fossils.firstOrNull {it.id==arId}
    fun openAR(id:String,partId:String?=null) {
        val fossil=Catalog.fossils.firstOrNull {it.id==id} ?: return
        arPartId=partId ?: if(store.complete(fossil)) "body" else "head"
        arId=id
    }
    LaunchedEffect(detailId,arId) {
        if(detailId!=null && detailFossil==null) detailId=null
        if(arId!=null && arFossil==null) arId=null
    }
    var permissionVersion by remember {mutableIntStateOf(0)}
    val location=rememberPlayerLocation(!store.demo,permissionVersion)
    val scope=rememberCoroutineScope()
    val snackbar=remember {SnackbarHostState()}
    fun notify(message:String) {scope.launch {snackbar.showSnackbar(message)}}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionVersion++
        if(result[Manifest.permission.ACCESS_FINE_LOCATION]!=true) notify("Escolha localização precisa no Android ou use o modo Virtual.")
    }
    val region=Catalog.regions.firstOrNull {it.id==store.regionId} ?: Catalog.regions.first()
    var demoNorth by rememberSaveable(region.id) {mutableDoubleStateOf(0.0)}
    var demoEast by rememberSaveable(region.id) {mutableDoubleStateOf(0.0)}
    LaunchedEffect(Unit) {
        if(!store.demo && androidx.core.content.ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
            permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    val demoOrigin=GeoPoint(region.latitude,region.longitude)
    val origin=if(store.demo) demoOrigin else location.fix?.point ?: demoOrigin
    val player=if(store.demo) GeoPoint(region.latitude+demoNorth/111320,
        region.longitude+demoEast/(111320*cos(Math.toRadians(region.latitude)))) else location.fix?.point
    val allEncounters=remember(store.demo,region,origin,player) {
        if(store.demo && !store.isEmulator) encounters(region,origin) else player?.let {worldEncounters(it)} ?: emptyList()
    }
    val available=allEncounters.filterNot {store.isRecovered(it)}
    val selected=if(encounterId!=null && Catalog.fossils.any {fossil -> fossil.id==encounterFossilId && fossil.fieldParts.any {it.id==encounterPartId}})
        Encounter(encounterId!!,encounterFossilId!!,encounterPartId!!,GeoPoint(encounterLat,encounterLng))
        else allEncounters.firstOrNull {it.id==encounterId}
    LaunchedEffect(encounterId,selected) {
        if(encounterId!=null && selected==null) encounterId=null
    }
    val liveReady=location.fix?.isUsable()==true
    fun useGps() {
        store.changeDemoMode(false)
        val allowed=androidx.core.content.ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED
        if(!allowed) permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
        else {
            val manager=context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            if(!androidx.core.location.LocationManagerCompat.isLocationEnabled(manager))
                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }
    fun relocate() {
        if(location.fix?.isUsable()!=true) notify("Aguarde um sinal de GPS recente e preciso.")
    }
    fun inRange(encounter:Encounter):Boolean {
        val fix=location.fix
        return store.demo || (fix?.isUsable()==true && distance(fix.point,encounter.point)<=COLLECTION_RADIUS_METERS)
    }
    BackHandler(enabled=arId!=null || encounterId!=null || detailId!=null || tab!=0) {
        when {
            arId!=null -> arId=null
            encounterId!=null -> encounterId=null
            detailId!=null -> detailId=null
            else -> tab=0
        }
    }
    if(arFossil!=null) {FossilAR(arFossil,previewPart=arPartId,onBack={arId=null});return}
    Box(Modifier.fillMaxSize().background(if(detailFossil!=null && selected==null) FossilStage else if((tab==1 || tab==3) && selected==null) CollectionPaper else if(tab==2 && selected==null) FossilStage else Sand)) {
        if(selected!=null) {
            encounterStates.SaveableStateProvider(selected.id) {
                EncounterExperience(selected,inRange(selected),player?.let {distance(it,selected.point).toInt()},onBack={encounterStates.removeState(selected.id);encounterId=null},onCamera={openAR(selected.fossilId,selected.partId)}) { session ->
                    if(!session.recovered) notify("Termine a limpeza e preserve a integridade do fóssil.")
                    else if(!inRange(selected)) notify("Aguarde um sinal de GPS preciso e fique perto do achado para coletar.")
                    else {
                        if(store.collect(selected)) {encounterStates.removeState(selected.id);notify("${selected.part.name} recuperado! Confira sua coleção.")}
                        encounterId=null
                    }
                }
            }
        } else {
            if(detailFossil!=null) {
                Box(Modifier.statusBarsPadding().navigationBarsPadding()) {
                    FossilDetail(detailFossil,store,onBack={detailId=null},onAR={partId->openAR(detailFossil.id,partId)},
                        onLab={labId=detailFossil.id;detailId=null;tab=2},onExplore={
                            if(store.demo) Catalog.regions.firstOrNull { detailFossil.id in it.fossilIds }?.let { store.selectRegion(it.id) }
                            detailId=null;tab=0
                        })
                }
            } else if(tab==0) {
                WorldScreen(store,region,origin,player,available,location.message,liveReady,
                    onGps=::useGps,onSelect=::selectEncounter,onRelocate=::relocate,onCamera={cameraPicker=true},gpsWalking=location.walking,onOpenPage={tab=it;detailId=null},onWalk={point ->
                        val north=(point.latitude-region.latitude)*111320
                        val east=(point.longitude-region.longitude)*111320*cos(Math.toRadians(region.latitude))
                        val factor=if(!store.isEmulator && hypot(north,east)>250) 250/hypot(north,east) else 1.0
                        demoNorth=north*factor;demoEast=east*factor
                    })
            } else Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Box(Modifier.weight(1f)) {
                    when(tab) {
                        1 -> CollectionScreen(store,onSelect={detailId=it},onBack={tab=0},onLab={tab=2})
                        2 -> LabScreen(store,onExplore={tab=0},onAR={openAR(it)},initialFossilId=labId,onFind={id ->
                            if(store.demo) Catalog.regions.firstOrNull {id in it.fossilIds}?.let {store.selectRegion(it.id)}
                            tab=0
                        })
                        3 -> JournalScreen(store,onSelect={detailId=it},onBack={tab=0},onReset={
                            encounterEpoch++
                            store.reset()
                        })
                    }
                }
            }
        }
        SnackbarHost(snackbar,modifier=Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=if(selected==null && detailId==null && tab==0) 118.dp else 10.dp))
        if(cameraPicker) CameraFossilPicker(store,onSelect={cameraPicker=false;openAR(it)},onDismiss={cameraPicker=false})
    }
}

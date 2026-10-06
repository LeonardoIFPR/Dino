package com.example.dino.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.dino.data.Fossil
import com.example.dino.ui.theme.*
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode

@Composable
fun FossilAR(fossil:Fossil,previewPart:String?=null,onBack:()->Unit) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    fun hasPermission()=ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED
    var permitted by remember {mutableStateOf(hasPermission())}
    var resumeVersion by remember {mutableIntStateOf(0)}
    var mode by remember {mutableStateOf("checking")}
    var supported by remember {mutableStateOf(false)}
    var installed by remember {mutableStateOf(false)}
    var preferCamera by rememberSaveable {mutableStateOf(false)}
    var generation by remember {mutableIntStateOf(0)}
    var message by remember {mutableStateOf<String?>(null)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {permitted=it}
    LaunchedEffect(Unit) {if(!permitted) permission.launch(Manifest.permission.CAMERA)}
    DisposableEffect(owner) {
        val observer=object:DefaultLifecycleObserver {
            override fun onResume(owner:LifecycleOwner) {permitted=hasPermission();resumeVersion++}
        }
        owner.lifecycle.addObserver(observer)
        onDispose {owner.lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(permitted,resumeVersion,preferCamera) {
        if(permitted) {
            ArCoreApk.getInstance().checkAvailabilityAsync(context) {availability ->
                supported=availability.isSupported
                installed=availability==ArCoreApk.Availability.SUPPORTED_INSTALLED
                mode=if(installed && !preferCamera) "ar" else "camera"
            }
        }
    }
    val path=fossil.parts.firstOrNull {it.id==previewPart}?.modelPath ?: fossil.completeModelPath
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if(!permitted) Column(Modifier.align(Alignment.Center).padding(30.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
            CameraGlyph(Modifier.size(40.dp),BoneWhite)
            Text("Veja seu fóssil na câmera",fontSize=23.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
            Text("Permita o acesso à câmera para mostrar o fóssil no ambiente ao seu redor.",fontSize=14.sp,lineHeight=22.sp,color=BoneWhite)
            Button(onClick={permission.launch(Manifest.permission.CAMERA)}) {Text("Permitir câmera")}
            TextButton(onClick={context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}) {Text("Abrir configurações",color=BoneWhite)}
        } else when(mode) {
            "ar" -> key(path,generation) { AnchoredFossil(path,fossil,previewPart,onRestart={generation++},onFailure={preferCamera=true;mode="camera";message="RA indisponível agora. Modo câmera ativado."}) }
            "camera" -> CameraOverlay(fossil,previewPart,message)
            else -> CircularProgressIndicator(Modifier.align(Alignment.Center),color=Amber)
        }
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Surface(onClick=onBack,shape=CircleShape,color=Earth.copy(alpha=.8f),modifier=Modifier.size(44.dp).semantics {contentDescription="Fechar câmera"}) {
                Canvas(Modifier.padding(14.dp)) {
                    drawLine(Color.White,Offset.Zero,Offset(size.width,size.height),2.dp.toPx(),StrokeCap.Round)
                    drawLine(Color.White,Offset(size.width,0f),Offset(0f,size.height),2.dp.toPx(),StrokeCap.Round)
                }
            }
            if(permitted && mode!="checking") Surface(shape=RoundedCornerShape(24.dp),color=Earth.copy(alpha=.8f)) {
                TextButton(onClick={
                    message=null
                    if(mode=="ar") preferCamera=true
                    else if(installed) preferCamera=false
                    else if(supported) {
                        val activity=context.findActivity()
                        if(activity!=null) runCatching {ArCoreApk.getInstance().requestInstall(activity,true)}
                            .onSuccess {resumeVersion++}.onFailure {message="Não foi possível ativar a RA. Continue no modo câmera."}
                    }
                },enabled=mode=="ar" || installed || supported,contentPadding=PaddingValues(horizontal=14.dp,vertical=8.dp)) {
                    Text(if(mode=="ar") "RA · trocar para câmera" else if(installed) "Câmera · ativar RA" else if(supported) "Ativar RA" else "Câmera",fontSize=11.sp,color=Color.White)
                }
            }
        }
    }
}

private fun Context.findActivity():Activity?=when(this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun CameraOverlay(fossil:Fossil,previewPart:String?,notice:String?) {
    var ready by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    Box(Modifier.fillMaxSize()) {
        LiveCameraPreview(Modifier.fillMaxSize(),onReady={ready=true},onError={error=it})
        if(error==null) FossilViewer(fossil,Modifier.fillMaxSize().padding(start=28.dp,end=28.dp,top=100.dp,bottom=210.dp),
            previewPart=previewPart,immersive=true,stageColor=Color.Transparent)
        CameraCaption(fossil,previewPart,notice ?: error ?: if(!ready) "Preparando câmera…" else "Arraste para girar · use dois dedos para aproximar",
            subtitle="Modelo sobre a câmera · sem fixação no ambiente")
    }
}

@Composable
private fun LiveCameraPreview(modifier:Modifier,onReady:()->Unit,onError:(String)->Unit) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    val readyCallback by rememberUpdatedState(onReady)
    val errorCallback by rememberUpdatedState(onError)
    val view=remember(context) {PreviewView(context).apply {implementationMode=PreviewView.ImplementationMode.COMPATIBLE;scaleType=PreviewView.ScaleType.FILL_CENTER}}
    AndroidView(factory={view},modifier=modifier,onReset=null)
    DisposableEffect(owner,view) {
        val future=ProcessCameraProvider.getInstance(context)
        val preview=Preview.Builder().build()
        var provider:ProcessCameraProvider?=null
        var disposed=false
        val streamObserver=Observer<PreviewView.StreamState> {if(it==PreviewView.StreamState.STREAMING) readyCallback()}
        view.previewStreamState.observe(owner,streamObserver)
        future.addListener({
            if(!disposed) try {
                val cameraProvider=future.get()
                provider=cameraProvider
                val selector=if(cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                preview.setSurfaceProvider(view.surfaceProvider)
                cameraProvider.bindToLifecycle(owner,selector,preview)
            } catch(_:Exception) {errorCallback("Não foi possível abrir a câmera. Feche outros apps que estejam usando a câmera e tente novamente.")}
        },ContextCompat.getMainExecutor(context))
        onDispose {disposed=true;view.previewStreamState.removeObserver(streamObserver);provider?.unbind(preview)}
    }
}

private class PlacementState {var hit:HitResult?=null}

@Composable
private fun AnchoredFossil(path:String,fossil:Fossil,previewPart:String?,onRestart:()->Unit,onFailure:()->Unit) {
    val resources=rememberModelSceneResources()
    val asset=rememberModelAsset(resources.loader,path)
    val model=asset.model
    val nodes=remember {mutableStateListOf<AnchorNode>()}
    val placement=remember {PlacementState()}
    var ready by remember {mutableStateOf(false)}
    var placed by remember {mutableStateOf(false)}
    var tracking by remember {mutableStateOf(true)}
    var viewport by remember {mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)}
    Box(Modifier.fillMaxSize()) {
        ARScene(modifier=Modifier.fillMaxSize().onSizeChanged {viewport=it},engine=resources.engine,modelLoader=resources.loader,
            onViewCreated={resources.view=this},childNodes=nodes,planeRenderer=false,
            sessionConfiguration={_,config ->config.planeFindingMode=Config.PlaneFindingMode.HORIZONTAL;config.lightEstimationMode=Config.LightEstimationMode.ENVIRONMENTAL_HDR},
            onSessionFailed={onFailure()},onTrackingFailureChanged={tracking=it==null},
            onSessionUpdated={_,frame ->
                if(!placed) {
                    placement.hit=if(frame.camera.trackingState==TrackingState.TRACKING && viewport.width>0)
                        frame.hitTest(viewport.width/2f,viewport.height/2f).firstOrNull {hit ->
                            val plane=hit.trackable as? Plane
                            plane!=null && plane.type==Plane.Type.HORIZONTAL_UPWARD_FACING && plane.isPoseInPolygon(hit.hitPose)
                        } else null
                    ready=placement.hit!=null
                }
            })
        if(!placed) Canvas(Modifier.align(Alignment.Center).size(44.dp)) {
            val color=if(ready) Amber else Color.White.copy(alpha=.75f)
            val length=size.width*.26f
            listOf(Offset.Zero,Offset(size.width,0f),Offset(0f,size.height),Offset(size.width,size.height)).forEach {corner ->
                drawLine(color,corner,corner+Offset(if(corner.x==0f) length else -length,0f),2.dp.toPx(),StrokeCap.Round)
                drawLine(color,corner,corner+Offset(0f,if(corner.y==0f) length else -length),2.dp.toPx(),StrokeCap.Round)
            }
        }
        CameraCaption(fossil,previewPart,if(asset.loading) "Preparando fóssil…" else if(model==null) "Não foi possível abrir o modelo 3D." else if(!tracking) "Mova o celular devagar para recuperar o rastreamento." else if(placed) "Arraste para mover · use dois dedos para girar e ajustar." else if(ready) "Superfície encontrada. Posicione o fóssil aqui." else "Aponte para uma mesa ou para o chão e mova o celular devagar.",
            buttonLabel=if(placed) "Reposicionar" else "Posicionar aqui",buttonEnabled=placed || (ready && model!=null && tracking),onButton={
                if(placed) onRestart() else {
                    val hit=placement.hit
                    if(hit!=null && model!=null) runCatching {
                        val anchor=AnchorNode(resources.engine,hit.createAnchor())
                        anchor.addChildNode(ModelNode(model,scaleToUnits=if(previewPart=="body") .7f else .28f,centerOrigin=Position(y=-.5f)).apply {isEditable=true})
                        nodes.add(anchor);placed=true
                    }.onFailure {ready=false;placement.hit=null}
                }
            })
    }
    ReleaseModelScene(resources)
}

@Composable
private fun BoxScope.CameraCaption(fossil:Fossil,previewPart:String?,message:String,subtitle:String?=null,
    buttonLabel:String?=null,buttonEnabled:Boolean=true,onButton:()->Unit={}) {
    Surface(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp),shape=RoundedCornerShape(16.dp),color=Earth.copy(alpha=.9f)) {
        Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(fossil.name,fontSize=19.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
            Text(if(previewPart=="body") "Esqueleto completo" else "Crânio",fontSize=11.sp,color=BoneWhite)
            Text(message,fontSize=12.sp,lineHeight=18.sp,color=Color.White)
            if(subtitle!=null) Text(subtitle,fontSize=10.sp,color=BoneWhite)
            if(buttonLabel!=null) OutlinedButton(onClick=onButton,enabled=buttonEnabled,modifier=Modifier.fillMaxWidth(),
                colors=ButtonDefaults.outlinedButtonColors(contentColor=BoneWhite,disabledContentColor=BoneWhite.copy(alpha=.4f))) {Text(buttonLabel,fontSize=12.sp)}
        }
    }
}

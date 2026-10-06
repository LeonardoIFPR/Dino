package com.example.dino.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.dino.R
import com.example.dino.data.*
import com.example.dino.ui.theme.*
import kotlin.math.*

private val DigPaper = Color(0xFFFAF7F0)
private val DigRed = Color(0xFFAD442F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncounterExperience(encounter:Encounter,inRange:Boolean,distanceMeters:Int?,
                        onBack:()->Unit,onCamera:()->Unit,onCollect:(CleaningSession)->Unit) {
    var phase by rememberSaveable(encounter.id) { mutableStateOf("inspect") }
    val session=rememberSaveable(encounter.id,saver=CleaningSession.Saver) { CleaningSession() }
    var toolName by rememberSaveable(encounter.id) { mutableStateOf(CleaningTool.BRUSH.name) }
    var pressure by rememberSaveable(encounter.id) { mutableFloatStateOf(.35f) }
    var information by rememberSaveable(encounter.id) { mutableStateOf(false) }
    val tool=CleaningTool.valueOf(toolName)
    val cleaning=phase=="clean"
    val fossil=encounter.fossil
    val uri=LocalUriHandler.current
    fun restart() { session.restart();toolName=CleaningTool.BRUSH.name;pressure=.35f }
    fun back() { if(cleaning) phase="inspect" else onBack() }
    BackHandler { back() }

    Column(Modifier.fillMaxSize().background(DigPaper).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=::back,modifier=Modifier.semantics { contentDescription="Voltar" }) {
                Canvas(Modifier.size(22.dp)) {
                    drawLine(Ink,Offset(size.width*.65f,size.height*.2f),Offset(size.width*.35f,size.height*.5f),2.dp.toPx(),StrokeCap.Round)
                    drawLine(Ink,Offset(size.width*.35f,size.height*.5f),Offset(size.width*.65f,size.height*.8f),2.dp.toPx(),StrokeCap.Round)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(fossil.name,fontSize=20.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                Text("${encounter.part.name} · escavação virtual",fontSize=11.sp,color=Muted)
            }
            if(!cleaning) IconButton(onClick=onCamera,modifier=Modifier.semantics {contentDescription="Ver crânio na câmera"}) {CameraGlyph()}
            if(cleaning) TextButton(onClick=::restart,contentPadding=PaddingValues(horizontal=8.dp)) { Text("Reiniciar",fontSize=11.sp,color=Earth) }
            else IconButton(onClick={information=true},modifier=Modifier.semantics { contentDescription="Informações científicas" }) {
                Box(Modifier.size(22.dp).background(Color(0xFFEAE2D4),CircleShape),contentAlignment=Alignment.Center) {
                    Text("i",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Earth)
                }
            }
        }
        if(cleaning) Row(Modifier.fillMaxWidth().padding(start=22.dp,end=22.dp,bottom=14.dp),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
            RecoveryMeter("Limpeza",session.progress,Amber,Modifier.weight(1f))
            RecoveryMeter("Integridade",session.integrity/100,if(session.integrity<45) DigRed else Earth,Modifier.weight(1f))
        }
        ExcavationSite(encounter,session,cleaning,tool,pressure,Modifier.fillMaxWidth().weight(1f))
        Surface(color=DigPaper,shadowElevation=6.dp) {
            Column(Modifier.fillMaxWidth().padding(start=22.dp,end=22.dp,top=16.dp,bottom=18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(!cleaning) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text("Um crânio entre as rochas",Modifier.weight(1f),fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                        Text(distanceMeters?.let { "$it m" } ?: "Demo",fontSize=11.sp,color=Earth)
                    }
                    Text("Remova a terra com cuidado para recuperar o fóssil.",fontSize=12.sp,lineHeight=18.sp,color=Muted)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(fossil.period,Modifier.weight(1f),fontSize=11.sp,color=Muted)
                        TextButton(onClick={information=true},contentPadding=PaddingValues(0.dp)) { Text("Sobre o achado",fontSize=11.sp,color=Earth) }
                    }
                    Button(onClick={phase="clean"},enabled=inRange,modifier=Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(12.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=Earth,contentColor=DigPaper)) {
                        Text(if(inRange) if(session.strokes>0) "Continuar escavação" else "Escavar fóssil" else "Aproxime-se para escavar",fontSize=14.sp)
                    }
                    if(!inRange) Text("Fique a até 100 m do achado, com um sinal de GPS preciso.",fontSize=11.sp,color=Muted)
                } else {
                    Text(when {
                        session.broken -> "A peça quebrou. Tente de novo com menos força."
                        session.recovered -> "Crânio recuperado! Pronto para a coleção."
                        tool==CleaningTool.PICK -> "Retire a terra grossa. Evite os ossos expostos."
                        else -> "Passe o pincel na terra até liberar o crânio."
                    },fontSize=12.sp,lineHeight=18.sp,color=if(session.broken) DigRed else Earth)
                    if(!session.broken && !session.recovered) {
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            ToolChoice("Pincel",CleaningTool.BRUSH,tool==CleaningTool.BRUSH,Modifier.weight(1f)) { toolName=CleaningTool.BRUSH.name }
                            ToolChoice("Espátula",CleaningTool.PICK,tool==CleaningTool.PICK,Modifier.weight(1f)) { toolName=CleaningTool.PICK.name }
                        }
                        Row(Modifier.fillMaxWidth().height(40.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            Text("Força",fontSize=11.sp,color=Muted)
                            Slider(value=pressure,onValueChange={pressure=it},valueRange=.15f..1f,modifier=Modifier.weight(1f))
                            Text(if(pressure>.78f) "Forte" else if(pressure>.5f) "Média" else "Leve",Modifier.width(36.dp),fontSize=11.sp,color=if(pressure>.78f) DigRed else Earth)
                        }
                    }
                    Button(onClick=if(session.broken) ::restart else ({onCollect(session)}),
                        enabled=session.broken || (session.recovered && inRange),
                        modifier=Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(12.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=Earth,contentColor=DigPaper)) {
                        Text(when {
                            session.broken -> "Tentar novamente"
                            session.recovered && !inRange -> "Aguarde o GPS para coletar"
                            session.recovered -> "Guardar na coleção"
                            else -> "Libere o crânio para coletar"
                        },fontSize=13.sp)
                    }
                }
            }
        }
    }
    if(information) ModalBottomSheet(onDismissRequest={information=false},containerColor=DigPaper) {
        LazyColumn(contentPadding=PaddingValues(start=24.dp,end=24.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item {Text(fossil.name,fontSize=24.sp,fontWeight=FontWeight.SemiBold,color=Ink);Text("${fossil.period} · ${fossil.age}",Modifier.padding(top=6.dp),fontSize=12.sp,color=Muted)}
            item {Text(encounter.part.name,fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=Ink);Text(encounter.part.description,Modifier.padding(top=8.dp),fontSize=14.sp,lineHeight=22.sp,color=Muted)}
            item {Text("Sobre a espécie",fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=Ink);Text(fossil.description,Modifier.padding(top=8.dp),fontSize=14.sp,lineHeight=22.sp,color=Muted)}
            item {Text("Onde viveu",fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=Ink);Text(fossil.habitat,Modifier.padding(top=8.dp),fontSize=14.sp,lineHeight=22.sp,color=Muted)}
            item {
                Text("Cena e localização virtuais. O modelo é uma reconstrução ilustrativa.",fontSize=11.sp,lineHeight=17.sp,color=Muted)
                TextButton(onClick={uri.openUri(fossil.sourceUrl)},contentPadding=PaddingValues(0.dp)) {Text("Fonte · ${fossil.sourceName} ↗",fontSize=12.sp)}
            }
        }
    }
}

@Composable
private fun RecoveryMeter(label:String,progress:Float,color:Color,modifier:Modifier) {
    Column(modifier,verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(label,fontSize=11.sp,color=Muted)
            Text("${(progress*100).toInt()}%",fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=color)
        }
        LinearProgressIndicator(progress={progress},color=color,trackColor=Color(0xFFE7DECF),modifier=Modifier.fillMaxWidth().height(3.dp).clip(CircleShape))
    }
}

@Composable
private fun ToolChoice(name:String,tool:CleaningTool,selected:Boolean,modifier:Modifier,onClick:()->Unit) {
    Surface(onClick=onClick,modifier=modifier.height(48.dp),shape=RoundedCornerShape(10.dp),color=if(selected) Earth else Color(0xFFEEE8DE)) {
        Row(Modifier.padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            ToolGlyph(tool,if(selected) BoneWhite else Earth,Modifier.size(24.dp))
            Text(name,fontSize=13.sp,fontWeight=FontWeight.Medium,color=if(selected) DigPaper else Earth)
        }
    }
}

@Composable
private fun ToolGlyph(tool:CleaningTool,color:Color,modifier:Modifier) {
    Canvas(modifier) {
        if(tool==CleaningTool.BRUSH) {
            drawLine(color,Offset(size.width*.7f,size.height*.12f),Offset(size.width*.43f,size.height*.59f),size.width*.14f,StrokeCap.Round)
            val tip=Path().apply {moveTo(size.width*.34f,size.height*.48f);lineTo(size.width*.56f,size.height*.61f);lineTo(size.width*.4f,size.height*.88f);lineTo(size.width*.13f,size.height*.72f);close()}
            drawPath(tip,color.copy(alpha=.8f))
        } else {
            drawLine(color,Offset(size.width*.75f,size.height*.13f),Offset(size.width*.46f,size.height*.48f),size.width*.14f,StrokeCap.Round)
            val blade=Path().apply {moveTo(size.width*.4f,size.height*.39f);lineTo(size.width*.61f,size.height*.57f);lineTo(size.width*.26f,size.height*.88f);lineTo(size.width*.13f,size.height*.73f);close()}
            drawPath(blade,color,style=Stroke(2.dp.toPx()))
        }
    }
}

@Composable
private fun ExcavationSite(encounter:Encounter,session:CleaningSession,cleaning:Boolean,tool:CleaningTool,pressure:Float,modifier:Modifier) {
    val context=LocalContext.current
    val soil=remember { BitmapFactory.decodeResource(context.resources,R.drawable.excavation_soil) }
    val mask=remember(session) { Bitmap.createBitmap(session.columns,session.rows,Bitmap.Config.ARGB_8888) }
    val pixels=remember(session) { IntArray(session.columns*session.rows) }
    val texturePaint=remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val maskPaint=remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { xfermode=PorterDuffXfermode(PorterDuff.Mode.DST_IN) } }
    var dimensions by remember { mutableStateOf(IntSize.Zero) }
    var cursor by remember { mutableStateOf<Offset?>(null) }
    val latestTool by rememberUpdatedState(tool)
    val latestPressure by rememberUpdatedState(pressure)
    val active by rememberUpdatedState(cleaning)
    val haptic=LocalHapticFeedback.current
    fun scrub(position:Offset) {
        if(dimensions.width<=0 || dimensions.height<=0 || !active) return
        val before=session.integrity
        session.brush(position.x/dimensions.width,position.y/dimensions.height,.105f,latestPressure,latestTool)
        if(before-session.integrity>1.5f) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    var input:Modifier=Modifier
    if(cleaning) input=Modifier.pointerInput(session) {
        detectDragGestures(onDragStart={cursor=it;scrub(it)},onDragEnd={cursor=null},onDragCancel={cursor=null}) {change,_ ->
            change.consume()
            val old=cursor ?: change.position
            val movement=change.position-old
            val steps=(movement.getDistance()/(dimensions.width*.035f)).toInt().coerceIn(1,24)
            for(i in 1..steps) scrub(old+movement*(i.toFloat()/steps))
            cursor=change.position
        }
    }.pointerInput(session) { detectTapGestures { scrub(it) } }

    Box(modifier.onSizeChanged { dimensions=it }.then(input)) {
        Image(painterResource(R.drawable.excavation_soil),contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        // Use the actual GLB in both phases; sediment is composited over the native scene.
        FossilViewer(encounter.fossil,Modifier.fillMaxSize().padding(28.dp),previewPart=encounter.partId,
            immersive=true,stageColor=Color.Transparent,interactive=!cleaning,surfaceOnTop=false)
        Canvas(Modifier.fillMaxSize()) {
            session.sediment.forEachIndexed { index,level ->
                val x=(index%session.columns+.5f)/session.columns
                val y=(index/session.columns+.5f)/session.rows
                val distance=hypot((x-.5f)/.33f,(y-.5f)/.35f)
                val discovery=(.2f+(distance-.2f)*1.5f+.12f*sin(index*1.7f)).coerceIn(.2f,1f)
                // Keep the same initially exposed patch when entering excavation.
                val opacity=level*discovery
                pixels[index]=((opacity*255).toInt().coerceIn(0,255) shl 24) or 0x00FFFFFF
            }
            mask.setPixels(pixels,0,session.columns,0,0,session.columns,session.rows)
            drawIntoCanvas { canvas ->
                val native=canvas.nativeCanvas
                val destination=RectF(0f,0f,size.width,size.height)
                val crop=if(size.width>size.height) {
                    val height=(soil.width*size.height/size.width).toInt()
                    Rect(0,(soil.height-height)/2,soil.width,(soil.height+height)/2)
                } else {
                    val width=(soil.height*size.width/size.height).toInt()
                    Rect((soil.width-width)/2,0,(soil.width+width)/2,soil.height)
                }
                val layer=native.saveLayer(destination,null)
                native.drawBitmap(soil,crop,destination,texturePaint)
                native.drawBitmap(mask,null,destination,maskPaint)
                native.restoreToCount(layer)
            }
            if(session.integrity<65) {
                val crack=Path().apply {moveTo(size.width*.4f,size.height*.25f);lineTo(size.width*.48f,size.height*.43f);lineTo(size.width*.43f,size.height*.55f);lineTo(size.width*.57f,size.height*.71f)}
                drawPath(crack,Earth.copy(alpha=(1-session.integrity/100).coerceAtLeast(.2f)),style=Stroke(if(session.broken) 3.dp.toPx() else 1.5.dp.toPx()))
            }
            cursor?.let { point ->
                drawCircle(BoneWhite.copy(alpha=.1f),size.width*.105f,point)
                drawCircle(BoneWhite.copy(alpha=.45f),size.width*.105f,point,style=Stroke(1.dp.toPx()))
                repeat(9) { i ->
                    val angle=i*2.4f
                    val radius=size.width*(.025f+i*.009f)
                    drawCircle(BoneWhite.copy(alpha=.6f),1.dp.toPx()+i%3,point+Offset(cos(angle)*radius,sin(angle)*radius))
                }
            }
        }
        Surface(Modifier.align(Alignment.TopStart).padding(16.dp),color=Earth.copy(alpha=.8f),shape=RoundedCornerShape(6.dp)) {
            Text("ÁREA DE ESCAVAÇÃO · VIRTUAL",Modifier.padding(10.dp,7.dp),fontSize=8.sp,letterSpacing=.7.sp,color=DigPaper)
        }
        if(!cleaning) Text("Arraste para observar o crânio",Modifier.align(Alignment.BottomCenter)
            .padding(bottom=18.dp).background(Earth.copy(alpha=.78f),RoundedCornerShape(6.dp)).padding(10.dp,7.dp),fontSize=10.sp,color=DigPaper)
        cursor?.let { point ->
            ToolGlyph(tool,BoneWhite,Modifier.offset { IntOffset(point.x.toInt()+8,point.y.toInt()-54) }.size(42.dp))
        }
    }
}

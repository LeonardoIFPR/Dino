package com.example.dino.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.example.dino.data.*
import com.example.dino.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties
import kotlin.math.*

private data class WorldPoint(val x: Double, val y: Double)
private fun project(point: GeoPoint): WorldPoint {
    val latitude = point.latitude.coerceIn(-85.05112878, 85.05112878)
    val radians = Math.toRadians(latitude)
    return WorldPoint((point.longitude + 180) / 360, (1 - ln(tan(radians) + 1 / cos(radians)) / PI) / 2)
}
private fun unproject(point: WorldPoint) = GeoPoint(
    Math.toDegrees(atan(sinh(PI * (1 - 2 * point.y)))), ((point.x * 360) % 360 + 360) % 360 - 180)
private data class MapTile(val zoom: Int, val x: Int, val y: Int) {
    val fileName get() = "$zoom-$x-$y"
}

/** Visible tiles only, limited parallel requests, HTTPS and persistent HTTP cache metadata. */
private class TileRepository(cacheRoot: File) {
    private val directory = File(cacheRoot, "world-map-tiles").apply { mkdirs() }
    private val semaphore = Semaphore(4)
    @Volatile private var retryAfterMillis=0L
    suspend fun load(tile: MapTile): ImageBitmap? = semaphore.withPermit {
        withContext(Dispatchers.IO) {
            val file = File(directory, "${tile.fileName}.png")
            val metadata = File(directory, "${tile.fileName}.properties")
            val properties = Properties().apply { if (metadata.exists()) metadata.inputStream().use { load(it) } }
            val expires = properties.getProperty("expires")?.toLongOrNull() ?: 0L
            fun cached() = if (file.exists()) BitmapFactory.decodeFile(file.path)?.asImageBitmap() else null
            if (expires > System.currentTimeMillis()) return@withContext cached()
            if(System.currentTimeMillis()<retryAfterMillis) return@withContext cached()
            val connection = URL("https://tile.openstreetmap.org/${tile.zoom}/${tile.x}/${tile.y}.png").openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 6000; connection.readTimeout = 8000
                connection.setRequestProperty("User-Agent", "FosseisPeloMapa/0.3 (Android; educational fossil exploration; com.example.dino)")
                properties.getProperty("etag")?.let { connection.setRequestProperty("If-None-Match", it) }
                properties.getProperty("modified")?.let { connection.setRequestProperty("If-Modified-Since", it) }
                fun cacheExpiry():Long {
                    val maxAge=Regex("max-age=(\\d+)").find(connection.getHeaderField("Cache-Control") ?: "")?.groupValues?.get(1)?.toLongOrNull()
                    return if(maxAge!=null) System.currentTimeMillis()+maxAge*1000
                        else connection.getHeaderFieldDate("Expires",System.currentTimeMillis()+7*86_400_000L)
                }
                if (connection.responseCode == HttpURLConnection.HTTP_NOT_MODIFIED && file.exists()) {
                    properties.setProperty("expires",cacheExpiry().toString())
                } else if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val bytes = connection.inputStream.use { it.readBytes() }
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext cached()
                    file.writeBytes(bytes)
                    properties.setProperty("expires",cacheExpiry().toString())
                    connection.getHeaderField("ETag")?.let { properties.setProperty("etag", it) }
                    connection.getHeaderField("Last-Modified")?.let { properties.setProperty("modified", it) }
                    metadata.outputStream().use { properties.store(it, null) }
                    return@withContext bitmap.asImageBitmap()
                } else {
                    if(connection.responseCode==429 || connection.responseCode==HttpURLConnection.HTTP_UNAVAILABLE) {
                        val seconds=connection.getHeaderField("Retry-After")?.toLongOrNull()
                        retryAfterMillis=if(seconds!=null) System.currentTimeMillis()+seconds.coerceAtLeast(5)*1000
                            else connection.getHeaderFieldDate("Retry-After",System.currentTimeMillis()+60_000)
                    }
                    return@withContext cached()
                }
                metadata.outputStream().use { properties.store(it, null) }
                cached()
            } catch (_: IOException) { cached() }
            finally { connection.disconnect() }
        }
    }
}

@Composable
fun ExpeditionWorldMap(
    origin: GeoPoint, player: GeoPoint?, markers: List<Encounter>, focusKey: String,
    recenter: Int, retry: Int, modifier: Modifier = Modifier,
    onSelect: (Encounter) -> Unit, onWalk: ((GeoPoint) -> Unit)?, onStatus: (String) -> Unit,
    explorerId: String = "male", gpsWalking: Boolean? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val repository = remember { TileRepository(context.cacheDir) }
    var camera by remember { mutableStateOf(project(player ?: origin)) }
    var zoom by remember { mutableFloatStateOf(18f) }
    var following by remember(focusKey) { mutableStateOf(true) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val images = remember { mutableStateMapOf<MapTile, ImageBitmap?>() }
    val statusCallback by rememberUpdatedState(onStatus)
    val selectCallback by rememberUpdatedState(onSelect)
    val walkCallback by rememberUpdatedState(onWalk)
    val currentMarkers by rememberUpdatedState(markers)
    var fromPlayer by remember(focusKey) {mutableStateOf(player ?: origin)}
    var toPlayer by remember(focusKey) {mutableStateOf(player ?: origin)}
    val stride=remember(focusKey) {Animatable(1f)}
    fun interpolatePlayer():GeoPoint {
        val delta=((toPlayer.longitude-fromPlayer.longitude+540)%360)-180
        return GeoPoint(fromPlayer.latitude+(toPlayer.latitude-fromPlayer.latitude)*stride.value,
            ((fromPlayer.longitude+delta*stride.value+540)%360)-180)
    }
    LaunchedEffect(player,focusKey) {
        val next=player ?: return@LaunchedEffect
        val current=interpolatePlayer()
        val jump=distance(current,next)>200f
        fromPlayer=current;toPlayer=next
        stride.snapTo(if(jump) 1f else 0f)
        if(!jump) stride.animateTo(1f,tween(1300,easing=LinearEasing))
    }
    val animatedPlayer=interpolatePlayer()
    val walking=gpsWalking ?: stride.isRunning
    val activeCamera=if(following && player!=null) project(animatedPlayer) else camera
    val currentCamera by rememberUpdatedState(activeCamera)
    var previousPlayer by remember(focusKey) {mutableStateOf(player)}
    var heading by remember(focusKey) {mutableFloatStateOf(0f)}
    LaunchedEffect(player) {
        val old=previousPlayer
        if(old!=null && player!=null && distance(old,player)>2.5f) {
            val north=player.latitude-old.latitude
            val east=(((player.longitude-old.longitude+540)%360)-180)*cos(Math.toRadians(player.latitude))
            heading=Math.toDegrees(atan2(east,-north)).toFloat()
        }
        previousPlayer=player
    }
    val scale = 256.0 * 2.0.pow(zoom.toDouble()) * density * .65
    val integerZoom = floor(zoom).toInt()
    val tileScale = 256.0 * 2.0.pow((zoom - integerZoom).toDouble()) * density * .65
    val tileCount = 1 shl integerZoom
    val centerOnScreen = Offset(viewport.width*.5f, viewport.height*.58f)
    fun screenPosition(point: GeoPoint): Offset {
        val projected = project(point)
        var dx = projected.x - activeCamera.x
        if (dx > .5) dx -= 1; if (dx < -.5) dx += 1
        return Offset(centerOnScreen.x + (dx * scale).toFloat(), centerOnScreen.y + ((projected.y-activeCamera.y)*scale).toFloat())
    }
    val left = floor(activeCamera.x*tileCount - centerOnScreen.x/tileScale).toInt()
    val right = floor(activeCamera.x*tileCount + (viewport.width-centerOnScreen.x)/tileScale).toInt()
    val top = floor(activeCamera.y*tileCount - centerOnScreen.y/tileScale).toInt().coerceAtLeast(0)
    val bottom = floor(activeCamera.y*tileCount + (viewport.height-centerOnScreen.y)/tileScale).toInt().coerceAtMost(tileCount-1)
    val tiles = if (viewport.width == 0) emptyList() else (left..right).flatMap { x -> (top..bottom).map { y -> MapTile(integerZoom, ((x % tileCount)+tileCount)%tileCount, y) } }
    val explorerPosition=screenPosition(animatedPlayer)
    LaunchedEffect(focusKey,recenter) {following=true;camera=project(player ?: origin);zoom=18f}
    LaunchedEffect(tiles, retry) {
        val existing = images.keys.toList()
        existing.filterNot { it in tiles }.forEach { images.remove(it) }
        if (retry > 0) tiles.filter { images[it] == null }.forEach { images.remove(it) }
        val missing = tiles.filterNot { images.containsKey(it) }
        if (missing.isNotEmpty()) statusCallback("Conectando ao mapa…")
        coroutineScope {
            missing.map { tile -> launch { val image = repository.load(tile); ensureActive(); images[tile] = image } }.joinAll()
        }
        statusCallback(if (tiles.any { images[it] != null }) "" else "Sem conexão com o mapa · toque para tentar novamente")
    }
    val pulse = rememberInfiniteTransition(label = "map-pulse")
    val pulseSize by pulse.animateFloat(.95f, 1.07f, infiniteRepeatable(tween(1900),RepeatMode.Reverse), label = "find-pulse")
    val saturation = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(.22f) }) }

    Box(modifier.background(Color(0xFFE2D4B9)).onSizeChanged { viewport = it }
        .pointerInput(Unit) {
            detectTransformGestures { centroid, pan, gestureZoom, _ ->
                val oldScale = 256.0*2.0.pow(zoom.toDouble())*density*.65
                val anchorX = currentCamera.x + (centroid.x-viewport.width*.5)/oldScale
                val anchorY = currentCamera.y + (centroid.y-viewport.height*.58)/oldScale
                following=false
                zoom = (zoom + (ln(gestureZoom.toDouble())/ln(2.0)).toFloat()).coerceIn(3f,19f)
                val newScale = 256.0*2.0.pow(zoom.toDouble())*density*.65
                camera = WorldPoint((anchorX-(centroid.x-viewport.width*.5+pan.x)/newScale+1)%1,
                    (anchorY-(centroid.y-viewport.height*.58+pan.y)/newScale).coerceIn(0.001,.999))
            }
        }
        .pointerInput(Unit) {
            detectTapGestures { tap ->
                val currentScale = 256.0*2.0.pow(zoom.toDouble())*density*.65
                fun position(point: GeoPoint): Offset {
                    val p = project(point)
                    var dx=p.x-currentCamera.x
                    if(dx>.5) dx-=1; if(dx<-.5) dx+=1
                    return Offset((viewport.width*.5+dx*currentScale).toFloat(),(viewport.height*.58+(p.y-currentCamera.y)*currentScale).toFloat())
                }
                val hit = currentMarkers.minByOrNull { (position(it.point)-tap).getDistance() }
                if (hit != null && (position(hit.point)-tap).getDistance() < 38*density) selectCallback(hit)
                else walkCallback?.invoke(unproject(WorldPoint(currentCamera.x+(tap.x-viewport.width*.5)/currentScale,currentCamera.y+(tap.y-viewport.height*.58)/currentScale)))
            }
        }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            for (x in left..right) for (y in top..bottom) {
                val tile = MapTile(integerZoom,((x%tileCount)+tileCount)%tileCount,y)
                images[tile]?.let { bitmap ->
                    val px = centerOnScreen.x+(x-activeCamera.x*tileCount)*tileScale
                    val py = centerOnScreen.y+(y-activeCamera.y*tileCount)*tileScale
                    drawImage(bitmap, dstOffset=IntOffset(floor(px).toInt(),floor(py).toInt()),
                        dstSize=IntSize(ceil(tileScale).toInt()+1,ceil(tileScale).toInt()+1),colorFilter=saturation)
                }
            }
            drawRect(Color(0xFFD7A65A).copy(alpha=.14f))
            if (player != null) {
                val radius = (100.0/(40_075_016.686*cos(Math.toRadians(player.latitude)))*scale).toFloat()
                drawCircle(Amber.copy(alpha=.035f),radius,explorerPosition)
                drawCircle(Amber.copy(alpha=.25f),radius,explorerPosition,style=Stroke(1.2f*density,pathEffect=PathEffect.dashPathEffect(floatArrayOf(7*density,8*density))))
                drawCircle(Amber.copy(alpha=.09f),48*density*pulseSize,explorerPosition)
                drawCircle(Amber.copy(alpha=.3f),29*density,explorerPosition,style=Stroke(1.3f*density))
            }
        }
        markers.forEach { marker ->
            val position = screenPosition(marker.point)
            if (position.x in -90*density..viewport.width+90*density && position.y in -90*density..viewport.height+90*density) {
                Column(Modifier.offset { IntOffset((position.x-32*density).toInt(),(position.y-44*density).toInt()) }.width(64.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Surface(onClick={selectCallback(marker)},shape=CircleShape,color=Parchment,shadowElevation=7.dp,modifier=Modifier.size(50.dp)) {
                        Box(contentAlignment=Alignment.Center) { FossilArt(marker.fossilId,Modifier.size(38.dp),Earth,partId=marker.partId) }
                    }
                    Surface(shape=CircleShape,color=Earth,modifier=Modifier.padding(top=3.dp)) { Text("ACHADO",Modifier.padding(6.dp,3.dp),color=BoneWhite,fontSize=7.sp,letterSpacing=.6.sp) }
                }
            }
        }
        Explorer3D(explorerId, walking, heading,
            Modifier.offset { IntOffset((explorerPosition.x-48*density).toInt(),(explorerPosition.y-104*density).toInt()) }.size(96.dp,132.dp))
    }
}

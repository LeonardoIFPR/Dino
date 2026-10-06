package com.example.dino.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dino.data.Fossil
import com.example.dino.ui.theme.*
import com.google.android.filament.Camera
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberOnGestureListener
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tan

@Composable
fun FossilViewer(fossil: Fossil, modifier: Modifier = Modifier,
                 visibleParts: Set<String>? = null, onPart: (String) -> Unit = {}, previewPart: String? = null,
                 immersive: Boolean = false, resetKey: Int = 0, stageColor: Color = Earth,
                 interactive: Boolean = true, surfaceOnTop: Boolean = true) {
    // A single visible skull must use its own bounds, rather than an empty skeleton's bounds.
    val isolatedPart = previewPart ?: visibleParts?.singleOrNull()
    val path = fossil.parts.firstOrNull { it.id == isolatedPart }?.modelPath ?: fossil.modelPath
    key(path) {
        val resources = rememberModelSceneResources()
        val engine = resources.engine
        val loader = resources.loader
        val asset = rememberModelAsset(loader, path)
        val model = asset.model
        if (model == null) {
            Box(modifier.background(stageColor), contentAlignment = Alignment.Center) {
                if (asset.loading) CircularProgressIndicator(color = Amber)
                else Text("Não foi possível abrir o modelo 3D.", color = if(immersive) Ink else BoneWhite)
            }
        } else {
            val node = remember(model) {
                ModelNode(model, scaleToUnits = if(immersive) 2f else 1.4f).apply {
                    position = -center * scale
                    // Use individual part bounds so a tap identifies a named mesh.
                    collisionShape = null
                    renderableNodes.forEach { it.updateCollisionShape() }
                }
            }
            SideEffect {
                node.renderableNodes.forEach { it.isVisible = visibleParts == null || it.name in visibleParts }
            }
            val camera = rememberCameraNode(engine) { position = Position(0f, 0.4f, 3f); lookAt(Position()) }
            fun frameModel(width: Int, height: Int) {
                if(!immersive || width<=0 || height<=0) return
                val aspect=width.toDouble()/height
                val yaw=-15f
                node.rotation=Rotation(y=yaw)
                val radians=Math.toRadians(yaw.toDouble())
                val halfWidth=(node.size.x*abs(cos(radians))+node.size.z*abs(sin(radians)))/2
                val halfDepth=(node.size.x*abs(sin(radians))+node.size.z*abs(cos(radians)))/2
                val halfHeight=node.size.y/2
                val verticalFov=42.0
                val tangent=tan(Math.toRadians(verticalFov/2))
                val distance=halfDepth+max(halfWidth/(tangent*aspect),halfHeight/tangent)/.80
                camera.position=Position(z=distance.toFloat())
                camera.lookAt(Position())
                camera.setProjection(verticalFov,direction=Camera.Fov.VERTICAL,aspect=aspect)
                resources.view?.cameraManipulator=if(interactive) SceneView.createDefaultCameraManipulator(camera.worldPosition,Position()) else null
            }
            LaunchedEffect(node,resetKey,interactive) {
                resources.view?.let { frameModel(it.width,it.height) }
            }
            val callback by rememberUpdatedState(onPart)
            val gestures = rememberOnGestureListener(onSingleTapConfirmed = { _, hit ->
                hit?.name?.let { name -> if (fossil.parts.any { it.id == name }) callback(name) }
            })
            Box(modifier.background(stageColor)) {
                ModelScene(resources, Modifier.fillMaxSize(), camera, listOf(node), gestures = gestures,
                    onViewportChanged={width,height->frameModel(width,height)},interactive=interactive,surfaceOnTop=surfaceOnTop)
                if(!immersive) Text(if (fossil.museumDisplay && isolatedPart == null) "EXPOSIÇÃO · ESQUELETO E CRÂNIO" else "MODELO ILUSTRATIVO", Modifier.align(Alignment.TopStart).padding(16.dp),
                    style = MaterialTheme.typography.labelSmall, color = BoneWhite)
                if(!immersive) Text("Arraste para girar · use dois dedos para aproximar", Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    style = MaterialTheme.typography.labelSmall, color = BoneWhite)
                if (visibleParts?.isEmpty() == true) Text("Encaixe seu primeiro fragmento", Modifier.align(Alignment.Center), color = BoneWhite)
            }
        }
        ReleaseModelScene(resources)
    }
}

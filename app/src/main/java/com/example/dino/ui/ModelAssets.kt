package com.example.dino.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.sceneview.SceneView
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.Node
import io.github.sceneview.gesture.GestureDetector
import io.github.sceneview.safeDestroy
import io.github.sceneview.utils.destroy
import kotlinx.coroutines.CancellationException

/** Keep the native engine alive until Compose has released its views and nodes. */
class ModelSceneResources(context: Context) {
    private val eglContext = SceneView.createEglContext()
    val engine = SceneView.createEngine(eglContext)
    val loader = SceneView.createModelLoader(engine, context)
    var view: SceneView? = null
    var released = false
        private set

    fun release() {
        if (released) return
        released = true
        Log.d("ModelScene", "Detaching scene before releasing its GLB assets")
        view?.onFrame = null
        view?.destroy()
        view = null
        // SceneView 2.2.1's independent remember effects may destroy the engine
        // before gltfio. Release after the current Compose disposal pass instead.
        Handler(Looper.getMainLooper()).post {
            try {
                loader.destroy()
                Log.d("ModelScene", "GLB assets released")
            }
            finally {
                engine.safeDestroy()
                eglContext.destroy()
            }
        }
    }
}

@Composable
fun rememberModelSceneResources(): ModelSceneResources {
    val context = LocalContext.current
    val resources = remember(context) { ModelSceneResources(context) }
    return resources
}

/** Register after Scene so its frame callback stops before Scene's remember effects. */
@Composable
fun ReleaseModelScene(resources: ModelSceneResources) {
    DisposableEffect(resources) { onDispose { resources.release() } }
}

/** A released SurfaceView cannot be reused with a different native engine. */
@Composable
fun ModelScene(
    resources: ModelSceneResources,
    modifier: Modifier,
    camera: CameraNode,
    nodes: List<Node>,
    interactive: Boolean = true,
    gestures: GestureDetector.OnGestureListener? = null,
    onFrame: ((Long) -> Unit)? = null,
    onViewportChanged: ((Int, Int) -> Unit)? = null,
    surfaceOnTop: Boolean = true
) {
    val sceneLifecycle = LocalLifecycleOwner.current.lifecycle
    val viewportCallback by rememberUpdatedState(onViewportChanged)
    val acceptsTouch by rememberUpdatedState(interactive)
    AndroidView(modifier = modifier, factory = { context ->
        object : SceneView(context, sharedEngine = resources.engine,
            sharedModelLoader = resources.loader, sharedCameraNode = camera,
            sharedLifecycle = sceneLifecycle,
            isOpaque = false,
            cameraManipulator = if (interactive) SceneView.createDefaultCameraManipulator(camera.worldPosition) else null) {
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                if (!resources.released) {
                    lifecycle = sceneLifecycle
                }
            }
            override fun onTouchEvent(event: MotionEvent): Boolean =
                if (acceptsTouch) super.onTouchEvent(event) else false
            override fun onResized(width: Int, height: Int) {
                super.onResized(width, height)
                if (!resources.released) viewportCallback?.invoke(width, height)
            }
            override fun onFrame(frameTimeNanos: Long) {
                if (!isDestroyed && !resources.released) super.onFrame(frameTimeNanos)
            }
            override fun destroy() {
                // Compose can detach and reattach a view during scroll layout.
                // Only final composition disposal may release native resources.
                if (resources.released) {
                    if (!isDestroyed) childNodes = emptyList()
                    super.destroy()
                }
            }
        }.also {
            resources.view = it
            // UiHelper enables Z-on-top for translucent surfaces by default.
            // Excavation needs the surface below Compose's sediment mask.
            it.setZOrderOnTop(surfaceOnTop)
            it.lifecycle = sceneLifecycle
            // Compose can attach the SurfaceView before its final layout pass.
            // Bind the actual holder after layout; UiHelper also initializes an
            // already-created surface, without waiting for an activity resume.
            it.post {
                if (!resources.released) {
                    it.uiHelper.attachTo(it.holder)
                    // A dynamically inserted SurfaceView needs a window draw
                    // to create its surface, even when Compose has no animation.
                    it.requestLayout()
                    it.rootView.invalidate()
                }
            }
        }
    }, update = { view ->
        view.childNodes = nodes
        view.onGestureListener = gestures
        view.onFrame = onFrame
    }, onReset = null, onRelease = { view ->
        resources.release()
    })
}

data class ModelAssetState(val model: ModelInstance? = null, val loading: Boolean = true)

@Composable
fun rememberModelAsset(loader: ModelLoader, path: String): ModelAssetState {
    var state by remember(loader, path) { mutableStateOf(ModelAssetState()) }
    LaunchedEffect(loader, path) {
        try {
            state = ModelAssetState(loader.loadModelInstance(path), false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            state = ModelAssetState(loading = false)
        }
    }
    return state
}

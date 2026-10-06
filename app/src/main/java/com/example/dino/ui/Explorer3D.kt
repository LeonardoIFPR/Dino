package com.example.dino.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.dino.R
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import kotlin.math.abs
import kotlin.math.sin

private class ExplorerMotion {
    var previousFrame = 0L
    var walkTime = 0f
    var idleTime = 0f
    var walkBlend = 0f
}

@Composable
fun ExplorerPortrait(explorerId: String, modifier: Modifier = Modifier) {
    Image(painterResource(if (explorerId == "female") R.drawable.explorer_female else R.drawable.explorer_male),
        contentDescription = if (explorerId == "female") "Exploradora" else "Explorador",
        modifier = modifier, contentScale = ContentScale.Fit)
}

@Composable
fun Explorer3D(explorerId: String, walking: Boolean, heading: Float, modifier: Modifier = Modifier) {
    key(explorerId) {
        val resources = rememberModelSceneResources()
        val engine = resources.engine
        val loader = resources.loader
        val asset = rememberModelAsset(loader, "models/explorer_$explorerId.glb")
        val model = asset.model
        if (model == null) {
            ExplorerPortrait(explorerId, modifier)
        } else {
            // A glTF instance owns its entities. Reuse its wrapper across map recompositions;
            // rememberNode(creator) can dispose a wrapper while the same asset is reused.
            val node = remember(model) {
                ModelNode(model, autoAnimate = false, scaleToUnits = 1.8f, centerOrigin = Position()).apply {
                    isTouchable = false
                    collisionShape = null
                }
            }
            val base = remember(node) { node.position }
            val moving by rememberUpdatedState(walking)
            val direction by rememberUpdatedState(heading)
            val motion = remember(node) { ExplorerMotion() }
            val walkIndex = remember(node) { (0 until node.animationCount).firstOrNull { node.animator.getAnimationName(it) == "Walk" } }
            val idleIndex = remember(node) { (0 until node.animationCount).firstOrNull { node.animator.getAnimationName(it) == "Idle" } }
            val camera = rememberCameraNode(engine) {
                position = Position(0f, .5f, 3.5f)
                lookAt(Position(0f, -.05f, 0f))
            }
            Box(modifier) {
                ModelScene(resources, Modifier.fillMaxSize(), camera, listOf(node),
                    interactive = false, onFrame = { frame ->
                        val dt = if (motion.previousFrame == 0L) .016f else ((frame - motion.previousFrame) / 1_000_000_000f).coerceIn(0f, .1f)
                        motion.previousFrame = frame
                        val target = if (moving) 1f else 0f
                        val step = dt / .18f
                        motion.walkBlend += (target - motion.walkBlend).coerceIn(-step, step)
                        motion.idleTime += dt
                        if (moving || motion.walkBlend > 0f) motion.walkTime += dt
                        if (walkIndex != null && idleIndex != null) {
                            // Blend into the stride and back to standing without snapping poses.
                            val walkTime = motion.walkTime % node.animator.getAnimationDuration(walkIndex).coerceAtLeast(.001f)
                            val idleTime = motion.idleTime % node.animator.getAnimationDuration(idleIndex).coerceAtLeast(.001f)
                            node.animator.applyAnimation(walkIndex, walkTime)
                            val blend = motion.walkBlend * motion.walkBlend * (3f - 2f * motion.walkBlend)
                            node.animator.applyCrossFade(idleIndex, idleTime, blend)
                            node.animator.updateBoneMatrices()
                        } else {
                            val index = if (moving) walkIndex ?: idleIndex else idleIndex ?: walkIndex
                            if (index != null) {
                                node.animator.applyAnimation(index, motion.idleTime % node.animator.getAnimationDuration(index).coerceAtLeast(.001f))
                                node.animator.updateBoneMatrices()
                            }
                        }
                        val difference = ((direction - node.rotation.y + 540f) % 360f) - 180f
                        node.rotation = Rotation(y = node.rotation.y + difference * (dt * 12f).coerceAtMost(1f))
                        val bob = if (moving && node.animationCount == 0) abs(sin(frame / 1_000_000_000.0 * 10)).toFloat() * .025f else 0f
                        node.position = base + Position(y = bob)
                    })
            }
        }
        ReleaseModelScene(resources)
    }
}

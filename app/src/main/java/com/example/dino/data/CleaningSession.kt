package com.example.dino.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import kotlin.math.hypot
import kotlin.math.max

enum class CleaningTool { BRUSH, PICK }

/** Sediment and damage belong to the attempt, not to the permanent collection. */
class CleaningSession {
    val columns = 28
    val rows = 28
    val sediment = mutableStateListOf<Float>().apply { repeat(columns * rows) { add(1f) } }
    var integrity by mutableFloatStateOf(100f)
        private set
    var strokes by mutableIntStateOf(0)
        private set
    private fun protected(x: Float, y: Float): Boolean =
        ((x - .5f) / .36f).let { it * it } + ((y - .5f) / .36f).let { it * it } <= 1f
    val progress: Float get() {
        var total = 0f; var count = 0
        sediment.forEachIndexed { index, level ->
            if (protected((index % columns + .5f) / columns, (index / columns + .5f) / rows)) {
                total += 1f - level; count++
            }
        }
        return if (count == 0) 0f else total / count
    }
    val broken get() = integrity <= 0f
    val recovered get() = !broken && progress >= .93f

    fun brush(x: Float, y: Float, radius: Float, pressure: Float, tool: CleaningTool) {
        if (broken || recovered) return
        var exposedHits = 0
        var worked = false
        sediment.indices.forEach { index ->
            val cx = (index % columns + .5f) / columns
            val cy = (index / columns + .5f) / rows
            val d = hypot(cx - x, cy - y)
            if (d <= radius) {
                val influence = (1f - d / radius).coerceAtLeast(.15f)
                val old = sediment[index]
                val removal = (if (tool == CleaningTool.BRUSH) .23f else .57f) * (.5f + pressure) * influence
                sediment[index] = max(0f, old - removal)
                if (old > 0f) worked = true
                if (old < .16f && protected(cx, cy) && (tool == CleaningTool.PICK || pressure > .78f)) exposedHits++
            }
        }
        if (exposedHits > 0) {
            integrity = (integrity - exposedHits * (if (tool == CleaningTool.PICK) .65f else .3f) * pressure).coerceAtLeast(0f)
        }
        if (worked || exposedHits > 0) strokes++
    }

    fun restart() { sediment.indices.forEach { sediment[it] = 1f }; integrity = 100f; strokes = 0 }

    companion object {
        val Saver = listSaver<CleaningSession, Float>(
            save = { listOf(it.integrity, it.strokes.toFloat()) + it.sediment.toList() },
            restore = { values -> CleaningSession().apply {
                integrity = values[0]; strokes = values[1].toInt()
                sediment.indices.forEach { sediment[it] = values[it + 2] }
            } }
        )
    }
}

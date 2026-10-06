package com.example.dino.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.dino.ui.theme.*
import kotlin.math.sin

@Composable
fun ExplorerAvatar(modifier: Modifier = Modifier, walking: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "explorer")
    val phase by transition.animateFloat(0f, 6.283f, infiniteRepeatable(tween(650, easing = LinearEasing)), label = "step")
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val step = if (walking) sin(phase) else 0f
        val bob = if (walking) kotlin.math.abs(step) * h * .025f else 0f
        drawOval(Color(0xFF483022).copy(alpha = .24f), Offset(w*.17f,h*.86f), Size(w*.66f,h*.1f))
        drawLine(Earth,Offset(w*.42f,h*.61f-bob),Offset(w*(.38f+step*.055f),h*.89f),w*.11f,StrokeCap.Round)
        drawLine(Earth,Offset(w*.59f,h*.61f-bob),Offset(w*(.63f-step*.055f),h*.89f),w*.11f,StrokeCap.Round)
        drawLine(Color(0xFF2E241D),Offset(w*(.35f+step*.055f),h*.89f),Offset(w*(.46f+step*.055f),h*.89f),w*.09f,StrokeCap.Round)
        drawLine(Color(0xFF2E241D),Offset(w*(.61f-step*.055f),h*.89f),Offset(w*(.72f-step*.055f),h*.89f),w*.09f,StrokeCap.Round)
        drawRoundRect(Color(0xFF79503A),Offset(w*.27f,h*.4f-bob),Size(w*.45f,h*.26f),androidx.compose.ui.geometry.CornerRadius(w*.09f))
        drawRoundRect(Amber,Offset(w*.31f,h*.39f-bob),Size(w*.39f,h*.26f),androidx.compose.ui.geometry.CornerRadius(w*.1f))
        drawLine(Color(0xFFC58B62),Offset(w*.3f,h*.45f-bob),Offset(w*(.2f-step*.055f),h*.65f-bob),w*.09f,StrokeCap.Round)
        drawLine(Color(0xFFC58B62),Offset(w*.7f,h*.45f-bob),Offset(w*(.8f+step*.055f),h*.65f-bob),w*.09f,StrokeCap.Round)
        drawLine(Earth,Offset(w*.4f,h*.39f-bob),Offset(w*.44f,h*.62f-bob),w*.045f)
        drawLine(Earth,Offset(w*.6f,h*.39f-bob),Offset(w*.56f,h*.62f-bob),w*.045f)
        drawCircle(Color(0xFFD8AA7C),w*.17f,Offset(w*.5f,h*.29f-bob))
        drawCircle(Earth,w*.018f,Offset(w*.45f,h*.3f-bob)); drawCircle(Earth,w*.018f,Offset(w*.56f,h*.3f-bob))
        drawOval(Earth,Offset(w*.14f,h*.185f-bob),Size(w*.72f,h*.12f))
        drawRoundRect(Color(0xFFD6AE6B),Offset(w*.29f,h*.08f-bob),Size(w*.42f,h*.17f),androidx.compose.ui.geometry.CornerRadius(w*.08f))
        drawRoundRect(Earth,Offset(w*.29f,h*.195f-bob),Size(w*.42f,h*.045f),androidx.compose.ui.geometry.CornerRadius(w*.015f))
    }
}

@Composable
fun NavigationGlyph(index: Int, modifier: Modifier = Modifier, color: Color = Earth) {
    Canvas(modifier) {
        val w = size.width; val h = size.height; val stroke = w*.065f
        when (index) {
            0 -> {
                drawCircle(color,w*.35f,Offset(w*.5f,h*.5f),style=androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                val p=Path().apply { moveTo(w*.64f,h*.25f); lineTo(w*.56f,h*.57f); lineTo(w*.28f,h*.72f); lineTo(w*.42f,h*.41f); close() }
                drawPath(p,color)
            }
            1 -> {
                for (x in 0..1) for (y in 0..1) drawRoundRect(color,Offset(w*(.16f+x*.4f),h*(.15f+y*.4f)),Size(w*.28f,h*.28f),androidx.compose.ui.geometry.CornerRadius(w*.045f),style=androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            }
            2 -> {
                drawLine(color,Offset(w*.28f,h*.76f),Offset(w*.72f,h*.28f),stroke*1.5f,StrokeCap.Round)
                drawLine(color,Offset(w*.46f,h*.2f),Offset(w*.85f,h*.43f),stroke*1.4f,StrokeCap.Round)
                drawLine(color,Offset(w*.2f,h*.25f),Offset(w*.72f,h*.78f),stroke,StrokeCap.Round)
                drawLine(color,Offset(w*.16f,h*.32f),Offset(w*.28f,h*.17f),stroke*1.4f,StrokeCap.Round)
            }
            else -> {
                drawRoundRect(color,Offset(w*.22f,h*.15f),Size(w*.57f,h*.72f),androidx.compose.ui.geometry.CornerRadius(w*.07f),style=androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                for (y in 0..2) drawLine(color,Offset(w*.37f,h*(.33f+y*.16f)),Offset(w*.66f,h*(.33f+y*.16f)),stroke*.8f,StrokeCap.Round)
                drawLine(color,Offset(w*.14f,h*.3f),Offset(w*.27f,h*.3f),stroke,StrokeCap.Round)
                drawLine(color,Offset(w*.14f,h*.65f),Offset(w*.27f,h*.65f),stroke,StrokeCap.Round)
            }
        }
    }
}

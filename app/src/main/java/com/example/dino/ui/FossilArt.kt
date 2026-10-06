package com.example.dino.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.dino.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.dino.ui.theme.Clay
import com.example.dino.ui.theme.Forest
import kotlin.math.*

@Composable
fun FossilArt(id: String, modifier: Modifier = Modifier, color: Color = Forest, partId: String? = null,
              colorFilter: ColorFilter? = null) {
    val thumbnail = when (id) {
        "triceratops" -> if (partId == "head") R.drawable.triceratops_head else R.drawable.triceratops_body
        "trex" -> if (partId == "head") R.drawable.trex_head else R.drawable.trex_body
        "carnotaurus" -> if (partId == "head") R.drawable.carnotaurus_head else R.drawable.carnotaurus_body
        else -> null
    }
    if (thumbnail != null) {
        Image(painterResource(thumbnail),contentDescription=null,modifier=modifier,contentScale=ContentScale.Fit,colorFilter=colorFilter)
        return
    }
    Canvas(modifier) {
        val w = size.width; val h = size.height
        when (id) {
            "ammonite" -> {
                val spiral = Path()
                for (i in 0..230) {
                    val t = i / 230f * PI.toFloat() * 5.3f
                    val r = .018f + i / 230f * .37f
                    val x = w * (.5f + cos(t) * r); val y = h * (.5f + sin(t) * r)
                    if (i == 0) spiral.moveTo(x, y) else spiral.lineTo(x, y)
                }
                drawPath(spiral, color.copy(alpha = .2f), style = Stroke(w * .14f, cap = StrokeCap.Round))
                drawPath(spiral, color, style = Stroke(w * .055f, cap = StrokeCap.Round))
                for (i in 12..34) {
                    val t = i / 34f * PI.toFloat() * 5.3f
                    val r = .018f + i / 34f * .37f
                    drawLine(color.copy(alpha = .6f), Offset(w*(.5f+cos(t)*(r-.025f)), h*(.5f+sin(t)*(r-.025f))),
                        Offset(w*(.5f+cos(t)*(r+.025f)), h*(.5f+sin(t)*(r+.025f))), w*.018f)
                }
            }
            "trilobite" -> {
                drawOval(color.copy(alpha=.16f), Offset(w*.22f,h*.14f), androidx.compose.ui.geometry.Size(w*.56f,h*.72f))
                drawOval(color, Offset(w*.32f,h*.12f), androidx.compose.ui.geometry.Size(w*.36f,h*.25f), style=Stroke(w*.035f))
                for (i in 0..6) {
                    val y = h*(.39f+i*.065f); val half = w*(.25f-i*.018f)
                    drawLine(color,Offset(w*.5f-half,y),Offset(w*.5f+half,y),w*.025f,StrokeCap.Round)
                }
                drawLine(color,Offset(w*.5f,h*.28f),Offset(w*.5f,h*.83f),w*.095f,StrokeCap.Round)
                drawCircle(color,w*.022f,Offset(w*.4f,h*.22f));drawCircle(color,w*.022f,Offset(w*.6f,h*.22f))
            }
            "tooth" -> {
                val p=Path().apply { moveTo(w*.3f,h*.82f); lineTo(w*.7f,h*.82f); cubicTo(w*.62f,h*.56f,w*.4f,h*.35f,w*.54f,h*.12f); cubicTo(w*.29f,h*.32f,w*.24f,h*.61f,w*.3f,h*.82f); close() }
                drawPath(p,Clay.copy(alpha=.18f));drawPath(p,color,style=Stroke(w*.04f))
                drawLine(color.copy(alpha=.5f),Offset(w*.4f,h*.74f),Offset(w*.45f,h*.35f),w*.02f)
            }
            else -> {
                drawLine(color.copy(alpha=.2f),Offset(w*.3f,h*.72f),Offset(w*.7f,h*.28f),w*.24f,StrokeCap.Round)
                drawLine(color,Offset(w*.3f,h*.72f),Offset(w*.7f,h*.28f),w*.11f,StrokeCap.Round)
                listOf(.25f to .64f,.36f to .77f,.64f to .23f,.76f to .36f).forEach { (x,y) ->
                    drawCircle(color,w*.095f,Offset(w*x,h*y))
                }
            }
        }
    }
}

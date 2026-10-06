package com.example.dino.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.dino.ui.theme.Earth

@Composable
internal fun CameraGlyph(modifier:Modifier=Modifier.size(22.dp),color:Color=Earth) {
    Canvas(modifier) {
        val stroke=Stroke(1.7.dp.toPx())
        drawRoundRect(color,Offset(size.width*.08f,size.height*.28f),Size(size.width*.84f,size.height*.59f),CornerRadius(3.dp.toPx()),style=stroke)
        drawCircle(color,size.width*.17f,Offset(size.width*.5f,size.height*.57f),style=stroke)
        val cap=Path().apply {moveTo(size.width*.3f,size.height*.28f);lineTo(size.width*.38f,size.height*.13f);lineTo(size.width*.62f,size.height*.13f);lineTo(size.width*.7f,size.height*.28f)}
        drawPath(cap,color,style=stroke)
    }
}

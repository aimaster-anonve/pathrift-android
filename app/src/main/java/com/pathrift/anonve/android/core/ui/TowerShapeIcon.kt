package com.pathrift.anonve.android.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pathrift.anonve.android.game.towers.TowerType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TowerShapeIcon(type: TowerType, color: Color, modifier: Modifier = Modifier, sizeDp: Dp = 36.dp) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val w = size.width
        val h = size.height

        when (type) {
            TowerType.BOLT -> {
                val path = Path().apply {
                    moveTo(cx + 3, 4f)
                    lineTo(cx - 3, h * 0.46f)
                    lineTo(cx + 2, h * 0.46f)
                    lineTo(cx - 3, h - 4)
                }
                drawPath(path, color, style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            TowerType.BLAST -> {
                val path = Path()
                val r = w * 0.40f
                for (i in 0 until 8) {
                    val angle = (i * Math.PI / 4 - Math.PI / 8).toFloat()
                    val px = cx + r * cos(angle.toDouble()).toFloat()
                    val py = cy + r * sin(angle.toDouble()).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, color.copy(alpha = 0.9f))
                drawCircle(Color.White.copy(alpha = 0.6f), radius = 5f, center = Offset(cx, cy))
            }
            TowerType.FROST -> {
                val r = w * 0.42f
                for (i in 0 until 6) {
                    val angle = (i * Math.PI / 3).toFloat()
                    drawLine(color,
                        Offset(cx, cy),
                        Offset(cx + r * cos(angle.toDouble()).toFloat(), cy + r * sin(angle.toDouble()).toFloat()),
                        strokeWidth = 4.5f, cap = StrokeCap.Round)
                }
                drawCircle(color, radius = 4f, center = Offset(cx, cy))
            }
            TowerType.PIERCE -> {
                val path = Path().apply {
                    moveTo(w - 4, cy)
                    lineTo(4f, cy - h * 0.28f)
                    lineTo(4 + w * 0.15f, cy)
                    lineTo(4f, cy + h * 0.28f)
                    close()
                }
                drawPath(path, color.copy(alpha = 0.9f))
            }
            TowerType.CORE -> {
                drawRoundRect(color, topLeft = Offset(5f, 5f), size = Size(w - 10, h - 10),
                    cornerRadius = CornerRadius(3f), style = Stroke(width = 5f))
                drawCircle(color, radius = 5f, center = Offset(cx, cy))
            }
            TowerType.INFERNO -> {
                listOf(Triple(1.0f, 0.9f, 0), Triple(0.68f, 0.55f, 1), Triple(0.38f, 0.3f, 2)).forEach { (scale, alpha, _) ->
                    val fh = h * 0.38f * scale
                    val fw = w * 0.27f * scale
                    val flame = Path().apply {
                        moveTo(cx, cy + fh)
                        cubicTo(cx + fw, cy + fh * 0.2f, cx + fw * 0.5f, cy - fh * 0.3f, cx, cy - fh * 0.6f)
                        cubicTo(cx - fw * 0.5f, cy - fh * 0.3f, cx - fw, cy + fh * 0.2f, cx, cy + fh)
                        close()
                    }
                    drawPath(flame, color.copy(alpha = alpha))
                }
            }
            TowerType.TESLA -> {
                drawCircle(color.copy(alpha = 0.4f), radius = w / 2 - 6,
                    center = Offset(cx, cy), style = Stroke(width = 3f))
                val bolt = Path().apply {
                    moveTo(cx + 3, 5f); lineTo(cx - 2, cy); lineTo(cx + 2, cy); lineTo(cx - 3, h - 5)
                }
                drawPath(bolt, color, style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            TowerType.NOVA -> {
                val r1 = w * 0.43f; val r2 = w * 0.17f
                val star = Path()
                for (i in 0 until 8) {
                    val angle = (i * Math.PI / 4 - Math.PI / 2).toFloat()
                    val r = if (i % 2 == 0) r1 else r2
                    val px = cx + r * cos(angle.toDouble()).toFloat()
                    val py = cy + r * sin(angle.toDouble()).toFloat()
                    if (i == 0) star.moveTo(px, py) else star.lineTo(px, py)
                }
                star.close()
                drawPath(star, color.copy(alpha = 0.9f))
            }
            TowerType.SNIPER -> {
                drawRoundRect(color.copy(alpha = 0.85f),
                    topLeft = Offset(7f, cy - 5f), size = Size(w - 12, 10f), cornerRadius = CornerRadius(3f))
                val oval = Path().apply {
                    val rect = Rect(Offset(3f, cy - 11f), Size(22f, 22f))
                    addOval(rect)
                }
                drawPath(oval, color, style = Stroke(width = 3f))
                drawLine(color.copy(alpha = 0.5f), Offset(3f, cy), Offset(25f, cy), strokeWidth = 1.5f)
                drawLine(color.copy(alpha = 0.5f), Offset(14f, cy - 11f), Offset(14f, cy + 11f), strokeWidth = 1.5f)
            }
            TowerType.ARTILLERY -> {
                drawCircle(color.copy(alpha = 0.8f), radius = 20f, center = Offset(cx - 4, cy))
                drawRoundRect(color, topLeft = Offset(cx, cy - 5), size = Size(w * 0.33f, 10f), cornerRadius = CornerRadius(4f))
            }
        }
    }
}

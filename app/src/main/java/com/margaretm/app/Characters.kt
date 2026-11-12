package com.margaretm.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// =====================================================================================
//  Characters.kt  -  the app's own cast of friendly cartoon animals, drawn in code (no image
//  files).  They replace icons: navigation, menu tiles, headers and result screens.
//  All eight are original designs on a 100 x 100 grid: head and shoulders, flat colours.
// =====================================================================================
const val CRITTERS = 8
val critterNames = listOf("Fox", "Owl", "Bunny", "Panda", "Lion", "Robot", "Frog", "Penguin")

private val EYE = Color(0xFF1B2733)
private val OUTLINE = Color(0xFFC3D2CA)

private fun DrawScope.o(u: Float, cx: Float, cy: Float, rx: Float, ry: Float, c: Color, ol: Boolean = false) {
    drawOval(c, Offset((cx - rx) * u, (cy - ry) * u), Size(2 * rx * u, 2 * ry * u))
    if (ol) drawOval(OUTLINE, Offset((cx - rx) * u, (cy - ry) * u), Size(2 * rx * u, 2 * ry * u), style = Stroke(1.6f * u))
}
private fun DrawScope.tri(u: Float, c: Color, vararg p: Float) {
    val path = Path(); path.moveTo(p[0] * u, p[1] * u)
    var i = 2; while (i < p.size) { path.lineTo(p[i] * u, p[i + 1] * u); i += 2 }
    path.close(); drawPath(path, c)
}
private fun DrawScope.eyes(u: Float, x1: Float, x2: Float, y: Float, r: Float, look: Float = 0f) {
    for (x in floatArrayOf(x1, x2)) {
        o(u, x, y, r, r * 1.1f, Color.White)
        o(u, x + look, y + .6f, r * .62f, r * .72f, EYE)
        drawCircle(Color.White, r * .22f * u, Offset((x + look - r * .2f) * u, (y - r * .25f) * u))
    }
}
private fun DrawScope.blush(u: Float, x1: Float, x2: Float, y: Float) {
    o(u, x1, y, 6f, 3.8f, Color(0xFFFF8FA3).copy(alpha = .55f)); o(u, x2, y, 6f, 3.8f, Color(0xFFFF8FA3).copy(alpha = .55f))
}
private fun DrawScope.smile(u: Float, cx: Float, cy: Float, w: Float, sweep: Float = 140f) {
    drawArc(EYE, 20f + (140f - sweep) / 2f, sweep, false, Offset((cx - w / 2) * u, (cy - w / 3) * u), Size(w * u, w * .66f * u), style = Stroke(2.4f * u, cap = StrokeCap.Round))
}

private fun DrawScope.drawCritter(kind: Int) {
    val u = size.minDimension / 100f
    when (((kind % CRITTERS) + CRITTERS) % CRITTERS) {
        0 -> { // FOX
            val or = Color(0xFFF08A3C)
            o(u, 50f, 102f, 30f, 28f, or); o(u, 50f, 106f, 15f, 18f, Color.White)
            tri(u, or, 15f, 6f, 13f, 46f, 45f, 28f); tri(u, or, 85f, 6f, 87f, 46f, 55f, 28f)
            tri(u, Color(0xFFFFC9A0), 21f, 18f, 21f, 38f, 37f, 29f); tri(u, Color(0xFFFFC9A0), 79f, 18f, 79f, 38f, 63f, 29f)
            o(u, 50f, 56f, 38f, 31f, or); o(u, 30f, 66f, 19f, 13f, Color.White); o(u, 70f, 66f, 19f, 13f, Color.White)
            eyes(u, 36f, 64f, 52f, 6f); o(u, 50f, 60f, 5.5f, 4f, EYE); smile(u, 50f, 67f, 11f); blush(u, 27f, 73f, 62f)
        }
        1 -> { // OWL
            val br = Color(0xFF8D6E63)
            o(u, 50f, 100f, 30f, 28f, br); o(u, 50f, 106f, 18f, 20f, Color(0xFFE8D5C4))
            tri(u, br, 17f, 12f, 20f, 36f, 42f, 22f); tri(u, br, 83f, 12f, 80f, 36f, 58f, 22f)
            o(u, 50f, 52f, 37f, 34f, br); o(u, 35f, 52f, 17f, 17f, Color(0xFFF3E6D8)); o(u, 65f, 52f, 17f, 17f, Color(0xFFF3E6D8))
            eyes(u, 35f, 65f, 52f, 9.5f); tri(u, Color(0xFFFFA000), 44f, 60f, 56f, 60f, 50f, 72f)
        }
        2 -> { // BUNNY
            val w = Color(0xFFFBE9F0)
            o(u, 34f, 18f, 9f, 24f, w, true); o(u, 66f, 18f, 9f, 24f, w, true)
            o(u, 34f, 20f, 4.5f, 17f, Color(0xFFFFB6C8)); o(u, 66f, 20f, 4.5f, 17f, Color(0xFFFFB6C8))
            o(u, 50f, 102f, 28f, 26f, w, true); o(u, 50f, 60f, 35f, 30f, w, true)
            eyes(u, 37f, 63f, 57f, 5.5f); o(u, 50f, 65f, 4.5f, 3.2f, Color(0xFFFF8FA3)); smile(u, 50f, 70f, 10f); blush(u, 27f, 73f, 68f)
        }
        3 -> { // PANDA
            val bk = Color(0xFF2B3440)
            o(u, 50f, 102f, 30f, 28f, Color.White, true); o(u, 22f, 96f, 9f, 18f, bk); o(u, 78f, 96f, 9f, 18f, bk)
            drawCircle(bk, 11f * u, Offset(22f * u, 24f * u)); drawCircle(bk, 11f * u, Offset(78f * u, 24f * u))
            o(u, 50f, 56f, 37f, 31f, Color.White, true)
            o(u, 35f, 54f, 9f, 11.5f, bk); o(u, 65f, 54f, 9f, 11.5f, bk)
            for (x in floatArrayOf(35f, 65f)) { drawCircle(Color.White, 3.6f * u, Offset(x * u, 52f * u)); drawCircle(EYE, 1.9f * u, Offset(x * u, 52.4f * u)) }
            o(u, 50f, 64f, 5.5f, 3.8f, bk); smile(u, 50f, 69f, 11f)
        }
        4 -> { // LION
            val ma = Color(0xFFC96F1A); val fc = Color(0xFFF6C26B)
            o(u, 50f, 102f, 26f, 24f, fc)
            for (i in 0 until 12) { val a = Math.toRadians(i * 30.0); drawCircle(ma, 12f * u, Offset((50f + 34f * Math.cos(a).toFloat()) * u, (52f + 31f * Math.sin(a).toFloat()) * u)) }
            o(u, 50f, 52f, 36f, 33f, Color(0xFFD9822B))
            drawCircle(fc, 8f * u, Offset(29f * u, 30f * u)); drawCircle(fc, 8f * u, Offset(71f * u, 30f * u))
            o(u, 50f, 56f, 27f, 26f, fc); o(u, 50f, 67f, 15f, 11f, Color(0xFFFFE6B8))
            eyes(u, 39f, 61f, 52f, 5f); tri(u, Color(0xFF6B3A1A), 45f, 60f, 55f, 60f, 50f, 66f); smile(u, 50f, 70f, 11f)
        }
        5 -> { // ROBOT
            val gr = Color(0xFFB8C7D6)
            drawLine(Color(0xFF8CA0B3), Offset(50f * u, 8f * u), Offset(50f * u, 20f * u), 3f * u, StrokeCap.Round)
            drawCircle(Color(0xFFFF7A66), 5f * u, Offset(50f * u, 7f * u))
            drawRoundRect(Color(0xFF9FB2C4), Offset(26f * u, 78f * u), Size(48f * u, 40f * u), CornerRadius(12f * u))
            drawCircle(Color(0xFFFFC857), 5f * u, Offset(50f * u, 92f * u))
            drawCircle(Color(0xFF8CA0B3), 7f * u, Offset(11f * u, 48f * u)); drawCircle(Color(0xFF8CA0B3), 7f * u, Offset(89f * u, 48f * u))
            drawRoundRect(gr, Offset(14f * u, 20f * u), Size(72f * u, 60f * u), CornerRadius(16f * u))
            drawRoundRect(Color(0xFF22313F), Offset(22f * u, 33f * u), Size(56f * u, 28f * u), CornerRadius(13f * u))
            drawCircle(Color(0xFF5CE1E6), 6.5f * u, Offset(38f * u, 47f * u)); drawCircle(Color(0xFF5CE1E6), 6.5f * u, Offset(62f * u, 47f * u))
            drawCircle(Color.White, 2f * u, Offset(36f * u, 45f * u)); drawCircle(Color.White, 2f * u, Offset(60f * u, 45f * u))
            smile(u, 50f, 69f, 14f, 120f)
        }
        6 -> { // FROG
            val gn = Color(0xFF5DBB63)
            o(u, 50f, 102f, 30f, 26f, gn); o(u, 50f, 106f, 18f, 18f, Color(0xFFD7F2C0))
            o(u, 50f, 60f, 39f, 28f, gn); o(u, 29f, 32f, 14f, 14f, gn); o(u, 71f, 32f, 14f, 14f, gn)
            eyes(u, 29f, 71f, 32f, 8f); drawCircle(Color(0xFF3E9447), 1.6f * u, Offset(45f * u, 54f * u)); drawCircle(Color(0xFF3E9447), 1.6f * u, Offset(55f * u, 54f * u))
            smile(u, 50f, 65f, 34f, 150f); blush(u, 24f, 76f, 64f)
        }
        else -> { // PENGUIN
            val nv = Color(0xFF2F3E52)
            o(u, 50f, 92f, 32f, 34f, nv); o(u, 50f, 98f, 21f, 26f, Color.White); o(u, 17f, 90f, 8f, 20f, nv); o(u, 83f, 90f, 8f, 20f, nv)
            o(u, 50f, 50f, 33f, 31f, nv); o(u, 39f, 55f, 14f, 16f, Color.White); o(u, 61f, 55f, 14f, 16f, Color.White)
            eyes(u, 39f, 61f, 50f, 5.5f); tri(u, Color(0xFFFFA726), 43f, 59f, 57f, 59f, 50f, 68f); blush(u, 29f, 71f, 63f)
        }
    }
}

@Composable fun Critter(kind: Int, size: Dp, modifier: Modifier = Modifier) = Canvas(modifier.size(size)) { drawCritter(kind) }

/** A critter that bobs gently, like the characters standing on the edge of the white sheet. */
@Composable fun CritterBounce(kind: Int, size: Dp, modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "cb").animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "t")
    Critter(kind, size, modifier.offset(y = (-5 * t).dp))
}

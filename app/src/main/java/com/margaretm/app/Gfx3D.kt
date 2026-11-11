package com.margaretm.app

import android.graphics.Matrix as AMatrix
import android.graphics.Paint as APaint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// =====================================================================================
//  Gfx3D.kt  -  draws the pure 3D toolkit (World3D.kt) onto a Compose Canvas.
//  Everything here is plain DrawScope code: no OpenGL, no extra dependencies.
// =====================================================================================

private val txt = APaint(APaint.ANTI_ALIAS_FLAG).apply { textAlign = APaint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
private val facePaint = APaint(APaint.ANTI_ALIAS_FLAG).apply { textAlign = APaint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
private val faceMatrix = AMatrix()
private val faceSrc = floatArrayOf(0f, 100f, 100f, 100f, 100f, 0f, 0f, 0f)   // unit "texture" square: BL, BR, TR, TL
private val faceDst = FloatArray(8)
private val meshPath = Path()

/** Centred text in screen space (letters and emoji). */
fun DrawScope.text2d(s: String, x: Float, y: Float, size: Float, color: Color = Color.White, shadow: Boolean = true) {
    txt.textSize = size; txt.color = color.toArgb()
    if (shadow) txt.setShadowLayer(size * .08f, 0f, size * .05f, 0x88000000.toInt()) else txt.clearShadowLayer()
    drawContext.canvas.nativeCanvas.drawText(s, x, y + size * .35f, txt)
}

/**
 * Draws a "decal" on a projected face. [q] is the oriented quad returned by [drawMesh];
 * [body] draws in a 100 x 100 face space, so the text is truly perspective-mapped onto the cube.
 */
fun DrawScope.onFace(q: Array<Offset>?, body: (android.graphics.Canvas) -> Unit) {
    if (q == null || q.size != 4) return
    for (i in 0 until 4) { faceDst[i * 2] = q[i].x; faceDst[i * 2 + 1] = q[i].y }
    if (!faceMatrix.setPolyToPoly(faceSrc, 0, faceDst, 0, 4)) return
    val c = drawContext.canvas.nativeCanvas
    c.save(); c.concat(faceMatrix); body(c); c.restore()
}

fun faceText(c: android.graphics.Canvas, s: String, x: Float, y: Float, size: Float, color: Color = Color.White) {
    facePaint.textSize = size; facePaint.color = color.toArgb()
    facePaint.setShadowLayer(size * .05f, 0f, size * .04f, 0x66000000)
    c.drawText(s, x, y + size * .35f, facePaint)
}

/**
 * Draws a convex mesh with back-face culling and per-face lighting.
 * Returns, for every face, its projected quad (oriented for [onFace]) or null when the face is hidden.
 */
fun DrawScope.drawMesh(cam: Cam, p: Placed, base: Color, alpha: Float = 1f, edge: Color? = null, tint: ((Int) -> Color)? = null): Array<Array<Offset>?> {
    val m = p.m; val out = arrayOfNulls<Array<Offset>>(m.f.size)
    for (i in m.f.indices) {
        val idx = m.f[i]
        var cx = 0f; var cy = 0f; var cz = 0f; var ok = true
        for (k in idx) { cx += p.w[k][0]; cy += p.w[k][1]; cz += p.w[k][2]; if (!cam.inFront(p.w[k][2])) ok = false }
        if (!ok) continue
        val c = idx.size.toFloat(); cx /= c; cy /= c; cz /= c
        if (!cam.facing(p.n[i], cx, cy, cz)) continue
        val pts = Array(idx.size) { Offset(cam.sx(p.w[idx[it]][0], p.w[idx[it]][2]), cam.sy(p.w[idx[it]][1], p.w[idx[it]][2])) }
        val sh = shade(p.n[i]); val c0 = tint?.invoke(i) ?: base
        meshPath.reset(); meshPath.moveTo(pts[0].x, pts[0].y)
        for (j in 1 until pts.size) meshPath.lineTo(pts[j].x, pts[j].y)
        meshPath.close()
        drawPath(meshPath, Color(c0.red * sh, c0.green * sh, c0.blue * sh, alpha))
        if (edge != null) drawPath(meshPath, edge.copy(alpha = alpha * 0.55f), style = Stroke(width = 2f))
        var area = 0f
        for (j in pts.indices) { val a = pts[j]; val b = pts[(j + 1) % pts.size]; area += a.x * b.y - b.x * a.y }
        out[i] = if (pts.size == 4 && area > 0f) arrayOf(pts[0], pts[3], pts[2], pts[1]) else pts
    }
    return out
}

/** Quad from 4 world points (12 floats: x,y,z x 4). */
private fun DrawScope.quad3(cam: Cam, color: Color, a: FloatArray) {
    for (i in 0 until 4) if (!cam.inFront(a[i * 3 + 2])) return
    meshPath.reset()
    for (i in 0 until 4) { val x = cam.sx(a[i * 3], a[i * 3 + 2]); val y = cam.sy(a[i * 3 + 1], a[i * 3 + 2]); if (i == 0) meshPath.moveTo(x, y) else meshPath.lineTo(x, y) }
    meshPath.close(); drawPath(meshPath, color)
}
private val q12 = FloatArray(12)
private fun DrawScope.ground(cam: Cam, color: Color, x0: Float, x1: Float, za: Float, zb: Float, y: Float = 0f) {
    q12[0] = x0; q12[1] = y; q12[2] = za; q12[3] = x1; q12[4] = y; q12[5] = za; q12[6] = x1; q12[7] = y; q12[8] = zb; q12[9] = x0; q12[10] = y; q12[11] = zb
    quad3(cam, color, q12)
}
private fun DrawScope.wall(cam: Cam, color: Color, x: Float, za: Float, zb: Float, h: Float) {
    q12[0] = x; q12[1] = 0f; q12[2] = za; q12[3] = x; q12[4] = 0f; q12[5] = zb; q12[6] = x; q12[7] = h; q12[8] = zb; q12[9] = x; q12[10] = h; q12[11] = za
    quad3(cam, color, q12)
}

fun fog(z: Float) = ((34f - z) / 18f).coerceIn(0f, 1f)
private fun hash(i: Int): Float { val s = sin(i * 12.9898 + 4.1414) * 43758.5453; return abs(s - floor(s)).toFloat() }
private val laneCols = listOf(Color(0xFF2E4359), Color(0xFF3A546E), Color(0xFF2A3E52))   // left, middle, right gate colours

// ---------- Scenery ----------
fun DrawScope.drawSky(cam: Cam, pal: Pal, t: Float, px: Float) {
    val top = lerp(Color(0xFF05080C), pal.a1, .30f); val mid = lerp(Color(0xFF0F1A24), pal.a2, .40f)
    drawRect(Brush.verticalGradient(colors = listOf(top, mid), startY = 0f, endY = cam.hor), Offset.Zero, Size(cam.w, cam.hor + 1f))
    drawRect(Brush.verticalGradient(colors = listOf(lerp(mid, Color.Black, .35f), Color(0xFF05080C)), startY = cam.hor, endY = cam.h), Offset(0f, cam.hor), Size(cam.w, cam.h - cam.hor))
    for (i in 0 until 46) {
        val x = hash(i * 2) * cam.w - px * 6f; val y = hash(i * 2 + 1) * cam.hor * .92f
        drawCircle(Color.White.copy(alpha = (.30f + .45f * (.5f + .5f * sin(t * 2f + i))).coerceIn(0f, 1f)), 1.5f + hash(i + 99) * 2.2f, Offset((x + cam.w) % cam.w, y))
    }
    val pc = Offset(cam.w * .74f - px * 12f, cam.hor * .42f); val pr = cam.w * .11f
    drawCircle(Brush.radialGradient(colors = listOf(lerp(pal.a2, Color.White, .35f), pal.a1), center = pc, radius = pr), pr, pc)
    rotate(-18f, pc) { drawOval(Color.White.copy(alpha = .55f), Offset(pc.x - pr * 1.7f, pc.y - pr * .32f), Size(pr * 3.4f, pr * .64f), style = Stroke(width = 5f)) }
    ridge(cam, lerp(Color(0xFF0F1A24), pal.a1, .25f), cam.hor * .20f, .006f, px * 18f)
    ridge(cam, lerp(Color(0xFF0A121A), pal.a1, .15f), cam.hor * .12f, .011f, px * 34f + 300f)
    drawRect(Brush.verticalGradient(colors = listOf(Color.Transparent, pal.a2.copy(alpha = .45f)), startY = cam.hor - 60f, endY = cam.hor), Offset(0f, cam.hor - 60f), Size(cam.w, 60f))
}
private fun DrawScope.ridge(cam: Cam, color: Color, amp: Float, freq: Float, shift: Float) {
    val p = Path(); p.moveTo(0f, cam.hor)
    var x = 0f
    while (x <= cam.w + 24f) { val v = x + shift; p.lineTo(x, cam.hor - amp * (.35f + .65f * abs(sin(v * freq) * sin(v * freq * .43f + 1.3f)))); x += 24f }
    p.lineTo(cam.w + 24f, cam.hor); p.close(); drawPath(p, color)
}

/** The glowing road with side rails and tall crystal pylons. [dist] is how far the player has travelled. */
fun DrawScope.drawRoad(cam: Cam, pal: Pal, dist: Float) {
    val band = 2f; val far = 46f; val zMin = -cam.back + 0.5f
    val fogC = lerp(lerp(Color(0xFF0F1A24), pal.a2, .40f), Color.Black, .2f)
    val roadA = lerp(Color(0xFF101A24), pal.a1, .24f); val roadB = lerp(Color(0xFF0C141C), pal.a1, .12f)
    var k = floor(dist / band).toInt() - 1   // one band behind the player so the road reaches the screen bottom
    while (true) {
        val z0 = k * band - dist; val z1 = z0 + band
        if (z0 > far) break
        if (z1 > zMin) {
            val a = max(z0, zMin); val f = ((z0 - 8f) / 38f).coerceIn(0f, 1f)
            ground(cam, lerp(if ((k and 1) == 0) roadA else roadB, fogC, f), -ROAD_HALF, ROAD_HALF, a, z1)
            if ((k and 1) == 0) { ground(cam, Color.White.copy(alpha = .35f * (1f - f)), -.53f, -.47f, a, z1, .005f); ground(cam, Color.White.copy(alpha = .35f * (1f - f)), .47f, .53f, a, z1, .005f) }
            val rail = lerp(pal.a2, fogC, f)
            ground(cam, rail, ROAD_HALF, ROAD_HALF + .14f, a, z1, .01f); ground(cam, rail, -ROAD_HALF - .14f, -ROAD_HALF, a, z1, .01f)
            val glow = pal.a2.copy(alpha = .38f * (1f - f))
            wall(cam, glow, ROAD_HALF, a, z1, .30f); wall(cam, glow, -ROAD_HALF, a, z1, .30f)
        }
        k++
    }
    // crystal pylons along both sides (convex boxes, drawn far to near)
    val step = 6f; val j0 = floor(dist / step).toInt(); val tmpCol = lerp(pal.a1, Color.White, .25f)
    for (j in j0 + 9 downTo j0) for (side in intArrayOf(-1, 1)) {
        val z = j * step - dist; if (z < zMin + .6f || z > far) continue
        val h = .8f + 1.9f * hash(j * 2 + (side + 1)); val x = side * (2.9f + 1.4f * hash(j * 3 + side))
        val f = fog(z)
        drawMesh(cam, place(CUBE, .26f, h, .26f, 0f, 0f, 0f, x, h, z), if (((j + side) and 1) == 0) tmpCol else lerp(pal.a2, Color.White, .2f), alpha = f, edge = Color.White)
        drawMesh(cam, place(OCTA, .36f, .36f, .36f, 0f, j * .7f + dist * .6f, 0f, x, h * 2f + .45f, z), Color.White, alpha = f * .9f)
    }
}

// ---------- Entities, player, effects ----------
private fun DrawScope.shadowAt(cam: Cam, x: Float, z: Float, r: Float, a: Float) {
    if (!cam.inFront(z)) return
    val w = cam.scale(z) * r * 2f
    drawOval(Color.Black.copy(alpha = .30f * a), Offset(cam.sx(x, z) - w / 2f, cam.sy(0f, z) - w * .13f), Size(w, w * .26f))
}

fun DrawScope.drawEntities(cam: Cam, g: RunWorld) {
    val t = g.t
    for (e in g.ents.sortedByDescending { it.z }) {
        if (e.gone || !cam.inFront(e.z)) continue
        val f = fog(e.z); if (f <= 0.01f) continue
        val x = e.lane * LANE_W; val bob = .06f * sin(t * 3f + e.seed)
        when (e.type) {
            EType.LETTER -> {
                shadowAt(cam, x, e.z, .42f, f)
                val s = .40f; val col = laneCols[e.lane + 1]
                val q = drawMesh(cam, place(CUBE, s, s, s, .12f * sin(t * 1.3f + e.seed * 2f), .5f * sin(t * 1.7f + e.seed), 0f, x, .55f + bob, e.z), col, alpha = f, edge = Color.White)
                onFace(q[0]) { c -> faceText(c, e.ch.toString(), 50f, 50f, 78f) }
            }
            EType.ROCK -> {
                shadowAt(cam, x, e.z, .45f, f)
                drawMesh(cam, place(OCTA, .46f, .52f, .46f, t * .9f + e.seed, t * 1.4f + e.seed, 0f, x, .5f + bob, e.z), Color(0xFFB9473F), alpha = f, edge = Color(0xFFFF8A80))
            }
            EType.COIN -> {
                val w = cam.scale(e.z) * .34f * max(.12f, abs(cos(t * 4f + e.seed))); val h = cam.scale(e.z) * .34f
                val cx = cam.sx(x, e.z); val cy = cam.sy(.5f + bob, e.z)
                shadowAt(cam, x, e.z, .2f, f)
                drawOval(Color(0xFFFFB300).copy(alpha = f), Offset(cx - w, cy - h), Size(w * 2f, h * 2f))
                drawOval(Color(0xFFFFE082).copy(alpha = f), Offset(cx - w * .66f, cy - h * .66f), Size(w * 1.32f, h * 1.32f))
            }
            EType.SHIELD, EType.SLOW -> {
                val c = if (e.type == EType.SHIELD) Color(0xFF7FE7F0) else Color(0xFFC9B8FF)
                shadowAt(cam, x, e.z, .4f, f)
                drawMesh(cam, place(OCTA, .36f, .44f, .36f, 0f, t * 2f + e.seed, 0f, x, .55f + bob, e.z), c, alpha = f, edge = Color.White)
                text2d(if (e.type == EType.SHIELD) "🛡️" else "⏳", cam.sx(x, e.z), cam.sy(1.15f + bob, e.z), cam.scale(e.z) * .5f)
            }
        }
    }
}

fun DrawScope.drawPlayer(cam: Cam, g: RunWorld, pal: Pal, avatar: String) {
    val x = g.px * LANE_W; val t = g.t; val bob = .03f * sin(t * 6f)
    val blink = g.hitFlash > 0.05f && ((g.t * 18f).toInt() % 2 == 0)
    if (blink) return
    shadowAt(cam, x, 0f, .5f, 1f)
    // engine sparkle trail
    for (i in 0 until 6) { val z = -.25f - i * .22f; if (!cam.inFront(z)) continue
        drawCircle(pal.a2.copy(alpha = (.7f - i * .11f).coerceAtLeast(0f)), cam.scale(z) * (.07f - i * .008f), Offset(cam.sx(x + .16f * sin(t * 20f + i), z), cam.sy(.12f, z))) }
    val lean = (g.px - g.lane) * -.5f
    drawMesh(cam, place(CUBE, .40f, .06f, .55f, 0f, 0f, lean, x, .16f + bob, 0f), lerp(pal.a1, Color.White, .35f), edge = Color.White)
    drawMesh(cam, place(OCTA, .16f, .10f, .22f, 0f, 0f, lean, x, .10f + bob, -.38f), pal.a2, edge = Color.White)
    text2d(avatar, cam.sx(x, 0f), cam.sy(.78f + bob, 0f), cam.scale(0f) * .62f)
    if (g.shield) {
        val c = Offset(cam.sx(x, 0f), cam.sy(.62f + bob, 0f)); val r = cam.scale(0f) * .62f
        drawCircle(Color(0xFF4FC3F7).copy(alpha = .20f + .08f * sin(t * 8f)), r, c); drawCircle(Color.White.copy(alpha = .7f), r, c, style = Stroke(width = 4f))
    }
}

fun DrawScope.drawEffects(cam: Cam, g: RunWorld) {
    for (s in g.sparks) { if (!cam.inFront(s.z)) continue
        drawCircle(Color(s.color).copy(alpha = s.life.coerceIn(0f, 1f)), max(2.5f, cam.scale(s.z) * s.size), Offset(cam.sx(s.x, s.z), cam.sy(s.y, s.z))) }
    for (p in g.pops) text2d(p.text, cam.sx(p.x, 0f), cam.sy(p.y, 0f), cam.scale(0f) * .34f, Color(p.color).copy(alpha = p.life.coerceIn(0f, 1f)))
}

/** The complete 3D scene: sky, road, objects, player, particles, screen effects. */
fun DrawScope.drawScene(cam: Cam, pal: Pal, g: RunWorld?, demoTime: Float, avatar: String) {
    val t = g?.t ?: demoTime; val dist = g?.dist ?: (demoTime * 6f); val px = g?.px ?: 0f
    val sh = if (g != null) sin(t * 60f) * g.shake * 14f else 0f
    translate(left = sh) {
        drawSky(cam, pal, t, px)
        drawRoad(cam, pal, dist)
        if (g != null) { drawEntities(cam, g); drawPlayer(cam, g, pal, avatar); drawEffects(cam, g) }
    }
    if (g != null) {
        if (g.slowT > 0f) drawRect(Color.White.copy(alpha = .05f), Offset.Zero, size)
        if (g.hitFlash > 0f) drawRect(Color(0xFFFF1744).copy(alpha = .32f * g.hitFlash), Offset.Zero, size)
        if (g.bannerT > 0f) {
            val a = (g.bannerT * 2f).coerceIn(0f, 1f); val pulse = 1f + .08f * sin(g.t * 14f)
            text2d(bank.firstOrNull { it.w == g.banner }?.e ?: "🎉", cam.w / 2f, cam.h * .36f, cam.w * .26f * pulse, Color.White.copy(alpha = a))
            text2d(g.banner, cam.w / 2f, cam.h * .50f, cam.w * .17f * pulse, Color.White.copy(alpha = a))
        }
    }
}

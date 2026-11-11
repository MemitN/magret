package com.margaretm.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as rotateDraw
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

// =====================================================================================
//  Ui.kt  -  design system.  Dark, quiet, one accent colour per theme.
//  Everything else in the app takes its colours from the tokens below.
// =====================================================================================
data class Pal(val name: String, val emoji: String, val bg1: Color, val bg2: Color, val a1: Color, val a2: Color, val price: Int)
val pals = listOf(
    Pal("Aqua", "◐", Color(0xFF080C12), Color(0xFF0C141D), Color(0xFF5CE1E6), Color(0xFF2BB3C0), 0),
    Pal("Lime", "◑", Color(0xFF0A0E0A), Color(0xFF111A10), Color(0xFFB6F36B), Color(0xFF7BC62D), 60),
    Pal("Amber", "◒", Color(0xFF0E0B08), Color(0xFF191209), Color(0xFFFFC857), Color(0xFFE59E1A), 120),
    Pal("Violet", "◓", Color(0xFF0B0911), Color(0xFF150F22), Color(0xFFB69CFF), Color(0xFF8566E8), 200))
val LocalPal = compositionLocalOf { pals[0] }
val Ink = Color(0xFFE8EEF5)        // main text
val Muted = Color(0xFF8A98A8)      // secondary text
val Panel = Color(0xFF121A23)      // cards
val Panel2 = Color(0xFF1A2430)     // raised / inactive controls
val Hair = Color(0xFF2A3645)       // 1dp outlines
val Dark = Color(0xFF061017)       // text on the accent colour
val Ok = Color(0xFF4ADE9A)         // correct
val Warn = Color(0xFFFF7A7A)       // wrong
val Shape12 = RoundedCornerShape(12.dp); val Shape16 = RoundedCornerShape(16.dp); val Shape20 = RoundedCornerShape(20.dp)

// ---------- Backdrop: near-black, faint dot grid, one slow accent glow ----------
@Composable fun Backdrop(content: @Composable BoxScope.() -> Unit) {
    val pal = LocalPal.current
    val t by rememberInfiniteTransition(label = "bg").animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "t")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(pal.bg1, pal.bg2)))) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(pal.a1.copy(alpha = .10f + .05f * t), Color.Transparent), Offset(size.width * .5f, -size.height * .02f), size.width * 1.1f), size.width * 1.1f, Offset(size.width * .5f, -size.height * .02f))
            val gap = 44.dp.toPx(); var y = gap / 2
            while (y < size.height) { var x = gap / 2; while (x < size.width) { drawCircle(Color.White.copy(alpha = .045f), 1.3f, Offset(x, y)); x += gap }; y += gap }
        }
        content()
    }
}

// ---------- Buttons, cards, chips ----------
@Composable fun PBtn(t: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), h: Dp = 56.dp, cols: List<Color>? = null, ghost: Boolean = false) {
    val pal = LocalPal.current; val hp = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }; val down by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (down) .96f else 1f, spring(dampingRatio = .6f, stiffness = 600f), label = "sc")
    val shape = RoundedCornerShape(minOf(h / 2, 18.dp))
    Box(modifier.height(h).scale(sc).background(if (ghost) Panel2 else pal.a1, shape).then(if (ghost) Modifier.border(1.dp, Hair, shape) else Modifier)
        .clickable(interactionSource = src, indication = null) { hp.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
        Alignment.Center) { Text(t, fontSize = 16.sp, color = if (ghost) Ink else Dark, fontWeight = FontWeight.SemiBold, letterSpacing = .4.sp, textAlign = TextAlign.Center) }
}
@Composable fun Btn(t: String, onClick: () -> Unit) = PBtn(t, onClick)

@Composable fun GCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }; val down by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (down) .98f else 1f, label = "gc")
    val m = modifier.scale(sc).background(Panel, Shape20).border(1.dp, Hair, Shape20)
    Column(if (onClick != null) m.clickable(interactionSource = src, indication = null, onClick = onClick).padding(16.dp) else m.padding(16.dp), content = content)
}
@Composable fun Chip(t: String) = Box(Modifier.background(Panel2, RoundedCornerShape(20.dp)).border(1.dp, Hair, RoundedCornerShape(20.dp))
    .padding(horizontal = 12.dp, vertical = 6.dp)) { Text(t, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink) }

/** Menu tile: dark panel, hairline outline, picture at the left, text underneath. `cols` is ignored (kept so old call sites compile). */
@Composable fun Tile(emoji: String, title: String, sub: String, cols: List<Color>, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }; val down by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (down) .96f else 1f, spring(dampingRatio = .6f), label = "tile")
    val pal = LocalPal.current
    Column(modifier.scale(sc).height(108.dp).background(Panel, Shape20).border(1.dp, if (down) pal.a1 else Hair, Shape20)
        .clickable(interactionSource = src, indication = null, onClick = onClick).padding(14.dp), Arrangement.SpaceBetween, Alignment.Start) {
        Text(emoji, fontSize = 30.sp)
        Column { Text(title, fontSize = 15.sp, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (sub.isNotEmpty()) Text(sub, fontSize = 11.sp, color = Muted, maxLines = 1) }
    }
}

@Composable fun Section(t: String) = Text(t.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Muted, letterSpacing = 1.8.sp, modifier = Modifier.padding(top = 14.dp, bottom = 2.dp))

@Composable fun Bar(p: Float, modifier: Modifier = Modifier, h: Dp = 6.dp) {
    val pal = LocalPal.current; val a by animateFloatAsState(p.coerceIn(0f, 1f), tween(700), label = "bar")
    Box(modifier.fillMaxWidth().height(h).background(Hair, RoundedCornerShape(h))) {
        Box(Modifier.fillMaxWidth(a).fillMaxHeight().background(pal.a1, RoundedCornerShape(h)))
    }
}
@Composable fun Ring(p: Float, size: Dp, label: String, sub: String = "") {
    val pal = LocalPal.current; val a by animateFloatAsState(p.coerceIn(0f, 1f), tween(900), label = "ring")
    Box(Modifier.size(size), Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            drawArc(color = Hair, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 8f, cap = StrokeCap.Round))
            drawArc(color = pal.a1, startAngle = -90f, sweepAngle = 360f * a, useCenter = false, style = Stroke(width = 8f, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            if (sub.isNotEmpty()) Text(sub, fontSize = 10.sp, color = Muted)
        }
    }
}

// ---------- Motion helpers ----------
@Composable fun Pop(delayMs: Int = 0, content: @Composable () -> Unit) {
    var s by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(delayMs.toLong()); s = true }
    val sc by animateFloatAsState(if (s) 1f else .85f, spring(dampingRatio = .7f), label = "pop")
    val al by animateFloatAsState(if (s) 1f else 0f, tween(220), label = "popa")
    Box(Modifier.scale(sc).alpha(al)) { content() }
}
@Composable fun Mascot(e: String, size: Int = 64) {
    val t by rememberInfiniteTransition(label = "m").animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    Text(e, fontSize = size.sp, modifier = Modifier.offset(y = (-6 * t).dp))
}
@Composable fun Confetti(key: Any) {
    val pal = LocalPal.current
    val prog = remember(key) { Animatable(0f) }
    val parts = remember(key) { List(46) { floatArrayOf(Random.nextFloat(), Random.nextFloat() * .3f, (Random.nextFloat() - .5f) * .25f, .5f + Random.nextFloat() * .8f, Random.nextFloat() * 6f, Random.nextInt(3).toFloat()) } }
    LaunchedEffect(key) { prog.animateTo(1f, tween(2400, easing = LinearEasing)) }
    val cols = listOf(pal.a1, Color.White, pal.a2)
    Canvas(Modifier.fillMaxSize()) {
        val t = prog.value
        if (t < 1f) parts.forEach { q ->
            val x = (q[0] + q[2] * t) * size.width; val y = (q[1] - .1f + q[3] * t * 1.2f) * size.height
            rotateDraw(degrees = q[4] * t * 120f, pivot = Offset(x, y)) { drawRect(cols[q[5].toInt()].copy(alpha = 1f - t * t), Offset(x, y), Size(8f, 16f)) }
        }
    }
}

// ---------- Chrome ----------
@Composable fun TopBar(title: String, back: (() -> Unit)?, right: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) Box(Modifier.size(40.dp).background(Panel2, Shape12).border(1.dp, Hair, Shape12).clickable(onClick = back), Alignment.Center) {
            Canvas(Modifier.size(16.dp)) { val p = Path(); p.moveTo(size.width * .65f, size.height * .1f); p.lineTo(size.width * .25f, size.height * .5f); p.lineTo(size.width * .65f, size.height * .9f)
                drawPath(p, Ink, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round)) } }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f).padding(horizontal = if (back != null) 12.dp else 0.dp))
        right()
    }
}
val tabs = listOf(Triple("home", "", "Home"), Triple("map", "", "Map"), Triple("awards", "", "Badges"), Triple("shop", "", "Shop"), Triple("parent", "", "Parents"))

/** Thin line icons, drawn so they take the accent / muted colour (emoji can't be recoloured). */
@Composable fun NavIcon(id: String, c: Color, size: Dp = 24.dp) = Canvas(Modifier.size(size)) {
    val w = this.size.width; val s = Stroke(2.2f * (w / 48f) * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round); val p = Path()
    when (id) {
        "home" -> { p.moveTo(w * .12f, w * .48f); p.lineTo(w * .5f, w * .14f); p.lineTo(w * .88f, w * .48f); p.moveTo(w * .22f, w * .42f); p.lineTo(w * .22f, w * .86f); p.lineTo(w * .78f, w * .86f); p.lineTo(w * .78f, w * .42f) }
        "map" -> { p.moveTo(w * .12f, w * .22f); p.lineTo(w * .38f, w * .14f); p.lineTo(w * .62f, w * .22f); p.lineTo(w * .88f, w * .14f); p.lineTo(w * .88f, w * .78f); p.lineTo(w * .62f, w * .86f); p.lineTo(w * .38f, w * .78f); p.lineTo(w * .12f, w * .86f); p.close()
            p.moveTo(w * .38f, w * .14f); p.lineTo(w * .38f, w * .78f); p.moveTo(w * .62f, w * .22f); p.lineTo(w * .62f, w * .86f) }
        "awards" -> { for (i in 0 until 5) { val a = Math.toRadians(-90.0 + i * 72); val b = Math.toRadians(-90.0 + i * 72 + 36)
            val x1 = w * .5f + w * .38f * Math.cos(a).toFloat(); val y1 = w * .52f + w * .38f * Math.sin(a).toFloat(); val x2 = w * .5f + w * .16f * Math.cos(b).toFloat(); val y2 = w * .52f + w * .16f * Math.sin(b).toFloat()
            if (i == 0) p.moveTo(x1, y1) else p.lineTo(x1, y1); p.lineTo(x2, y2) }; p.close() }
        "shop" -> { p.moveTo(w * .5f, w * .1f); p.lineTo(w * .86f, w * .42f); p.lineTo(w * .5f, w * .9f); p.lineTo(w * .14f, w * .42f); p.close(); p.moveTo(w * .14f, w * .42f); p.lineTo(w * .86f, w * .42f); p.moveTo(w * .36f, w * .42f); p.lineTo(w * .5f, w * .9f); p.moveTo(w * .64f, w * .42f); p.lineTo(w * .5f, w * .9f) }
        else -> { p.moveTo(w * .5f, w * .1f); p.lineTo(w * .84f, w * .22f); p.lineTo(w * .84f, w * .5f); p.cubicTo(w * .84f, w * .72f, w * .66f, w * .84f, w * .5f, w * .92f); p.cubicTo(w * .34f, w * .84f, w * .16f, w * .72f, w * .16f, w * .5f); p.lineTo(w * .16f, w * .22f); p.close() }
    }
    drawPath(p, c, style = s)
}
@Composable fun BottomNav(cur: String, go: (String) -> Unit) {
    val pal = LocalPal.current; val fx = LocalSfx.current
    Row(Modifier.fillMaxWidth().background(Panel, Shape20).border(1.dp, Hair, Shape20).padding(horizontal = 4.dp, vertical = 8.dp), Arrangement.SpaceEvenly) {
        tabs.forEach { (id, _, n) ->
            val on = id == cur
            val c by animateColorAsState(if (on) pal.a1 else Muted, tween(200), label = "navc")
            Column(Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!on) { fx?.tap(); go(id) } }, horizontalAlignment = Alignment.CenterHorizontally) {
                NavIcon(id, c); Spacer(Modifier.height(3.dp))
                Text(n, fontSize = 10.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = c, letterSpacing = .6.sp)
                Spacer(Modifier.height(3.dp)); Box(Modifier.size(width = if (on) 14.dp else 0.dp, height = 2.dp).background(pal.a1, CircleShape))
            }
        }
    }
}
@Composable fun BadgePopup(list: List<Badge>, close: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xDD04080C)).clickable(onClick = close), Alignment.Center) {
        Confetti(list)
        Pop { GCard(Modifier.padding(28.dp).fillMaxWidth()) {
            Text("NEW BADGE${if (list.size > 1) "S" else ""}", fontSize = 12.sp, color = LocalPal.current.a1, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally))
            list.take(4).forEach { b -> Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(b.emoji, fontSize = 36.sp); Spacer(Modifier.width(14.dp)); Column { Text(b.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Ink); Text(b.desc, fontSize = 13.sp, color = Muted) } } }
            Spacer(Modifier.height(14.dp)); Text("+10 coins each · tap to continue", fontSize = 12.sp, color = Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
        } }
    }
}

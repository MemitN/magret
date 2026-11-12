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
import androidx.compose.ui.draw.clip
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
    Pal("Meadow", "🌿", Color(0xFF2DB87A), Color(0xFF117A4D), Color(0xFF1B9A62), Color(0xFF7ED9A8), 0),
    Pal("Lagoon", "🌊", Color(0xFF2AA9C9), Color(0xFF127393), Color(0xFF1387AA), Color(0xFF7CD3EA), 60),
    Pal("Berry", "🫐", Color(0xFF9B6BE8), Color(0xFF5A3BB0), Color(0xFF6F45CF), Color(0xFFC1A6F5), 120),
    Pal("Sunset", "🌅", Color(0xFFFF8F6B), Color(0xFFD9503A), Color(0xFFE4573D), Color(0xFFFFC0A8), 200))
val LocalPal = compositionLocalOf { pals[0] }
val Ink = Color(0xFF123A2C)        // main text
val Muted = Color(0xFF66807A)      // secondary text
val Panel = Color(0xFFF3F8F5)      // cards on the white sheet
val Panel2 = Color(0xFFE4EFE9)     // inactive controls
val Hair = Color(0xFFD2E2D8)       // 1dp outlines
val Dark = Color(0xFFFFFFFF)       // text on the accent colour (name kept from the earlier versions)
val Ok = Color(0xFF1FA971)         // correct
val Warn = Color(0xFFE5484D)       // wrong
val Shape28 = RoundedCornerShape(28.dp); val Shape12 = RoundedCornerShape(12.dp); val Shape16 = RoundedCornerShape(16.dp); val Shape20 = RoundedCornerShape(20.dp)

// ---------- Backdrop: saturated theme colour with soft leaf shapes (the white sheet sits on top of this) ----------
@Composable fun Backdrop(content: @Composable BoxScope.() -> Unit) {
    val pal = LocalPal.current
    val t by rememberInfiniteTransition(label = "bg").animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "t")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(pal.bg1, pal.bg2)))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            fun leaf(cx: Float, cy: Float, lw: Float, lh: Float, deg: Float, a: Float) = rotateDraw(deg, Offset(cx, cy)) { drawOval(Color.White.copy(alpha = a), Offset(cx - lw / 2, cy - lh / 2), Size(lw, lh)) }
            leaf(w * .05f, h * .05f, w * .7f, w * .26f, -32f + 4f * t, .10f); leaf(w * .98f, h * .10f, w * .55f, w * .20f, 38f - 4f * t, .09f)
            leaf(w * .12f, h * .30f, w * .5f, w * .18f, 24f, .07f); leaf(w * .95f, h * .62f, w * .6f, w * .2f, -28f + 3f * t, .06f)
            drawCircle(Color.White.copy(alpha = .08f), w * .30f, Offset(w * .86f, h * .02f)); drawCircle(Color.White.copy(alpha = .06f), w * .18f, Offset(w * .08f, h * .52f))
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

/** Menu tile: a soft card with a cartoon character leaning in from the corner. `cols` is ignored (kept so old call sites compile). */
@Composable fun Tile(emoji: String, title: String, sub: String, cols: List<Color>, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }; val down by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (down) .96f else 1f, spring(dampingRatio = .6f), label = "tile")
    val pal = LocalPal.current; val fx = LocalSfx.current
    val kind = (title.hashCode() and 0x7fffffff) % CRITTERS
    Box(modifier.scale(sc).height(118.dp).clip(Shape20).background(if (kind % 2 == 0) pal.a2.copy(alpha = .22f) else Panel).border(1.dp, if (down) pal.a1 else Hair, Shape20)
        .clickable(interactionSource = src, indication = null) { fx?.tap(); onClick() }) {
        Critter(kind, 70.dp, Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 16.dp))
        Box(Modifier.padding(12.dp).size(34.dp).background(Color.White, CircleShape).border(1.dp, Hair, CircleShape), Alignment.Center) { Text(emoji, fontSize = 18.sp) }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(.70f).padding(start = 12.dp, bottom = 10.dp)) {
            Text(title, fontSize = 14.sp, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 16.sp)
            if (sub.isNotEmpty()) Text(sub, fontSize = 11.sp, color = Muted, maxLines = 1)
        }
    }
}

/** Big feature card: theme-colour gradient, white text, a character beside it. */
@Composable fun HeroCard(title: String, sub: String, kind: Int, light: Boolean = false, onClick: () -> Unit) {
    val pal = LocalPal.current; val fx = LocalSfx.current
    val src = remember { MutableInteractionSource() }; val down by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (down) .97f else 1f, spring(dampingRatio = .6f), label = "hero")
    val shape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth().height(104.dp).scale(sc).clip(shape)
        .background(if (light) Brush.horizontalGradient(listOf(Panel, Panel)) else Brush.linearGradient(listOf(pal.bg1, pal.a1)), shape)
        .then(if (light) Modifier.border(1.dp, Hair, shape) else Modifier)
        .clickable(interactionSource = src, indication = null) { fx?.tap(); onClick() }) {
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.66f).padding(start = 18.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (light) Ink else Color.White)
            Text(sub, fontSize = 12.sp, color = if (light) Muted else Color.White.copy(alpha = .85f), maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.background(if (light) pal.a1 else Color.White, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 5.dp)) {
                Text("Play  ▶", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (light) Color.White else pal.a1) }
        }
        CritterBounce(kind, 92.dp, Modifier.align(Alignment.BottomEnd).padding(end = 10.dp))
    }
}

/** Green strip above the white sheet: who is playing, their coins and streak, and a character standing on the sheet. */
@Composable fun AppHeader(k: Profile, screen: String) {
    val title = when (screen) { "home" -> "Hi, ${k.name}!"; "map" -> "Adventure map"; "awards" -> "Badges"; "shop" -> "Shop"; else -> "Parent zone" }
    val sub = if (screen == "home") "Level ${k.level} · ${rankName(k.level)}" else "${k.av} ${k.name} · level ${k.level}"
    val kind = when (screen) { "home" -> 0; "map" -> 1; "awards" -> 4; "shop" -> 2; else -> 3 }
    Row(Modifier.fillMaxWidth().height(112.dp).padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            Text(sub, fontSize = 13.sp, color = Color.White.copy(alpha = .88f), maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Chip("🪙 ${k.coins}"); Chip("🔥 ${k.streak}"); Chip("⭐ ${k.stars}") }
        }
        CritterBounce(kind, 100.dp, Modifier.align(Alignment.Bottom))
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
    val cols = listOf(pal.a1, pal.a2, Color(0xFFFFC857))
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

val navCritter = mapOf("home" to 0, "map" to 1, "awards" to 4, "shop" to 2, "parent" to 3)
@Composable fun BottomNav(cur: String, go: (String) -> Unit) {
    val pal = LocalPal.current; val fx = LocalSfx.current
    Row(Modifier.fillMaxWidth().clip(Shape28).background(Color.White, Shape28).padding(horizontal = 6.dp, vertical = 6.dp), Arrangement.SpaceEvenly) {
        tabs.forEach { (id, _, n) ->
            val on = id == cur
            val bg by animateColorAsState(if (on) pal.a1.copy(alpha = .14f) else Color.Transparent, tween(200), label = "navbg")
            val sc by animateFloatAsState(if (on) 1.08f else .92f, spring(dampingRatio = .55f), label = "navs")
            Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(bg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!on) { fx?.tap(); go(id) } }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Critter(navCritter[id] ?: 0, 38.dp, Modifier.scale(sc).alpha(if (on) 1f else .75f))
                Text(n, fontSize = 10.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = if (on) pal.a1 else Muted, letterSpacing = .4.sp)
            }
        }
    }
}
@Composable fun BadgePopup(list: List<Badge>, close: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0x99101820)).clickable(onClick = close), Alignment.Center) {
        Confetti(list)
        Pop { GCard(Modifier.padding(28.dp).fillMaxWidth()) {
            Text("NEW BADGE${if (list.size > 1) "S" else ""}", fontSize = 12.sp, color = LocalPal.current.a1, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally))
            list.take(4).forEach { b -> Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(b.emoji, fontSize = 36.sp); Spacer(Modifier.width(14.dp)); Column { Text(b.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Ink); Text(b.desc, fontSize = 13.sp, color = Muted) } } }
            Spacer(Modifier.height(14.dp)); Text("+10 coins each · tap to continue", fontSize = 12.sp, color = Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
        } }
    }
}

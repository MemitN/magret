package com.margaretm.app

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as rotateDraw
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.random.Random

// ---------- Themes (unlockable in the Shop) ----------
data class Pal(val name: String, val emoji: String, val bg1: Color, val bg2: Color, val a1: Color, val a2: Color, val price: Int)
val pals = listOf(
    Pal("Candy", "🍭", Color(0xFFEDE7FF), Color(0xFFFFF1E6), Color(0xFF6C4DF4), Color(0xFFFF6FB5), 0),
    Pal("Ocean", "🌊", Color(0xFFDDF4FF), Color(0xFFE3FFF3), Color(0xFF0E8BD8), Color(0xFF22C1A5), 60),
    Pal("Forest", "🌳", Color(0xFFE8F6DE), Color(0xFFFFF6D6), Color(0xFF2E9E5B), Color(0xFF9BC53D), 120),
    Pal("Galaxy", "🌌", Color(0xFFD6E0FF), Color(0xFFEBD6FF), Color(0xFF3F2BB8), Color(0xFF00B8D9), 200))
val LocalPal = compositionLocalOf { pals[0] }
val Purple = Color(0xFF6C4DF4); val Pink = Color(0xFFFF6FB5)
val Ink = Color(0xFF2B2B44)
val cardColors = listOf(listOf(Color(0xFFFF8A5B), Color(0xFFFF5E8A)), listOf(Color(0xFF6C4DF4), Color(0xFF9B7BFF)),
    listOf(Color(0xFF22C1A5), Color(0xFF3DB9FF)), listOf(Color(0xFFFFB400), Color(0xFFFF7A00)))

// ---------- Voice + sound ----------
class Voice(ctx: Context) {
    private var ready = false
    private lateinit var tts: TextToSpeech
    var on = true
    var rate = 0.8f
        set(v) { field = v; if (ready) tts.setSpeechRate(v) }
    init { tts = TextToSpeech(ctx) { if (it == TextToSpeech.SUCCESS) { tts.language = Locale("en", "ZA"); tts.setSpeechRate(rate); ready = true } } }
    fun say(t: String) { if (ready && on) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, t) }
    fun close() { tts.shutdown() }
}
class Sfx {
    private val tg: ToneGenerator? = try { ToneGenerator(AudioManager.STREAM_MUSIC, 55) } catch (e: Exception) { null }
    var on = true
    fun good() { if (on) tg?.startTone(ToneGenerator.TONE_PROP_ACK, 160) }
    fun bad() { if (on) tg?.startTone(ToneGenerator.TONE_PROP_NACK, 160) }
    fun win() { if (on) tg?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 300) }
    fun close() { tg?.release() }
}

// ---------- Backdrop: slow drifting colour orbs ----------
@Composable fun Backdrop(content: @Composable BoxScope.() -> Unit) {
    val pal = LocalPal.current
    val t by rememberInfiniteTransition(label = "bg").animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "t")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(pal.bg1, pal.bg2)))) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(pal.a1.copy(alpha = .10f), size.width * .55f, Offset(size.width * (.15f + .2f * t), size.height * (.12f + .05f * t)))
            drawCircle(pal.a2.copy(alpha = .10f), size.width * .5f, Offset(size.width * (.9f - .25f * t), size.height * (.85f - .06f * t)))
            drawCircle(pal.a2.copy(alpha = .07f), size.width * .3f, Offset(size.width * .8f, size.height * (.3f + .2f * t)))
        }
        content()
    }
}

// ---------- Buttons, cards, chips ----------
@Composable fun PBtn(t: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), h: Dp = 64.dp, cols: List<Color>? = null) {
    val pal = LocalPal.current; val hp = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }; val down by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (down) .94f else 1f, spring(dampingRatio = .5f, stiffness = 500f), label = "sc")
    Box(modifier.height(h).scale(sc).shadow(8.dp, RoundedCornerShape(h / 2))
        .background(Brush.horizontalGradient(cols ?: listOf(pal.a1, pal.a2)), RoundedCornerShape(h / 2))
        .clickable(interactionSource = src, indication = null) { hp.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        Alignment.Center) { Text(t, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}
@Composable fun Btn(t: String, onClick: () -> Unit) = PBtn(t, onClick)

@Composable fun GCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }; val down by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (down) .97f else 1f, label = "gc")
    val m = modifier.scale(sc).shadow(6.dp, RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .94f), RoundedCornerShape(24.dp))
    Column(if (onClick != null) m.clickable(interactionSource = src, indication = null, onClick = onClick).padding(16.dp) else m.padding(16.dp), content = content)
}
@Composable fun Chip(t: String) = Box(Modifier.shadow(4.dp, RoundedCornerShape(20.dp)).background(Color.White, RoundedCornerShape(20.dp))
    .padding(horizontal = 14.dp, vertical = 8.dp)) { Text(t, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink) }

@Composable fun Tile(emoji: String, title: String, sub: String, cols: List<Color>, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }; val down by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (down) .94f else 1f, spring(dampingRatio = .55f), label = "tile")
    Column(modifier.scale(sc).height(116.dp).shadow(8.dp, RoundedCornerShape(26.dp)).background(Brush.linearGradient(cols), RoundedCornerShape(26.dp))
        .clickable(interactionSource = src, indication = null, onClick = onClick).padding(8.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 38.sp); Text(title, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (sub.isNotEmpty()) Text(sub, fontSize = 11.sp, color = Color.White.copy(alpha = .85f), textAlign = TextAlign.Center)
    }
}

@Composable fun Section(t: String) = Text(t, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.padding(top = 8.dp))

@Composable fun Bar(p: Float, modifier: Modifier = Modifier, h: Dp = 12.dp) {
    val pal = LocalPal.current; val a by animateFloatAsState(p.coerceIn(0f, 1f), tween(700), label = "bar")
    Box(modifier.fillMaxWidth().height(h).background(Color(0x22000000), RoundedCornerShape(h))) {
        Box(Modifier.fillMaxWidth(a).fillMaxHeight().background(Brush.horizontalGradient(listOf(pal.a1, pal.a2)), RoundedCornerShape(h)))
    }
}
@Composable fun Ring(p: Float, size: Dp, label: String, sub: String = "") {
    val pal = LocalPal.current; val a by animateFloatAsState(p.coerceIn(0f, 1f), tween(900), label = "ring")
    Box(Modifier.size(size), Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            drawArc(color = Color(0x22000000), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 14f, cap = StrokeCap.Round))
            drawArc(brush = Brush.linearGradient(listOf(pal.a1, pal.a2)), startAngle = -90f, sweepAngle = 360f * a, useCenter = false, style = Stroke(width = 14f, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            if (sub.isNotEmpty()) Text(sub, fontSize = 10.sp, color = Ink)
        }
    }
}

// ---------- Motion helpers ----------
@Composable fun Pop(delayMs: Int = 0, content: @Composable () -> Unit) {
    var s by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(delayMs.toLong()); s = true }
    val sc by animateFloatAsState(if (s) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "pop")
    Box(Modifier.scale(sc)) { content() }
}
@Composable fun Mascot(e: String, size: Int = 64) {
    val t by rememberInfiniteTransition(label = "m").animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    Text(e, fontSize = size.sp, modifier = Modifier.offset(y = (-10 * t).dp).rotate(-6f + 12f * t))
}
@Composable fun Confetti(key: Any) {
    val prog = remember(key) { Animatable(0f) }
    val parts = remember(key) { List(70) { floatArrayOf(Random.nextFloat(), Random.nextFloat() * .3f, (Random.nextFloat() - .5f) * .3f, .5f + Random.nextFloat() * .8f, Random.nextFloat() * 6f, Random.nextInt(5).toFloat()) } }
    LaunchedEffect(key) { prog.animateTo(1f, tween(2600, easing = LinearEasing)) }
    val cols = listOf(Color(0xFFFF5E8A), Color(0xFFFFB400), Color(0xFF22C1A5), Color(0xFF6C4DF4), Color(0xFF3DB9FF))
    Canvas(Modifier.fillMaxSize()) {
        val t = prog.value
        if (t < 1f) parts.forEach { q ->
            val x = (q[0] + q[2] * t) * size.width; val y = (q[1] - .1f + q[3] * t * 1.2f) * size.height
            rotateDraw(degrees = q[4] * t * 120f, pivot = Offset(x, y)) { drawRect(cols[q[5].toInt()].copy(alpha = 1f - t * t), Offset(x, y), Size(14f, 22f)) }
        }
    }
}

// ---------- Chrome ----------
@Composable fun TopBar(title: String, back: (() -> Unit)?, right: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) Box(Modifier.size(44.dp).shadow(4.dp, CircleShape).background(Color.White, CircleShape).clickable(onClick = back), Alignment.Center) {
            Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink) }
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
        right()
    }
}
val tabs = listOf(Triple("home", "🏠", "Home"), Triple("map", "🗺️", "Map"), Triple("awards", "🏅", "Badges"), Triple("shop", "🛍️", "Shop"), Triple("parent", "👪", "Parents"))
@Composable fun BottomNav(cur: String, go: (String) -> Unit) {
    val pal = LocalPal.current
    Row(Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(28.dp)).background(Color.White, RoundedCornerShape(28.dp)).padding(6.dp), Arrangement.SpaceEvenly) {
        tabs.forEach { (id, e, n) ->
            val on = id == cur
            val sc by animateFloatAsState(if (on) 1.12f else 1f, spring(dampingRatio = .5f), label = "nav")
            val bg by animateColorAsState(if (on) pal.a1.copy(alpha = .14f) else Color.Transparent, label = "navbg")
            Column(Modifier.weight(1f).scale(sc).background(bg, RoundedCornerShape(20.dp)).clickable { go(id) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(e, fontSize = 22.sp); Text(n, fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, color = if (on) pal.a1 else Ink)
            }
        }
    }
}
@Composable fun BadgePopup(list: List<Badge>, close: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable(onClick = close), Alignment.Center) {
        Confetti(list)
        Pop { GCard(Modifier.padding(28.dp).fillMaxWidth()) {
            Big("🎉 New badge${if (list.size > 1) "s" else ""}!", 26)
            list.take(4).forEach { b -> Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(b.emoji, fontSize = 40.sp); Spacer(Modifier.width(12.dp)); Column { Text(b.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink); Text(b.desc, fontSize = 13.sp, color = Ink) } } }
            Spacer(Modifier.height(10.dp)); Text("+10 coins each · tap to continue", fontSize = 12.sp, color = Ink, modifier = Modifier.align(Alignment.CenterHorizontally))
        } }
    }
}

package com.margaretm.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

// =====================================================================================
//  Games3D.kt  -  the two 3D activities + the animated 3D hero card used on the Home screen
// =====================================================================================

private val cubeCols = listOf(Color(0xFF26384B), Color(0xFF1E2F41), Color(0xFF2E4359), Color(0xFF223446), Color(0xFF2A3E53), Color(0xFF1B2B3B))

// ---------------------------------------------------------------------------------
//  LETTER DASH 3D
//  A 3-lane space race. Pick the next letter of the word out of three glowing letter
//  cubes, dodge asteroids, grab shields / slow-mo / coins. Swipe or tap to change lane.
// ---------------------------------------------------------------------------------
@Composable fun LetterDash(k: Profile, diff: Int, v: Voice, fx: Sfx, onRecord: (String, Boolean) -> Unit, onFinish: (RunSummary) -> Unit, back: () -> Unit) {
    val pal = LocalPal.current; val hp = LocalHapticFeedback.current
    var world by remember { mutableStateOf<RunWorld?>(null) }
    var speed by remember { mutableIntStateOf(1) }
    var demo by remember { mutableFloatStateOf(0f) }
    var hud by remember { mutableIntStateOf(0) }
    var summary by remember { mutableStateOf<RunSummary?>(null) }
    var bestBefore by remember { mutableIntStateOf(k.runBest) }
    val rec by rememberUpdatedState(onRecord); val fin by rememberUpdatedState(onFinish)

    fun start() {
        val seed = System.nanoTime(); bestBefore = k.runBest; summary = null
        world = RunWorld(runWords(k.mastery, diff, 4, seed), diff, listOf(.75f, 1f, 1.3f)[speed], seed)
        hud++
    }
    fun handle(e: Ev) {
        when (e) {
            is Ev.Good -> { fx.good(); v.say(e.ch.toString()); hp.performHapticFeedback(HapticFeedbackType.LongPress) }
            is Ev.Bad -> { fx.bad(); v.say("Oops! Look for ${e.need}") }
            is Ev.WordDone -> { fx.win(); v.say("${e.word.lowercase()}! " + if (e.clean) "Perfect!" else "Great job!"); rec(e.word, e.clean) }
            Ev.Crash -> { fx.crash(); v.say("Ouch! Dodge the rocks!") }
            Ev.ShieldPop -> { fx.power(); v.say("Your shield saved you!") }
            Ev.Coin -> fx.coin()
            is Ev.Power -> fx.power()
            is Ev.Over -> if (e.won) fx.win()
        }
        hud++
    }

    // One loop drives everything: the animated menu backdrop, the simulation, and the hand-over to the result card.
    LaunchedEffect(Unit) {
        var last = 0L; var overAt = 0L
        while (true) {
            val now = withFrameNanos { it }
            val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now; demo += dt
            val g = world
            if (g != null) {
                if (!g.over) { for (e in g.step(dt)) handle(e) }
                else if (summary == null) {
                    if (overAt == 0L) overAt = now
                    else if (now - overAt > 1_100_000_000L) { val s = g.summary(); summary = s; overAt = 0L; fin(s) }
                }
            }
        }
    }
    LaunchedEffect(Unit) { v.say("Letter Dash! Catch the letters to spell words.") }

    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))) {
        Canvas(Modifier.fillMaxSize()
            .pointerInput(world) { detectTapGestures { o -> if (world != null) { world?.move(if (o.x < size.width / 2f) -1 else 1); fx.whoosh() } } }
            .pointerInput(world) {
                var acc = 0f
                detectHorizontalDragGestures(onDragStart = { acc = 0f }) { ch, d ->
                    ch.consume(); acc += d
                    if (acc > 50f) { world?.move(1); fx.whoosh(); acc = 0f } else if (acc < -50f) { world?.move(-1); fx.whoosh(); acc = 0f }
                }
            }) {
            drawScene(Cam(size.width, size.height), pal, world, demo, k.av)
        }
        val g = world
        if (g != null && summary == null) DashHud(g, hud, back)
        if (g == null) DashIntro(k.runBest, speed, { speed = it }, { start() }, back)
        summary?.let { s -> DashResult(s, s.score > bestBefore, { start() }, back) }
    }
}

@Composable private fun DashHud(g: RunWorld, @Suppress("UNUSED_PARAMETER") tick: Int, back: () -> Unit) {
    val fx = LocalSfx.current
    Column(Modifier.fillMaxSize().padding(10.dp), Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Panel, Shape12).border(1.dp, Hair, Shape12).clickable(onClick = back), Alignment.Center) { Text("←", fontSize = 20.sp, color = Ink) }
            Spacer(Modifier.width(10.dp))
            Text("❤️".repeat(g.hearts.coerceIn(0, 3)) + "🖤".repeat(3 - g.hearts.coerceIn(0, 3)), fontSize = 22.sp, modifier = Modifier.weight(1f))
            if (g.combo >= 2) { Chip("🔥 ${g.combo}"); Spacer(Modifier.width(6.dp)) }
            Chip("⭐ ${g.score}")
        }
        Row(Modifier.fillMaxWidth().background(Panel.copy(alpha = .88f), RoundedCornerShape(18.dp)).border(1.dp, Hair, RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(g.item.e, fontSize = 40.sp); Spacer(Modifier.width(10.dp))
            Row(Modifier.weight(1f), Arrangement.spacedBy(6.dp)) {
                g.word.forEachIndexed { i, c ->
                    val now = i == g.idx
                    Box(Modifier.size(40.dp).background(if (i < g.idx) Ok.copy(alpha = .3f) else if (now) LocalPal.current.a1 else Panel2, RoundedCornerShape(10.dp)), Alignment.Center) {
                        Text(c.toString(), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = if (i < g.idx) Ok else if (now) Dark else Muted) }
                }
            }
            Text("${(g.wi + 1).coerceAtMost(g.words.size)}/${g.words.size}", fontSize = 13.sp, color = Muted)
        }
        if (g.shield || g.slowT > 0f) Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) { if (g.shield) Chip("🛡️ shield"); if (g.slowT > 0f) Chip("⏳ slow-mo") }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            listOf("◀" to -1, "▶" to 1).forEach { (t, d) ->
                Box(Modifier.weight(1f).height(64.dp).background(Panel.copy(alpha = .7f), RoundedCornerShape(20.dp)).border(1.dp, Hair, RoundedCornerShape(20.dp)).clickable { g.move(d); fx?.whoosh() }, Alignment.Center) {
                    Text(t, fontSize = 26.sp, color = Ink) }
            }
        }
    }
}

@Composable private fun DashIntro(best: Int, speed: Int, setSpeed: (Int) -> Unit, start: () -> Unit, back: () -> Unit) {
    val pal = LocalPal.current
    Box(Modifier.fillMaxSize().padding(10.dp), Alignment.Center) {
        GCard(Modifier.fillMaxWidth()) {
            Column(Modifier.verticalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally) {
                Mascot("🚀", 56)
                Big("Letter Dash 3D", 28)
                Text("Race down the space road and catch the letters in the right order to spell each word!", fontSize = 14.sp, color = Ink, textAlign = TextAlign.Center)
                Text("🧊 Pick the next letter   👆 Swipe or tap ◀ ▶\n🪨 Dodge rocks   🛡️ Shield   ⏳ Slow-mo   🪙 Coins", fontSize = 13.sp, color = Ink, textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    listOf("🐢 Chill", "🚀 Normal", "⚡ Zoom").forEachIndexed { i, t ->
                        Box(Modifier.weight(1f).background(if (i == speed) pal.a1 else Panel2, RoundedCornerShape(14.dp)).clickable { setSpeed(i) }.padding(vertical = 10.dp), Alignment.Center) {
                            Text(t, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (i == speed) Dark else Ink) } }
                }
                if (best > 0) Text("🏆 Your best: $best", fontWeight = FontWeight.Bold, color = Ink)
                PBtn("Start ▶", start, h = 56.dp)
                TextButton(back) { Text("Back") }
            }
        }
    }
}

@Composable private fun DashResult(s: RunSummary, newBest: Boolean, again: () -> Unit, home: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xAA04080C)).padding(10.dp), Alignment.Center) {
        if (s.stars >= 2) Confetti(s)
        GCard(Modifier.fillMaxWidth()) {
            Column(Modifier.verticalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally) {
                Mascot(if (s.stars == 3) "🏆" else if (s.stars >= 1) "🎉" else "💪", 64)
                Big(if (s.won) (if (s.stars == 3) "Perfect run!" else "You made it!") else if (s.words > 0) "Nice try!" else "Keep going!", 28)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { (1..3).forEach { n -> Pop(n * 220) { Text(if (n <= s.stars) "⭐" else "☆", fontSize = 44.sp) } } }
                Text("${s.words} of ${s.total} words · ${s.letters} letters · best combo ${s.bestCombo}", fontSize = 14.sp, color = Ink, textAlign = TextAlign.Center)
                Text("Score ${s.score}" + if (newBest) "  🏆 New best!" else "", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Chip("⚡ +${s.xp} XP"); Chip("🪙 +${s.coinReward}") }
                PBtn("Play again", again, h = 56.dp)
                TextButton(home) { Text("Home") }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
//  LETTER CUBE 3D
//  Drag to spin a real 3D cube (with inertia and a snap-to-face). Explore the alphabet,
//  or take the challenge: "turn the cube until you see the letter K".
// ---------------------------------------------------------------------------------
@Composable fun LetterCube(v: Voice, fx: Sfx, onStar: () -> Unit, back: () -> Unit) {
    var group by remember { mutableIntStateOf(0) }
    var mode by remember { mutableIntStateOf(0) }
    val letters = remember(group) { List(6) { alphabet[(group * 6 + it) % 26] } }
    var yaw by remember { mutableFloatStateOf(0.6f) }; var tilt by remember { mutableFloatStateOf(0.4f) }
    var vy by remember { mutableFloatStateOf(2.4f) }; var vt by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var front by remember { mutableIntStateOf(0) }; var settled by remember { mutableIntStateOf(-1) }
    var target by remember { mutableIntStateOf(0) }; var round by remember { mutableIntStateOf(0) }; var found by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf("") }

    fun newRound() { target = (0..5).random(); msg = ""; v.say("Turn the cube to find the letter ${letters[target].c}") }
    fun setMode(m: Int) { mode = m; round = 0; found = 0; msg = ""; if (m == 1) newRound() else { settled = -1 } }

    LaunchedEffect(Unit) {
        var last = 0L; val q = (PI / 2.0).toFloat()
        while (true) {
            val now = withFrameNanos { it }
            val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f); last = now
            if (!dragging) {
                yaw += vy * dt; tilt += vt * dt
                val damp = (1f - 2.2f * dt).coerceAtLeast(0f); vy *= damp; vt *= damp
                if (abs(vy) < 0.7f && abs(vt) < 0.7f) {
                    val ty = (yaw / q).roundToInt() * q; val tt = (tilt / q).roundToInt() * q
                    yaw += (ty - yaw) * min(1f, dt * 9f); tilt += (tt - tilt) * min(1f, dt * 9f)
                }
            }
            front = frontFace(CUBE, 0f, yaw, 0f, tilt)
            val still = !dragging && abs(vy) < 0.7f && abs(vt) < 0.7f
            settled = if (still && abs(yaw - (yaw / q).roundToInt() * q) < .02f && abs(tilt - (tilt / q).roundToInt() * q) < .02f) front else -1
        }
    }
    LaunchedEffect(settled) { if (settled >= 0 && mode == 0) { val l = letters[settled]; v.say("${l.c}. ${l.c} for ${l.word}") } }

    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(10.dp)) {
        TopBar("🧊 Letter Cube", back) { if (mode == 1) Chip("🎯 $found/5") }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            listOf("🔍 Explore", "🎯 Challenge").forEachIndexed { i, t ->
                val pal = LocalPal.current
                Box(Modifier.weight(1f).background(if (mode == i) pal.a1 else Panel2, RoundedCornerShape(14.dp)).clickable { setMode(i) }.padding(vertical = 10.dp), Alignment.Center) {
                    Text(t, fontWeight = FontWeight.SemiBold, color = if (mode == i) Dark else Ink) } }
        }
        if (mode == 0) Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            TextButton({ group = (group + 4) % 5; settled = -1 }) { Text("◀ Previous") }
            Chip("${letters.first().c} – ${letters.last().c}")
            TextButton({ group = (group + 1) % 5; settled = -1 }) { Text("Next ▶") }
        }
        GCard(Modifier.fillMaxWidth().weight(1f)) {
            Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(onDragStart = { dragging = true; vy = 0f; vt = 0f }, onDragEnd = { dragging = false }, onDragCancel = { dragging = false }) { ch, d ->
                    ch.consume()
                    yaw -= d.x * 0.011f; tilt -= d.y * 0.011f
                    vy = (vy * .5f + (-d.x * 0.011f * 60f) * .5f).coerceIn(-14f, 14f); vt = (vt * .5f + (-d.y * 0.011f * 60f) * .5f).coerceIn(-14f, 14f)
                }
            }) {
                val s = min(size.width, size.height)
                val cam = Cam(size.width, size.height, hor = size.height * .5f, camH = 0f, back = 0f, fl = s * 1.25f)
                drawOval(Color.Black.copy(alpha = .10f), Offset(size.width * .5f - s * .30f, size.height * .5f + s * .36f), Size(s * .60f, s * .09f))
                val q = drawMesh(cam, place(CUBE, .95f, .95f, .95f, 0f, yaw, 0f, 0f, 0f, 4.2f, tilt), Color.White, edge = Color.White, tint = { cubeCols[it] })
                for (i in 0 until 6) onFace(q[i]) { c -> val l = letters[i]; faceText(c, "${l.c}${l.c.lowercaseChar()}", 50f, 40f, 58f); faceText(c, l.emoji, 50f, 76f, 34f) }
            }
        }
        val fl = letters[front.coerceIn(0, 5)]
        if (mode == 0) {
            GCard(Modifier.fillMaxWidth(), { v.say("${fl.c}. ${fl.c} for ${fl.word}") }) {
                Text("${fl.emoji}  ${fl.c} for ${fl.word}   🔊", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            Text("Drag the cube to spin it. Flick for a big spin!", fontSize = 12.sp, color = Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            PBtn("🎲 Spin!", { vy = (if ((0..1).random() == 0) 1 else -1) * (7f + (0..5).random()); vt = ((-3..3).random()).toFloat() }, h = 52.dp)
        } else if (round < 5) {
            Big(if (msg.isNotEmpty()) msg else "Find the letter  🎯 ${letters[target].c}", 24)
            PBtn("✔ This one!", {
                if (settled < 0) { msg = "Let the cube stop first" }
                else if (front == target) { found++; onStar(); fx.good(); v.say("Yes! ${fl.c} for ${fl.word}!"); round++; if (round < 5) newRound() else { fx.win(); v.say("You are a cube master!"); msg = "" } }
                else { fx.bad(); msg = "That's ${fl.c}. Keep turning!"; v.say("That is ${fl.c}. Keep turning!") }
            }, h = 56.dp)
        } else {
            Big("🎉 Cube master! $found/5", 26)
            PBtn("Play again", { setMode(1) }, h = 52.dp); TextButton(back) { Text("Home") }
        }
    }
}

// ---------------------------------------------------------------------------------
//  3D ZONE hero card for the Home screen: a slowly spinning cube + two big buttons
// ---------------------------------------------------------------------------------
@Composable fun Hero3D(best: Int, onDash: () -> Unit, onCube: () -> Unit) {
    val pal = LocalPal.current
    val spin by rememberInfiniteTransition(label = "hero").animateFloat(0f, 6.2832f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")
    Row(Modifier.fillMaxWidth().clip(Shape20).background(Brush.linearGradient(listOf(Panel, Panel2))).border(1.dp, Hair, Shape20).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(128.dp)) {
            val cam = Cam(size.width, size.height, hor = size.height * .5f, camH = 0f, back = 0f, fl = size.minDimension * 1.45f)
            val q = drawMesh(cam, place(CUBE, .95f, .95f, .95f, 0f, spin, 0f, 0f, 0f, 4.2f, .5f), Color.White, edge = Color.White, tint = { cubeCols[it] })
            val ls = "ABCDEF"
            for (i in 0 until 6) onFace(q[i]) { c -> faceText(c, ls[i].toString(), 50f, 52f, 70f) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), Arrangement.spacedBy(6.dp)) {
            Text("3D ZONE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = pal.a1, letterSpacing = 2.sp)
            Text("Race, spin & spell", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            if (best > 0) Text("Dash best  $best", fontSize = 12.sp, color = Muted)
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                MiniBtn("🚀 Dash", Modifier.weight(1f), listOf(Dark), onDash)
                MiniBtn("🧊 Cube", Modifier.weight(1f), emptyList(), onCube)
            }
        }
    }
}

@Composable fun MiniBtn(t: String, modifier: Modifier, cols: List<Color>, onClick: () -> Unit) =
    Box(modifier.height(42.dp).background(if (cols.size == 1) LocalPal.current.a1 else Panel2, RoundedCornerShape(14.dp)).border(1.dp, if (cols.size == 1) LocalPal.current.a1 else Hair, RoundedCornerShape(14.dp)).clickable(onClick = onClick), Alignment.Center) {
        Text(t, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (cols.size == 1) Dark else Ink) }

package com.margaretm.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class QuizSpec(val kind: String, val title: String, val qs: List<Question>, val timeLimit: Int = 0, val level: Int = 0, val route: String = "")
data class QuizResult(val correct: Int, val total: Int, val combo: Int)

@Composable fun Choice(t: String, state: Int, mod: Modifier, size: Int, onClick: () -> Unit) {
    val bg by animateColorAsState(when (state) { 1 -> Color(0xFF2ECC71); 2 -> Color(0xFFFF5252); else -> Color.White }, label = "ch")
    val sc by animateFloatAsState(when (state) { 1 -> 1.08f; 2 -> .94f; else -> 1f }, spring(dampingRatio = .4f), label = "cs")
    Box(mod.scale(sc).alpha(if (state == 3) .45f else 1f).shadow(6.dp, RoundedCornerShape(22.dp)).background(bg, RoundedCornerShape(22.dp)).clickable(onClick = onClick), Alignment.Center) {
        Text(t, fontSize = size.sp, fontWeight = FontWeight.Bold, color = if (state == 1 || state == 2) Color.White else Ink, textAlign = TextAlign.Center)
    }
}

/** One screen that plays any list of questions: combo, hearts-free, optional countdown. */
@Composable fun QuizScreen(spec: QuizSpec, v: Voice, fx: Sfx, onAnswer: (Question, Boolean) -> Unit, onDone: (QuizResult) -> Unit, back: () -> Unit) {
    val qs = spec.qs; val timed = spec.timeLimit > 0
    var i by remember { mutableIntStateOf(0) }; var correct by remember { mutableIntStateOf(0) }; var answered by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }; var best by remember { mutableIntStateOf(0) }
    var picked by remember(i) { mutableIntStateOf(-1) }; var left by remember { mutableIntStateOf(spec.timeLimit) }
    var fin by remember { mutableStateOf(false) }
    val q = qs[i]
    fun finish() { if (!fin) { fin = true; onDone(QuizResult(correct, answered, best)) } }
    LaunchedEffect(i) { v.say(q.say) }
    if (timed) LaunchedEffect(Unit) { while (left > 0) { delay(1000); left-- }; finish() }
    LaunchedEffect(picked) {
        if (picked >= 0) { delay(if (picked == q.answer) 850 else 1500); if (i < qs.lastIndex) i++ else finish() }
    }
    fun pick(k: Int) {
        if (picked >= 0 || fin) return
        picked = k; answered++; val ok = k == q.answer; onAnswer(q, ok)
        if (ok) { correct++; combo++; best = maxOf(best, combo); fx.good(); v.say(listOf("Yes!", "Great job!", "Super!", "Awesome!", "Brilliant!").random()) }
        else { combo = 0; fx.bad(); v.say(if (q.kind == Kind.ODD) "Not quite!" else "Not quite. It is ${q.choices[q.answer].lowercase()}") }
    }
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(12.dp)) {
        TopBar(spec.title, back) { if (combo >= 2) { Chip("🔥 $combo"); Spacer(Modifier.width(6.dp)) }; if (timed) Chip("⏱ $left") }
        if (!timed) Bar((i + if (picked >= 0) 1 else 0).toFloat() / qs.size)
        GCard(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), Arrangement.spacedBy(10.dp, Alignment.CenterVertically), Alignment.CenterHorizontally) {
                Text(q.prompt, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Ink)
                Big(q.big, if (q.kind == Kind.COUNT) 40 else if (q.kind == Kind.MISSING_LETTER) 48 else 88)
                TextButton({ v.say(q.say) }) { Text("🔊 Hear it again") }
            }
        }
        fun st(k: Int) = when { picked < 0 -> 0; k == q.answer -> 1; k == picked -> 2; else -> 3 }
        val longText = q.choices.any { it.length >= 4 && q.kind != Kind.ODD }
        if (longText) Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) { q.choices.forEachIndexed { k, c -> Choice(c, st(k), Modifier.fillMaxWidth().height(58.dp), 24) { pick(k) } } }
        else q.choices.indices.chunked(if (q.choices.size == 4) 2 else 3).forEach { row ->
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) { row.forEach { k -> Choice(q.choices[k], st(k), Modifier.weight(1f).height(92.dp), if (q.kind == Kind.ODD) 44 else 40) { pick(k) } } }
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable fun ResultScreen(title: String, r: QuizResult, stars: Int, xp: Int, coins: Int, again: () -> Unit, next: (() -> Unit)?, home: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        if (stars >= 2) Confetti(r)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), Arrangement.spacedBy(14.dp, Alignment.CenterVertically), Alignment.CenterHorizontally) {
            Mascot(if (stars == 3) "🏆" else if (stars >= 1) "🎉" else "💪", 90)
            Big(if (stars == 3) "Perfect!" else if (stars >= 1) "Well done!" else "Keep going!", 34)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (1..3).forEach { s -> Pop(s * 250) { Text(if (s <= stars) "⭐" else "☆", fontSize = 56.sp) } } }
            Big("${r.correct} of ${r.total} correct · best combo ${r.combo}", 16)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Chip("⚡ +$xp XP"); Chip("🪙 +$coins") }
            Spacer(Modifier.height(8.dp))
            if (next != null) Btn("Next level ▶") { next() }
            Btn("Play again") { again() }; TextButton(home) { Text("Home") }
        }
    }
}

// ---------- Memory Match: flip picture/word pairs ----------
@Composable fun MemoryMatch(v: Voice, fx: Sfx, onWin: (Int) -> Unit, back: () -> Unit) {
    val cards = remember { bank.shuffled().take(6).flatMap { listOf(Triple(it.w, it.e, false), Triple(it.w, it.w, true)) }.shuffled() }
    var open by remember { mutableStateOf(listOf<Int>()) }; var matched by remember { mutableStateOf(setOf<Int>()) }; var moves by remember { mutableIntStateOf(0) }
    val won = matched.size == cards.size
    LaunchedEffect(Unit) { v.say("Find the picture and its word!") }
    LaunchedEffect(open) {
        if (open.size == 2) { moves++
            if (cards[open[0]].first == cards[open[1]].first) { matched = matched + open; fx.good(); v.say(cards[open[0]].first.lowercase()) } else { delay(900); fx.bad() }
            open = emptyList() }
    }
    LaunchedEffect(won) { if (won) { fx.win(); v.say("You found them all in $moves moves!"); onWin(moves) } }
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(10.dp)) {
        TopBar("Memory Match", back) { Chip("👣 $moves") }
        cards.indices.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), Arrangement.spacedBy(10.dp)) { row.forEach { idx ->
                val face = idx in open || idx in matched
                val rot by animateFloatAsState(if (face) 180f else 0f, spring(dampingRatio = .7f), label = "flip")
                Box(Modifier.weight(1f).fillMaxHeight().graphicsLayer { rotationY = rot; cameraDistance = 14f * density }.shadow(6.dp, RoundedCornerShape(18.dp))
                    .background(if (idx in matched) Color(0xFFBFF3D3) else if (rot > 90f) Color.White else LocalPal.current.a1, RoundedCornerShape(18.dp))
                    .clickable { if (!face && open.size < 2 && !won) { open = open + idx } }, Alignment.Center) {
                    if (rot > 90f) Box(Modifier.graphicsLayer { rotationY = 180f }) { val c = cards[idx]; Text(c.second, fontSize = if (c.third) 20.sp else 40.sp, fontWeight = FontWeight.Bold, color = Ink) }
                    else Text("?", fontSize = 34.sp, color = Color.White, fontWeight = FontWeight.Bold)
                } } }
        }
        if (won) { Big("🎉 All matched in $moves moves!", 22); Btn("Done") { back() } }
    }
}

// ---------- Sentence Builder: tap words in order ----------
@Composable fun SentenceBuilder(v: Voice, fx: Sfx, onDone: (Int) -> Unit, back: () -> Unit) {
    val rounds = remember { val tpl = listOf("I SEE A %s", "THE %s IS BIG", "I LIKE THE %s", "WE HAVE A %s", "LOOK AT THE %s")
        bank.filter { it.w.length <= 5 }.shuffled().take(5).mapIndexed { n, it -> it to tpl[n % tpl.size].format(it.w).split(" ") } }
    var n by remember { mutableIntStateOf(0) }; var done by remember(n) { mutableIntStateOf(0) }; var oops by remember(n) { mutableIntStateOf(0) }
    val (item, words) = rounds[n]; val order = remember(n) { words.indices.shuffled() }
    val fin = done == words.size
    LaunchedEffect(n) { v.say("Build the sentence. Tap the words in order.") }
    LaunchedEffect(fin) { if (fin) { fx.win(); v.say(words.joinToString(" ").lowercase()) } }
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        TopBar("Sentence Builder", back) { Chip("${n + 1}/${rounds.size}") }
        Big(item.e, 96)
        GCard(Modifier.fillMaxWidth()) { Text(words.mapIndexed { k, w -> if (k < done) w else "___" }.joinToString("  "), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        if (!fin) {
            order.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) { row.forEach { k ->
                Choice(words[k], if (k < done) 3 else 0, Modifier.weight(1f).height(64.dp), 24) {
                    if (k == done) { done++; fx.good(); v.say(words[k].lowercase()) } else { oops++; fx.bad(); v.say("Which word comes next?") } } } } }
        } else {
            Big("🎉 ⭐ 🎉", 44)
            Btn(if (n < rounds.lastIndex) "Next sentence" else "Finish") { if (n < rounds.lastIndex) n++ else onDone(rounds.size) }
        }
    }
}

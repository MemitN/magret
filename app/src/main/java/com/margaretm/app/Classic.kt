package com.margaretm.app

import androidx.compose.foundation.border
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

// ---------- Content (move to JSON assets later) ----------
data class Letter(val c: Char, val word: String, val emoji: String)
val alphabet = listOf(
    Letter('A',"Apple","🍎"),Letter('B',"Balloon","🎈"),Letter('C',"Car","🚗"),Letter('D',"Dog","🐶"),
    Letter('E',"Elephant","🐘"),Letter('F',"Fish","🐠"),Letter('G',"Giraffe","🦒"),Letter('H',"House","🏠"),
    Letter('I',"Ice cream","🍦"),Letter('J',"Juice","🧃"),Letter('K',"Kite","🪁"),Letter('L',"Lion","🦁"),
    Letter('M',"Monkey","🐵"),Letter('N',"Nest","🪺"),Letter('O',"Orange","🍊"),Letter('P',"Penguin","🐧"),
    Letter('Q',"Queen","👑"),Letter('R',"Robot","🤖"),Letter('S',"Star","⭐"),Letter('T',"Teddy","🧸"),
    Letter('U',"Umbrella","☂️"),Letter('V',"Violin","🎻"),Letter('W',"Whale","🐋"),Letter('X',"Box","🎁"),
    Letter('Y',"Yo-yo","🪀"),Letter('Z',"Zebra","🦓"))
data class Word(val w: String, val emoji: String)
val words3 = listOf(Word("CAT","🐱"),Word("DOG","🐶"),Word("SUN","☀️"),Word("CAR","🚗"),Word("BUG","🐞"))
val words4 = listOf(Word("BOOK","📚"),Word("TREE","🌳"),Word("MILK","🥛"),Word("FISH","🐠"),Word("DOOR","🚪"),Word("HAND","✋"))

@Composable fun Big(t: String, size: Int = 28) = Text(t, fontSize = (if (size >= 24) size - 4 else size).sp, color = if (size >= 24) Ink else Muted, fontWeight = if (size >= 24) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())




@Composable fun NameGame(name: String, v: Voice, done: () -> Unit) {
    val vowels = "aeiou"
    val idx = name.indices.filter { name[it].lowercaseChar() in vowels }.ifEmpty { listOf(name.lastIndex) }
    val shown = name.mapIndexed { i, c -> if (i in idx) '_' else c }.joinToString(" ")
    val answer = idx.map { name[it] }.joinToString("")
    var input by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { v.say("Let's test your memory. Type the missing letters in your name.") }
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {
        Big("Let's test your memory!"); Big(shown, 56)
        if (!ok) {
            OutlinedTextField(input, { input = it.take(idx.size) }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Missing letters, in order") })
            Btn("Check") {
                if (input.equals(answer, true)) { ok = true; v.say("You are a genius, $name! Let's start by learning our alphabets.") }
                else v.say("Oops, try again with me!")
            }
        } else { Big("🌟 You are a genius, $name! 🌟"); Btn("Let's go!") { done() } }
    }
}


@Composable fun AlphabetWorld(v: Voice, back: () -> Unit) {
    var i by remember { mutableIntStateOf(0) }
    val l = alphabet[i]
    LaunchedEffect(i) { v.say("${l.c}. ${l.c} for ${l.word}") }
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(12.dp)) {
        TopBar("Alphabet World", back) { Chip("${i + 1}/26") }
        Bar((i + 1) / 26f)
        GCard(Modifier.fillMaxWidth().weight(1f), { v.say("${l.c} for ${l.word}") }) {
            Column(Modifier.fillMaxSize(), Arrangement.spacedBy(6.dp, Alignment.CenterVertically), Alignment.CenterHorizontally) {
                Big("${l.c}${l.c.lowercaseChar()}", 110)
                key(i) { Pop { Text(l.emoji, fontSize = 110.sp) } }
                Big(l.word, 32)
                Text("🔊 tap the card to hear it", fontSize = 12.sp, color = Muted)
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            PBtn("◀", { if (i > 0) i-- }, Modifier.weight(1f), 56.dp, ghost = true)
            PBtn(if (i < 25) "▶" else "Done ✔", { if (i < 25) i++ else back() }, Modifier.weight(1f), 56.dp)
        }
    }
}

@Composable fun WordBuilder(name: String, list: List<Word>, v: Voice, star: () -> Unit, back: () -> Unit, hear: Boolean = false) {
    var n by remember { mutableIntStateOf(0) }
    val w = list[n]
    var placed by remember(n) { mutableStateOf("") }
    val bank = remember(n) { (w.w.toList() + ('A'..'Z').filter { it !in w.w }.shuffled().take(3)).shuffled() }
    var used by remember(n) { mutableStateOf(setOf<Int>()) }
    val done = placed == w.w
    LaunchedEffect(n) { v.say(if (hear) "Listen. ${w.w.toList().joinToString(" ")}. ${w.w.lowercase()}. Can you build it?" else "Let's build ${w.w.lowercase()}! Find the first letter.") }
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        if (hear && !done) { Big("👂", 110); Button({ v.say("${w.w.lowercase()}") }, Modifier.fillMaxWidth()) { Text("🔊 Hear the word again") } } else Big(w.emoji, 110)
        Big(w.w.indices.joinToString("  ") { if (it < placed.length) placed[it].toString() else "_" }, 52)
        if (!done) {
            LazyVerticalGrid(GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bank.indices.toList()) { k ->
                    val used1 = k in used
                    Box(Modifier.aspectRatio(1f).background(if (used1) Panel else LocalPal.current.a1, RoundedCornerShape(16.dp))
                        .clickable(enabled = !used1) {
                            if (bank[k] == w.w[placed.length]) {
                                placed += bank[k]; used = used + k; v.say(bank[k].toString())
                                if (placed == w.w) { star(); v.say("You did it, $name! ${w.w.toList().joinToString(" ")} spells ${w.w.lowercase()}! One star!") }
                            } else v.say("Oops, try again with me!")
                        }, Alignment.Center) { Text(bank[k].toString(), fontSize = 36.sp, color = Dark) }
                }
            }
        } else {
            Big("🎉 ⭐ 🎉", 48)
            Btn(if (n < list.lastIndex) "Next word" else "Finish") { if (n < list.lastIndex) n++ else back() }
        }
        TextButton(back) { Text("Home") }
    }
}

// ---------- Stories (Family theme) ----------
data class Story(val title: String, val emoji: String, val text: String, val q: String, val right: String, val wrong: String, val praise: String)
val stories = listOf(
    Story("Mom and Dad","👨‍👩‍👦","This is MOM. This is DAD. MOM and DAD love YOU.","Who loves you?","MOM and DAD","A BUG","Yes! Mom and Dad love you!"),
    Story("I can help","🧹","I see DAD. I see a BROOM. I can HELP DAD.","Who can help Dad?","I","A CAT","Correct! You can help Dad! You are kind!"),
    Story("We eat","🍽️","The POT is HOT. MOM can COOK. WE eat.","Who can cook?","MOM","A DOG","Right! Mom can cook! Yummy!"),
    Story("My home","🏠","This is my HOME. My FAMILY is HERE. We are SAFE.","Where is your family?","AT HOME","AT SCHOOL","Yes! Your family is at home!"),
    Story("Grandma","👵","This is GRANDMA. GRANDMA can READ. I LIKE GRANDMA.","What can Grandma do?","READ","FLY","Amazing! Grandma can read!"),
    Story("We play","⚽","It is TIME to PLAY. DAD has a BALL. WE can KICK.","What do we play with?","A BALL","MILK","A ball! We can kick!"))

@Composable fun Stories(name: String, v: Voice, star: () -> Unit, back: () -> Unit) {
    var i by remember { mutableIntStateOf(0) }
    var answered by remember(i) { mutableStateOf(false) }
    val st = stories[i]
    val opts = remember(i) { listOf(st.right, st.wrong).shuffled() }
    LaunchedEffect(i) { v.say(st.text) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        Big("${i + 1}/6  ${st.title}", 26); Big(st.emoji, 100); Big(st.text, 30)
        Button({ v.say(st.text) }, Modifier.fillMaxWidth()) { Text("🔊 Read to me") }
        Big(st.q, 26)
        opts.forEach { o -> Btn(o) {
            if (answered) return@Btn
            if (o == st.right) { answered = true; star(); v.say(st.praise + " One star!") } else v.say("Try again! Listen. ${st.text}")
        } }
        if (answered) { Big("⭐ $name, well done!", 28); Btn(if (i < 5) "Next story" else "Finish") { if (i < 5) i++ else back() } }
        TextButton(back) { Text("Home") }
    }
}

// ---------- Tracing ----------
@Composable fun Trace(v: Voice, star: () -> Unit, back: () -> Unit) {
    var i by remember { mutableIntStateOf(0) }
    val l = alphabet[i]
    val ink = LocalPal.current.a1
    val strokes = remember(i) { mutableStateListOf<List<Offset>>() }
    var cur by remember(i) { mutableStateOf(listOf<Offset>()) }
    LaunchedEffect(i) { v.say("Trace big ${l.c}, and small ${l.c.lowercaseChar()}, with your finger. ${l.c} for ${l.word}") }
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(12.dp)) {
        Big("Trace ${l.c}${l.c.lowercaseChar()}  ${l.emoji}", 30)
        Box(Modifier.fillMaxWidth().weight(1f).background(Panel, RoundedCornerShape(24.dp))) {
            Text("${l.c}${l.c.lowercaseChar()}", fontSize = 200.sp, color = Hair, modifier = Modifier.align(Alignment.Center))
            Canvas(Modifier.fillMaxSize().pointerInput(i) {
                detectDragGestures(onDragStart = { cur = listOf(it) }, onDragEnd = { strokes.add(cur); cur = listOf() }, onDrag = { c, _ -> cur = cur + c.position })
            }) {
                (strokes + listOf(cur)).forEach { s ->
                    if (s.size > 1) { val p = Path(); p.moveTo(s[0].x, s[0].y); s.drop(1).forEach { p.lineTo(it.x, it.y) }
                        drawPath(p, ink, style = Stroke(28f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            PBtn("Clear", { strokes.clear() }, Modifier.weight(1f), 52.dp, ghost = true)
            PBtn("Done ⭐", { star(); v.say("Beautiful! One star!"); if (i < 25) i++ else back() }, Modifier.weight(2f), 52.dp)
        }
        TextButton(back) { Text("Home") }
    }
}

// ---------- Alphabet basket game: tap a letter to put it in the basket ----------
@Composable fun Basket(name: String, v: Voice, star: () -> Unit, back: () -> Unit) {
    var spilled by remember { mutableStateOf(false) }
    val round = remember { alphabet.shuffled().take(6) }
    var inBasket by remember { mutableStateOf(setOf<Char>()) }
    LaunchedEffect(Unit) { v.say("Hi $name! Tap the basket to tilt it!") }
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
        Big("🧺 ${inBasket.size}/6", 40)
        if (!spilled) { Big("👇 Tap to tilt the basket", 24); Btn("🧺 TILT!") { spilled = true; v.say("Oh no! The alphabet spilled! Put the letters back in the basket!") } }
        else {
            LazyVerticalGrid(GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(round) { l ->
                    val inn = l.c in inBasket
                    Box(Modifier.aspectRatio(1f).background(if (inn) Panel else Panel2, RoundedCornerShape(20.dp)).clickable(enabled = !inn) {
                        inBasket = inBasket + l.c; v.say("${l.c} for ${l.word}")
                        if (inBasket.size == 6) { star(); v.say("Good job $name! You cleaned up! One star!") }
                    }, Alignment.Center) { Text(if (inn) "✔" else "${l.emoji}\n${l.c}${l.c.lowercaseChar()}", fontSize = 30.sp, textAlign = TextAlign.Center) }
                }
            }
            if (inBasket.size == 6) { Big("🎉 Good job! ⭐", 32); Btn("Play again") { spilled = false; inBasket = setOf() } }
        }
        TextButton(back) { Text("Home") }
    }
}

@Composable fun Race(v: Voice, onEnd: (Int) -> Unit, back: () -> Unit, board: () -> Unit) {
    var left by remember { mutableIntStateOf(30) }; var score by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    val pool = remember { (words3 + words4).shuffled() }
    var wi by remember { mutableIntStateOf(0) }
    val w = pool[wi % pool.size]
    var placed by remember(wi) { mutableStateOf("") }
    val bank = remember(wi) { (w.w.toList() + ('A'..'Z').filter { it !in w.w }.shuffled().take(2)).shuffled() }
    LaunchedEffect(Unit) { v.say("Word race! Build as many words as you can. Go!"); while (left > 0) { delay(1000); left-- }; finished = true; onEnd(score); v.say("Time's up! You built $score words!") }
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Chip("⏱ $left"); Chip("✅ $score") }
        if (!finished) {
            Big(w.emoji, 100); Big(w.w.indices.joinToString("  ") { if (it < placed.length) placed[it].toString() else "_" }, 48)
            LazyVerticalGrid(GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bank.indices.toList()) { k ->
                    Box(Modifier.aspectRatio(1.4f).background(Brush.linearGradient(listOf(Panel2, Panel2)), RoundedCornerShape(18.dp)).clickable {
                        if (bank[k] == w.w[placed.length]) { placed += bank[k]; if (placed == w.w) { score++; wi++ } }
                    }, Alignment.Center) { Text(bank[k].toString(), fontSize = 34.sp, color = Ink, fontWeight = FontWeight.Bold) }
                }
            }
        } else {
            Big("🏁 $score words!", 40); Big("⭐ +$score stars", 24)
            Btn("🏆 Leaderboard") { board() }; Btn("Home") { back() }
        }
    }
}


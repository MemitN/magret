package com.margaretm.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable private fun Scroll(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), Arrangement.spacedBy(12.dp), content = content)

// ---------- Account / profiles ----------
@Composable fun Auth(st: Store, ok: () -> Unit) {
    val signup = !st.hasAccount()
    var email by remember { mutableStateOf("") }; var pin by remember { mutableStateOf("") }; var err by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
        Box(Modifier.fillMaxWidth(), Alignment.Center) { CritterBounce(0, 110.dp) }; Big("Margaret M", 38); Big(if (signup) "Create parent account" else "Parent login", 18)
        OutlinedTextField(email, { email = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Parent email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text("4-digit PIN") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
        if (err.isNotEmpty()) Text(err, color = Warn)
        Btn(if (signup) "Create account" else "Log in") {
            if (!email.contains("@") || pin.length != 4) err = "Enter a valid email and a 4-digit PIN"
            else if (signup) { st.signUp(email, pin); ok() } else if (st.login(email, pin)) ok() else err = "Wrong email or PIN"
        }
    }
}

@Composable fun Profiles(kids: List<Profile>, pick: (String) -> Unit, add: () -> Unit, out: () -> Unit) = Scroll {
    Big("Who is playing?", 32)
    kids.forEach { k -> GCard(Modifier.fillMaxWidth(), { pick(k.id) }) { Row(verticalAlignment = Alignment.CenterVertically) {
        Text(k.av, fontSize = 46.sp); Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) { Text(k.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink); Text("Level ${k.level} · ${rankName(k.level)}", fontSize = 13.sp, color = Ink)
            Text("⭐ ${k.stars}   🔥 ${k.streak}   🪙 ${k.coins}", fontSize = 14.sp, color = Ink) } } } }
    Btn("➕ Add a child") { add() }; TextButton(out) { Text("Log out parent") }
}

@Composable fun AddKid(st: Store, save: (String, String) -> Unit, back: () -> Unit) {
    var n by remember { mutableStateOf("") }; var a by remember { mutableStateOf(kidAvatars[0]) }
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(18.dp, Alignment.CenterVertically)) {
        Big("New player", 32)
        OutlinedTextField(n, { n = it.filter(Char::isLetter).take(12) }, Modifier.fillMaxWidth(), label = { Text("Child's name") }, singleLine = true)
        Big("Pick a friend", 18)
        kidAvatars.take(6).let { Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) { it.forEach { e ->
            Text(e, fontSize = 36.sp, modifier = Modifier.background(if (e == a) LocalPal.current.a1.copy(alpha = .25f) else Color.Transparent, RoundedCornerShape(16.dp)).clickable { a = e }.padding(6.dp)) } } }
        Btn("Start") { if (n.isNotBlank()) save(n.trim().lowercase().replaceFirstChar { it.uppercase() }, a) }
        TextButton(back) { Text("Back") }
    }
}

// ---------- Home ----------
data class Tl(val e: String, val t: String, val s: String, val route: String)
@Composable fun HomeScreen(k: Profile, goal: Int, v: Voice, openChest: () -> Int, go: (String) -> Unit) {
    val fx = LocalSfx.current
    LaunchedEffect(Unit) { v.say("Hi ${k.name}! What shall we play?") }
    val today = dayKey(System.currentTimeMillis()); val dailyDone = k.dailyDay == today
    val doneToday = k.today(); val weak = weakWords(k.mastery, 6).size
    val next = adventure.firstOrNull { (k.levels[it.n] ?: 0) == 0 }
    val wod = bank[(dailySeed(today) % bank.size).toInt()]
    var chestMsg by remember { mutableStateOf("") }
    val chestReady = k.chestDay != today
    val practice = Kind.values().map { Tl(it.emoji, it.title, "", "practice:${it.name}") }
    val games = listOf(Tl("🧠", "Memory Match", "flip & find", "memory"), Tl("✏️", "Sentences", "build it", "sentence"), Tl("⚡", "Lightning", "45 seconds", "lightning"),
        Tl("🔁", "Review", if (weak > 0) "$weak words to polish" else "smart practice", "review"), Tl("🏁", "Word Race", "30 seconds", "race"), Tl("👂", "Hear & Build", "listen first", "hear"))
    val creative = listOf(Tl("🎨", "Paint Studio", "draw & stick", "paint"), Tl("📖", "Story Time", "", "stories"), Tl("🔤", "Alphabet World", "A to Z", "alphabet"),
        Tl("✍️", "Trace Letters", "", "trace"), Tl("🧺", "Alphabet Basket", "", "basket"), Tl("🐱", "3-letter words", "", "words3"), Tl("📚", "4-letter words", "", "words4"),
        Tl("🏆", "Leaderboard", "", "board"), Tl("👪", "Switch player", "", "profiles"))
    @Composable fun grid(l: List<Tl>, off: Int) = l.chunked(2).forEachIndexed { r, row -> Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
        row.forEachIndexed { c, g -> Tile(g.e, g.t, g.s, emptyList(), Modifier.weight(1f)) { go(g.route) } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } }
    Scroll {
        // progress + primary actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LEVEL ${k.level}", fontSize = 11.sp, color = Muted, letterSpacing = 1.6.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Text("${k.xp} XP", fontSize = 11.sp, color = Muted) }
        Bar(levelProgress(k.xp), Modifier.padding(bottom = 2.dp))
        if (next != null) HeroCard("Continue", "${next.world.substringBefore(' ')} · level ${next.n}", 0) { go("level:${next.n}") }
        HeroCard(if (dailyDone) "Daily challenge ✔" else "Daily challenge", if (dailyDone) "Done today · play again for fun" else "Bonus stars and coins today", 6, light = true) { go("daily") }
        // today: goal | gift | word
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), Arrangement.spacedBy(10.dp)) {
            GCard(Modifier.weight(1.25f).fillMaxHeight()) { Text("TODAY", fontSize = 11.sp, color = Muted, letterSpacing = 1.6.sp); Spacer(Modifier.height(4.dp))
                Text("$doneToday / $goal", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Ink); Text(if (doneToday >= goal) "Goal reached" else "answers", fontSize = 12.sp, color = Muted)
                Spacer(Modifier.weight(1f)); Bar((doneToday.toFloat() / goal)) }
            GCard(Modifier.weight(1f).fillMaxHeight(), { if (chestReady) { val c = openChest(); chestMsg = "+$c 🪙"; fx?.open(); v.say("You found $c coins!") } }) {
                Text("GIFT", fontSize = 11.sp, color = Muted, letterSpacing = 1.6.sp); Spacer(Modifier.weight(1f))
                if (chestReady) Mascot("🎁", 30) else Text("✓", fontSize = 28.sp, color = LocalPal.current.a1)
                Text(if (chestMsg.isNotEmpty()) chestMsg else if (chestReady) "Tap to open" else "Tomorrow", fontSize = 12.sp, color = Muted) }
            GCard(Modifier.weight(1f).fillMaxHeight(), { v.say("Word of the day: ${wod.w.lowercase()}. ${wod.w.toList().joinToString(" ")}. ${wod.w.lowercase()}") }) {
                Text("WORD", fontSize = 11.sp, color = Muted, letterSpacing = 1.6.sp); Spacer(Modifier.weight(1f))
                Text(wod.e, fontSize = 28.sp); Text(wod.w, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink, maxLines = 1) }
        }
        Hero3D(k.runBest, { go("runner") }, { go("cube") })
        Section("Learn"); grid(practice, 0)
        Section("Play"); grid(games, 1)
        Section("Create & explore"); grid(creative, 2)
        Spacer(Modifier.height(8.dp))
    }
}

// ---------- Adventure map ----------
@Composable fun MapScreen(k: Profile, play: (Int) -> Unit) {
    val pulse by rememberInfiniteTransition(label = "node").animateFloat(1f, 1.16f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "p")
    val cur = adventure.firstOrNull { (k.levels[it.n] ?: 0) == 0 }?.n
    Scroll {
        val done = k.levels.count { it.value >= 1 }
        Text("$done / ${adventure.size} levels · ${k.levels.values.sum()} ★", fontSize = 13.sp, color = Muted)
        Bar(done.toFloat() / adventure.size)
        val pal = LocalPal.current
        worlds.forEachIndexed { w, name ->
            val got = adventure.count { it.n in (w * 8 + 1)..(w * 8 + 8) && (k.levels[it.n] ?: 0) >= 1 }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Muted, letterSpacing = 1.6.sp, modifier = Modifier.weight(1f))
                Text("$got/8", fontSize = 12.sp, color = if (got == 8) pal.a1 else Muted) }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Hair))
            adventure.filter { it.n in (w * 8 + 1)..(w * 8 + 8) }.chunked(4).forEachIndexed { r, row ->
                val ordered = if (r % 2 == 0) row else row.reversed()
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), Arrangement.SpaceEvenly) { ordered.forEach { lv ->
                    val s = k.levels[lv.n] ?: 0; val open = lv.n == 1 || (k.levels[lv.n - 1] ?: 0) >= 1
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp).alpha(if (open) 1f else .4f)) {
                        val ring = if (lv.n == cur) pal.a1 else Hair
                        Box(Modifier.size(58.dp).scale(if (lv.n == cur) pulse else 1f).background(if (s > 0) pal.a1 else Panel2, CircleShape).border(if (lv.n == cur) 2.dp else 1.dp, ring, CircleShape)
                            .clickable(enabled = open) { play(lv.n) }, Alignment.Center) { Text(if (open) "${lv.n}" else "·", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = if (s > 0) Dark else Ink) }
                        Text(if (open) "★".repeat(s) + "☆".repeat(3 - s) else "", fontSize = 11.sp, color = pal.a1)
                        Text(if (lv.n % 8 == 0) "👑" else lv.kind.emoji, fontSize = 13.sp)
                    } } }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ---------- Badges ----------
@Composable fun AwardsScreen(k: Profile) {
    var sel by remember { mutableStateOf<Badge?>(null) }
    Scroll {
        Text("${k.badges.size} of ${badges.size} collected", fontSize = 13.sp, color = Muted); Bar(k.badges.size.toFloat() / badges.size)
        sel?.let { b -> GCard(Modifier.fillMaxWidth()) { Text("${b.emoji} ${b.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink); Text(b.desc + if (b.id in k.badges) " ✔" else "", color = Ink) } }
        badges.chunked(4).forEach { row -> Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            row.forEach { b -> val got = b.id in k.badges
                Column(Modifier.weight(1f).alpha(if (got) 1f else .3f).clickable { sel = b }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(b.emoji, fontSize = 34.sp); Text(b.name, fontSize = 10.sp, textAlign = TextAlign.Center, color = Ink, maxLines = 2) } }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) } } }
    }
}

// ---------- Shop ----------
@Composable fun ShopScreen(k: Profile, v: Voice, buy: (Int) -> Boolean, equipAv: (String) -> Unit, buyAv: (String, Int) -> Boolean, setTheme: (Int) -> Unit) {
    var msg by remember { mutableStateOf("") }
    Scroll {
        if (msg.isNotEmpty()) Text(msg, color = Ink, fontWeight = FontWeight.Bold)
        Section("Themes")
        pals.forEachIndexed { i, p -> val own = i in k.ownedTheme
            GCard(Modifier.fillMaxWidth(), { if (own) { setTheme(i); msg = "${p.name} theme on!" } else msg = if (buy(i)) "Unlocked ${p.name}! 🎉" else "Need ${p.price - k.coins} more coins" }) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(p.emoji, fontSize = 32.sp); Spacer(Modifier.width(12.dp))
                    Text(p.name, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Box(Modifier.size(22.dp).background(p.a1, CircleShape)); Spacer(Modifier.width(4.dp)); Box(Modifier.size(22.dp).background(p.a2, CircleShape)); Spacer(Modifier.width(12.dp))
                    Text(if (k.theme == i) "✔ On" else if (own) "Use" else "🪙 ${p.price}", fontWeight = FontWeight.Bold, color = Ink) } } }
        Section("Friends")
        kidAvatars.chunked(4).forEach { row -> Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { row.forEach { a ->
            val own = a in k.ownedAv || (avatarPrice[a] ?: 0) == 0; val price = avatarPrice[a] ?: 0
            Column(Modifier.weight(1f).background(if (k.av == a) LocalPal.current.a1.copy(alpha = .22f) else Panel2, RoundedCornerShape(18.dp)).clickable {
                if (own) { equipAv(a); msg = "New friend picked!" } else msg = if (buyAv(a, price)) "Unlocked $a! 🎉" else "Need ${price - k.coins} more coins" }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(a, fontSize = 36.sp); Text(if (own) (if (k.av == a) "✔" else "own") else "🪙 $price", fontSize = 12.sp, color = Ink) } } } }
        Spacer(Modifier.height(8.dp))
    }
}

// ---------- Parent zone ----------
@Composable fun ParentScreen(st: Store, kids: List<Profile>, cfg: Settings, voiceNames: List<String>, testVoice: () -> Unit, setCfg: (Settings) -> Unit, switchKid: () -> Unit, deleteKid: (String) -> Unit, logout: () -> Unit, refresh: () -> Unit) {
    var ok by remember { mutableStateOf(false) }; var pin by remember { mutableStateOf("") }; var err by remember { mutableStateOf(false) }
    if (!ok) { Column(Modifier.fillMaxSize(), Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
        Big("👪 Parent zone", 30); Big("Enter your PIN to continue", 16)
        OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(4); err = false }, Modifier.fillMaxWidth(), label = { Text("4-digit PIN") }, singleLine = true, isError = err,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
        Btn("Unlock") { if (st.checkPin(pin)) ok = true else err = true } }; return }
    var newPin by remember { mutableStateOf("") }; var confirmDel by remember { mutableStateOf<String?>(null) }
    Scroll {
        Big("👪 Parent zone", 30)
        Section("Settings")
        GCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("🔊 Voice", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold); Switch(cfg.voiceOn, { setCfg(cfg.copy(voiceOn = it)) }) }
            Text("Speaking speed", color = Ink); Slider(cfg.rate, { setCfg(cfg.copy(rate = it)) }, valueRange = 0.4f..1.2f)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("🎵 Background music", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold); Switch(cfg.music, { setCfg(cfg.copy(music = it)) }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("🔔 Sound effects", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold); Switch(cfg.sound, { setCfg(cfg.copy(sound = it)) }) }
            Text("Voice style (tap to hear)", color = Ink)
            Row(Modifier.horizontalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp)) {
                listOf("Auto") .plus(voiceNames).forEachIndexed { i, n ->
                    Box(Modifier.background(if (cfg.voiceIdx == i) LocalPal.current.a1 else Panel2, RoundedCornerShape(16.dp)).clickable { setCfg(cfg.copy(voiceIdx = i)); testVoice() }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(n, color = if (cfg.voiceIdx == i) Dark else Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold) } } }
            Text("Daily goal (answers per day)", color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(5, 10, 20, 40).forEach { g ->
                Box(Modifier.background(if (cfg.goal == g) LocalPal.current.a1 else Panel2, RoundedCornerShape(16.dp)).clickable { setCfg(cfg.copy(goal = g)) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("$g", color = if (cfg.goal == g) Dark else Ink, fontWeight = FontWeight.Bold) } } }
        }
        Section("Progress")
        kids.forEach { k ->
            GCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(k.av, fontSize = 34.sp); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(k.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink); Text("Level ${k.level} · ${rankName(k.level)}", fontSize = 12.sp, color = Ink) } }
                Spacer(Modifier.height(6.dp))
                Text("Answered ${k.answered} · accuracy ${k.accuracy()}% · ${k.mastery.count { it.value >= 3 }} words mastered · ${k.badges.size} badges", fontSize = 13.sp, color = Ink)
                Spacer(Modifier.height(8.dp)); WeekChart(k)
                val weak = weakWords(k.mastery, 5)
                if (weak.isNotEmpty()) Text("Needs practice: " + weak.joinToString(", ") { it.lowercase() }, fontSize = 13.sp, color = Ink, fontWeight = FontWeight.Bold)
                if (confirmDel == k.id) Row { TextButton({ deleteKid(k.id); confirmDel = null }) { Text("Yes, delete ${k.name}", color = Warn) }; TextButton({ confirmDel = null }) { Text("Cancel") } }
                else TextButton({ confirmDel = k.id }) { Text("Delete player", color = Warn) }
            }
        }
        Section("Security")
        OutlinedTextField(newPin, { newPin = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text("New 4-digit PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
        PBtn("Change PIN", { if (newPin.length == 4) { st.changePin(newPin); newPin = "" } }, h = 52.dp)
        PBtn("Switch player", { switchKid() }, h = 52.dp, ghost = true)
        TextButton(logout) { Text("Log out parent") }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable fun WeekChart(k: Profile) {
    val pal = LocalPal.current; val now = System.currentTimeMillis()
    val days = (6 downTo 0).map { dayKey(now - it * 86400000L) }; val vals = days.map { k.history[it] ?: 0 }; val mx = maxOf(vals.max(), 1)
    Column { Text("Last 7 days", fontSize = 11.sp, color = Ink)
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val w = size.width / 7f
            vals.forEachIndexed { i, v -> val h = size.height * v / mx
                drawRoundRect(Brush.verticalGradient(listOf(pal.a2, pal.a1)), Offset(i * w + w * .2f, size.height - h), Size(w * .6f, maxOf(h, 4f)), CornerRadius(8f)) }
        } }
}

@Composable fun Board(kids: List<Profile>, me: String?, back: () -> Unit) {
    val sorted = kids.sortedWith(compareByDescending<Profile> { it.xp }.thenByDescending { it.stars })
    Scroll {
        Big("🏆 Leaderboard", 32); Big("Ranked by XP · best Word Race ${sorted.maxOfOrNull { it.best } ?: 0}", 13)
        sorted.forEachIndexed { i, k -> Row(Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)).background(if (k.id == me) LocalPal.current.a1.copy(alpha = .22f) else Panel, RoundedCornerShape(20.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(listOf("🥇", "🥈", "🥉").getOrElse(i) { "${i + 1}" }, fontSize = 28.sp); Spacer(Modifier.width(10.dp)); Text(k.av, fontSize = 32.sp); Spacer(Modifier.width(10.dp))
            Text(k.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f)); Text("Lv ${k.level} · ⚡${lbl(k.xp)} · 🏁${k.best}", fontSize = 13.sp, color = Ink) } }
        Btn("Back") { back() }
    }
}

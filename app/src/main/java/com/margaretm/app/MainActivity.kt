package com.margaretm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContent { MaterialTheme { App() } }
    }
}

data class Outcome(val spec: QuizSpec, val r: QuizResult, val stars: Int, val xp: Int, val coins: Int)
private val mainTabs = setOf("home", "map", "awards", "shop", "parent")

@Composable fun App() {
    val ctx = LocalContext.current
    val st = remember { Store(ctx) }; val voice = remember { Voice(ctx) }; val fx = remember { Sfx() }
    DisposableEffect(Unit) { onDispose { voice.close(); fx.close() } }
    var cfg by remember { mutableStateOf(st.settings) }
    LaunchedEffect(cfg) { voice.on = cfg.voiceOn; voice.rate = cfg.rate; fx.on = cfg.sound }
    var tick by remember { mutableIntStateOf(0) }
    var screen by remember { mutableStateOf(if (st.activeId != null && st.hasAccount() && st.profiles().any { it.id == st.activeId }) "home" else if (st.hasAccount()) "login" else "auth") }
    var quiz by remember { mutableStateOf<QuizSpec?>(null) }
    var outcome by remember { mutableStateOf<Outcome?>(null) }
    var fresh by remember { mutableStateOf<List<Badge>>(emptyList()) }
    val kids = remember(tick) { st.profiles() }
    val me = kids.firstOrNull { it.id == st.activeId }
    val pal = pals[(me?.theme ?: 0).coerceIn(0, pals.lastIndex)]
    fun go(s: String) { screen = s }

    /** Single place where XP, coins, stars and badges are granted. */
    fun reward(xp: Int, coins: Int, stars: Int, combo: Int = 0, edit: (Profile) -> Profile = { it }) {
        val m = me ?: return
        val f = st.award(m.id, xp, coins, stars, combo, edit)
        if (f.isNotEmpty()) { fresh = f; fx.win() }
        tick++
    }
    fun diff() = ((me?.level ?: 1) / 4).coerceAtMost(3)

    /** Daily gift: coins grow with the play streak, with a big bonus every 7th day. Returns the coins (0 if already opened). */
    fun chest(): Int {
        val m = me ?: return 0; val today = dayKey(System.currentTimeMillis())
        if (m.chestDay == today) return 0
        val c = 10 + 5 * minOf(m.streak, 10) + if (m.streak > 0 && m.streak % 7 == 0) 50 else 0
        reward(10, c, 0) { it.copy(chestDay = today) }
        return c
    }

    fun done(spec: QuizSpec, r: QuizResult) {
        val m = me ?: return; val today = dayKey(System.currentTimeMillis()); val timed = spec.kind == "lightning"
        val stars = if (timed) minOf(3, r.correct / 6) else starsFor(r.correct, r.total)
        val firstDaily = spec.kind == "daily" && m.dailyDay != today
        val xp = r.correct * (if (timed) 4 else 5) + stars * 10 + (if (firstDaily) 40 else 0)
        val coins = r.correct + stars * 2 + (if (firstDaily) 15 else 0)
        reward(xp, coins, stars, r.combo) { p ->
            var x = p.copy(games = p.games + 1)
            if (spec.kind == "level") x = x.copy(levels = x.levels + (spec.level to maxOf(x.levels[spec.level] ?: 0, stars)))
            if (firstDaily) x = x.copy(daily = x.daily + 1, dailyDay = today)
            x
        }
        outcome = Outcome(spec, r, stars, xp, coins); fx.win(); go("result")
    }

    fun launch(route: String) {
        val m = me ?: return; val g = Gen(System.nanoTime())
        val spec: QuizSpec? = when {
            route.startsWith("practice:") -> Kind.valueOf(route.removePrefix("practice:")).let { k -> QuizSpec("practice", k.title, List(10) { g.make(k, diff()) }) }
            route == "daily" -> QuizSpec("daily", "Daily Challenge", Gen(dailySeed(dayKey(System.currentTimeMillis()))).mix(10, diff()))
            route == "lightning" -> QuizSpec("lightning", "Lightning", g.mix(80, diff()), timeLimit = 45)
            route == "review" -> reviewQuestions(weakWords(m.mastery, 8), System.nanoTime()).let { r -> QuizSpec("review", "Review", if (r.size < 6) r + g.mix(6 - r.size, diff()) else r) }
            route.startsWith("level:") -> adventure[route.removePrefix("level:").toInt() - 1].let { lv -> QuizSpec("level", "${lv.world.substringBefore(' ')} ${lv.n}", List(6) { g.make(lv.kind, lv.diff) }, level = lv.n) }
            else -> null
        }
        if (spec != null) { quiz = spec.copy(route = route); outcome = null; go("quiz") } else go(route)
    }

    BackHandler(enabled = screen != "home" && screen != "auth" && screen != "login" && screen != "profiles") { go(if (me != null) "home" else "profiles") }
    CompositionLocalProvider(LocalPal provides pal) {
        Backdrop {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Box(Modifier.weight(1f)) {
                    AnimatedContent(screen, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "screens") { s ->
                        when (s) {
                            "auth", "login" -> Auth(st) { tick++; go("profiles") }
                            "profiles" -> Profiles(kids, { id -> st.activeId = id; st.checkin(id); tick++; go("home") }, { go("addkid") }, { st.activeId = null; go("login") })
                            "addkid" -> AddKid(st, { n, a -> val k = st.addKid(n, a); st.activeId = k.id; st.checkin(k.id); tick++; go("namegame") }, { go("profiles") })
                            "namegame" -> NameGame(me?.name ?: "Friend", voice) { reward(20, 5, 1); go("home") }
                            "home" -> me?.let { HomeScreen(it, cfg.goal, voice, ::chest, ::launch) }
                            "map" -> me?.let { k -> MapScreen(k) { n -> launch("level:$n") } }
                            "awards" -> me?.let { AwardsScreen(it) }
                            "shop" -> me?.let { k -> ShopScreen(k, voice,
                                { i -> if (k.coins >= pals[i].price) { st.update(k.id) { it.copy(coins = it.coins - pals[i].price, ownedTheme = it.ownedTheme + i, theme = i) }; tick++; true } else false },
                                { a -> st.update(k.id) { it.copy(av = a) }; tick++ },
                                { a, price -> if (k.coins >= price) { st.update(k.id) { it.copy(coins = it.coins - price, ownedAv = it.ownedAv + a, av = a) }; tick++; true } else false },
                                { i -> st.update(k.id) { it.copy(theme = i) }; tick++ }) }
                            "parent" -> ParentScreen(st, kids, cfg, { cfg = it; st.settings = it }, { go("profiles") },
                                { id -> st.delete(id); tick++; if (st.activeId == null) go("profiles") }, { st.activeId = null; go("login") }, { tick++ })
                            "quiz" -> quiz?.let { q -> QuizScreen(q, voice, fx, { qq, ok -> me?.let { st.record(it.id, qq, ok) } }, { r -> done(q, r) }, { go("home") }) }
                            "result" -> outcome?.let { o -> ResultScreen(o.spec.title, o.r, o.stars, o.xp, o.coins, { launch(o.spec.route) },
                                if (o.spec.kind == "level" && o.spec.level < adventure.size && o.stars >= 1) ({ launch("level:${o.spec.level + 1}") }) else null, { go("home") }) }
                            "memory" -> MemoryMatch(voice, fx, { reward(30, 8, 2) { it.copy(games = it.games + 1) } }) { go("home") }
                            "sentence" -> SentenceBuilder(voice, fx, { reward(30, 8, 2) { it.copy(games = it.games + 1) }; go("home") }) { go("home") }
                            "alphabet" -> AlphabetWorld(voice) { go("home") }
                            "words3" -> WordBuilder(me?.name ?: "", words3, voice, { reward(8, 1, 1) }) { go("home") }
                            "words4" -> WordBuilder(me?.name ?: "", words4, voice, { reward(8, 1, 1) }) { go("home") }
                            "hear" -> WordBuilder(me?.name ?: "", words3.take(3) + words4.take(3), voice, { reward(8, 1, 1) }, hear = true) { go("home") }
                            "stories" -> Stories(me?.name ?: "", voice, { reward(8, 1, 1) }) { go("home") }
                            "trace" -> Trace(voice, { reward(8, 1, 1) }) { go("home") }
                            "basket" -> Basket(me?.name ?: "", voice, { reward(8, 1, 1) }) { go("home") }
                            "race" -> Race(voice, { sc -> reward(sc * 3, sc, if (sc >= 8) 3 else if (sc >= 4) 2 else if (sc >= 1) 1 else 0) { it.copy(best = maxOf(it.best, sc), race = maxOf(it.race, sc), games = it.games + 1) } }, { go("home") }, { go("board") })
                            "board" -> Board(kids, me?.id) { go("home") }
                            "runner" -> me?.let { k -> LetterDash(k, diff(), voice, fx,
                                { w, ok -> st.record(k.id, Question(Kind.PICTURE_WORD, "", "", emptyList(), 0, "", w), ok) },
                                { r -> reward(r.xp, r.coinReward, r.stars, r.bestCombo) { p -> p.copy(games = p.games + 1, runBest = maxOf(p.runBest, r.score), runWords = p.runWords + r.words) } },
                                { go("home") }) }
                            "cube" -> LetterCube(voice, fx, { reward(8, 1, 1) { it.copy(cube = it.cube + 1) } }) { go("home") }
                            "paint" -> PaintStudio(voice, { reward(12, 3, 1) { it.copy(art = it.art + 1) } }) { go("home") }
                            else -> {}
                        }
                    }
                }
                if (screen in mainTabs && me != null) { Spacer(Modifier.height(8.dp)); BottomNav(screen) { go(it) } }
            }
            if (fresh.isNotEmpty()) BadgePopup(fresh) { fresh = emptyList() }
        }
    }
}

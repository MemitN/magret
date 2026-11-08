package com.margaretm.app

import java.util.Random

// ================= CONTENT BANK (pure Kotlin, no Android imports) =================
data class Item(val w: String, val e: String, val theme: String)

private fun theme(name: String, raw: String) = raw.trim().split(Regex("\\s+")).map { val (w, e) = it.split("="); Item(w, e, name) }

val bank: List<Item> = listOf(
    theme("Animals", "CAT=🐱 DOG=🐶 PIG=🐷 COW=🐮 HEN=🐔 BEE=🐝 BAT=🦇 OWL=🦉 FOX=🦊 ANT=🐜 FROG=🐸 DUCK=🦆 FISH=🐠 BEAR=🐻 LION=🦁 BIRD=🐦 SNAKE=🐍 HORSE=🐴 MOUSE=🐭 TIGER=🐯 ZEBRA=🦓 PANDA=🐼 SHEEP=🐑 WHALE=🐋 SHARK=🦈 BUG=🐞"),
    theme("Food", "EGG=🥚 PIE=🥧 BREAD=🍞 CAKE=🍰 MILK=🥛 RICE=🍚 CORN=🌽 PEAR=🍐 APPLE=🍎 GRAPE=🍇 LEMON=🍋 MANGO=🥭 BANANA=🍌 PIZZA=🍕 HONEY=🍯 CHEESE=🧀 CARROT=🥕 COOKIE=🍪"),
    theme("Nature", "SUN=☀️ MOON=🌙 STAR=⭐ TREE=🌳 RAIN=🌧️ SNOW=❄️ FIRE=🔥 LEAF=🍃 ROSE=🌹 CLOUD=☁️ OCEAN=🌊"),
    theme("Vehicles", "CAR=🚗 BUS=🚌 BIKE=🚲 BOAT=⛵ TRAIN=🚆 PLANE=✈️ TRUCK=🚚 ROCKET=🚀"),
    theme("Home", "BED=🛏️ DOOR=🚪 BOOK=📚 KEY=🔑 LAMP=💡 CLOCK=🕐 CHAIR=🪑 PHONE=📱 BELL=🔔 HAT=🎩 SHOE=👟 SOCK=🧦"),
    theme("Body", "EYE=👁️ EAR=👂 HAND=✋ NOSE=👃 FOOT=🦶 MOUTH=👄 TOOTH=🦷"),
    theme("Fun", "BALL=⚽ KITE=🪁 DRUM=🥁 GIFT=🎁 TOY=🧸 ROBOT=🤖 CROWN=👑 HEART=❤️")
).flatten()

val themeNames = bank.map { it.theme }.distinct()
val sightWords = ("THE AND YOU THAT WAS FOR ARE WITH HIS THEY HAVE THIS FROM ONE HAD WORD BUT NOT WHAT ALL WERE WHEN YOUR CAN SAID " +
    "THERE USE EACH WHICH SHE HOW THEIR WILL UP OTHER ABOUT OUT MANY THEN THEM THESE SOME HER WOULD MAKE LIKE HIM INTO TIME LOOK TWO MORE WRITE").split(" ")
val rhymes: Map<String, List<String>> = mapOf(
    "AT" to listOf("CAT", "BAT", "HAT", "RAT", "MAT", "SAT"), "OG" to listOf("DOG", "LOG", "FOG", "HOG"),
    "UN" to listOf("SUN", "BUN", "FUN", "RUN", "NUN"), "IG" to listOf("PIG", "BIG", "DIG", "WIG"),
    "UG" to listOf("BUG", "HUG", "MUG", "RUG", "JUG"), "ED" to listOf("BED", "RED", "FED", "LED"),
    "ELL" to listOf("BELL", "WELL", "TELL", "SELL"), "AKE" to listOf("CAKE", "LAKE", "MAKE", "TAKE"),
    "AIN" to listOf("RAIN", "TRAIN", "PAIN", "GAIN"), "OAT" to listOf("BOAT", "COAT", "GOAT", "MOAT"),
    "IN" to listOf("PIN", "WIN", "FIN", "TIN"), "OP" to listOf("HOP", "TOP", "MOP", "POP"))
val rhymeEmoji = mapOf("CAT" to "🐱", "BAT" to "🦇", "HAT" to "🎩", "DOG" to "🐶", "SUN" to "☀️", "PIG" to "🐷", "BUG" to "🐞",
    "BED" to "🛏️", "BELL" to "🔔", "CAKE" to "🍰", "RAIN" to "🌧️", "TRAIN" to "🚆", "BOAT" to "⛵", "GOAT" to "🐐")
val countEmoji = listOf("🍎", "⭐", "🐟", "🎈", "🚗", "🐞", "🌸", "🍪", "🦋", "⚽")
val kidAvatars = listOf("🦁", "🐼", "🦄", "🐸", "🚀", "🐱", "🐙", "🦊", "🐢", "🦉", "🐯", "🐬", "🦖", "🐝", "🧚", "🤖")
val avatarPrice = kidAvatars.mapIndexed { i, a -> a to (if (i < 6) 0 else 20 + (i - 6) * 10) }.toMap()

// ================= QUESTIONS =================
enum class Kind(val title: String, val emoji: String, val blurb: String) {
    FIRST_LETTER("Starting Sound", "🔤", "Which letter does it start with?"),
    PICTURE_WORD("Picture Match", "🖼️", "Match the word to the picture"),
    MISSING_LETTER("Missing Letter", "🧩", "Find the missing letter"),
    RHYME("Rhyme Time", "🎵", "Find the word that rhymes"),
    SIGHT("Sight Words", "👀", "Hear it, tap it"),
    COUNT("Counting Garden", "🌻", "How many can you count?"),
    CASE("Big & Small", "🔡", "Match capital to small letters"),
    ODD("Odd One Out", "🔍", "Which one does not belong?"),
    LAST_LETTER("Ending Sound", "🔚", "Which letter does it end with?")
}

data class Question(val kind: Kind, val prompt: String, val big: String, val choices: List<String>, val answer: Int, val say: String, val word: String = "")

class Gen(seed: Long) {
    private val r = Random(seed)
    private fun <T> List<T>.pick() = this[r.nextInt(size)]
    private fun <T> List<T>.pickN(n: Int): List<T> = shuffled(r).take(n)
    private fun letters(except: Set<Char>, n: Int) = ('A'..'Z').filter { it !in except }.pickN(n)
    private fun mk(k: Kind, p: String, big: String, right: String, wrong: List<String>, say: String, word: String = ""): Question {
        val all = (wrong + right).shuffled(r); return Question(k, p, big, all, all.indexOf(right), say, word)
    }
    private fun pool(level: Int) = bank.filter { it.w.length <= (3 + level.coerceIn(0, 3)) }.ifEmpty { bank }

    fun make(k: Kind, level: Int = 0, only: Item? = null): Question {
        val n = if (level >= 3) 4 else 3
        when (k) {
            Kind.FIRST_LETTER -> { val i = only ?: pool(level).pick(); return mk(k, "What does ${i.w.lowercase()} start with?", i.e, i.w[0].toString(), letters(setOf(i.w[0]), n - 1).map { it.toString() }, "What letter does ${i.w.lowercase()} start with?", i.w) }
            Kind.LAST_LETTER -> { val i = only ?: pool(level).pick(); return mk(k, "What does ${i.w.lowercase()} end with?", i.e, i.w.last().toString(), letters(setOf(i.w.last()), n - 1).map { it.toString() }, "What letter does ${i.w.lowercase()} end with?", i.w) }
            Kind.PICTURE_WORD -> { val p = pool(level); val i = only ?: p.pick(); return mk(k, "Which word is this?", i.e, i.w, (p + i).distinct().filter { it.w != i.w }.pickN(n - 1).map { it.w }, "Which word says ${i.w.lowercase()}?", i.w) }
            Kind.MISSING_LETTER -> { val i = only ?: pool(level).pick(); val pos = r.nextInt(i.w.length); val shown = i.w.mapIndexed { j, c -> if (j == pos) '_' else c }.joinToString(" ")
                return mk(k, "Which letter is missing?", "${i.e}\n$shown", i.w[pos].toString(), letters(setOf(i.w[pos]), n - 1).map { it.toString() }, "Which letter is missing from ${i.w.lowercase()}?", i.w) }
            Kind.RHYME -> { val fams = rhymes.entries.toList().pickN(2); val f = fams[0]; val ws = f.value.pickN(2); val target = ws[0]; val right = ws[1]
                val wrong = rhymes.values.flatten().filter { it !in f.value }.distinct().pickN(n - 1)
                return mk(k, "Which word rhymes with $target?", (rhymeEmoji[target] ?: "🎵"), right, wrong.take(n - 1), "Which word rhymes with ${target.lowercase()}?", target) }
            Kind.SIGHT -> { val w = sightWords.pick(); return mk(k, "Tap the word you hear", "👂", w, sightWords.filter { it != w }.pickN(n - 1), w.lowercase(), w) }
            Kind.COUNT -> { val max = 3 + level * 3; val c = 1 + r.nextInt(max.coerceAtMost(10)); val e = countEmoji.pick()
                val wrong = (1..10).filter { it != c }.pickN(n - 1).map { it.toString() }
                return mk(k, "How many do you see?", e.repeat(c), c.toString(), wrong, "How many do you see?", c.toString()) }
            Kind.CASE -> { val c = ('A'..'Z').toList().pick(); return mk(k, "Find the small letter for $c", c.toString(), c.lowercaseChar().toString(), letters(setOf(c), n - 1).map { it.lowercaseChar().toString() }, "Which small letter matches $c?", c.toString()) }
            Kind.ODD -> { val ts = themeNames.pickN(2); val a = bank.filter { it.theme == ts[0] }.pickN(n - 1); val b = bank.filter { it.theme == ts[1] }.pick()
                val all = (a + b).shuffled(r); return Question(k, "Which one does not belong?", "🔍", all.map { it.e }, all.indexOf(b), "Which one does not belong?", b.w) }
        }
    }
    fun mix(n: Int, level: Int): List<Question> = List(n) { make(Kind.values().toList().pick(), level) }
}

// ================= PROGRESSION =================
fun xpForLevel(l: Int) = 40 * l * (l + 1) / 2 + 60 * l               // cumulative XP to reach level l+1
fun levelOf(xp: Int): Int { var l = 0; while (xp >= xpForLevel(l + 1)) l++; return l + 1 }
fun levelProgress(xp: Int): Float { val l = levelOf(xp); val lo = xpForLevel(l - 1).let { if (l == 1) 0 else it }; val hi = xpForLevel(l); return ((xp - lo).toFloat() / (hi - lo)).coerceIn(0f, 1f) }
fun rankName(l: Int) = when { l >= 20 -> "Legend 👑"; l >= 14 -> "Super Reader 🏆"; l >= 9 -> "Word Wizard 🧙"; l >= 5 -> "Story Star 🌟"; l >= 3 -> "Little Reader 📘"; else -> "Beginner 🌱" }

// Adventure map: 40 levels, 5 worlds. Each level = 6 questions of a kind, tougher as you go.
data class MapLevel(val n: Int, val world: String, val kind: Kind, val diff: Int, val title: String)
val worlds = listOf("Sunny Meadow 🌼", "Ocean Cove 🌊", "Candy Hills 🍭", "Rocket Space 🚀", "Dragon Castle 🐲")
val adventure: List<MapLevel> = (0 until 40).map { i ->
    val kinds = Kind.values(); val k = kinds[(i * 4 + i / 9) % kinds.size]
    MapLevel(i + 1, worlds[i / 8], k, (i / 8).coerceAtMost(3), "${k.title} ${i % 8 + 1}")
}

// Leitner spaced repetition: box 0..4. Wrong -> back to 0, right -> up one. Lower boxes come first in Review.
fun nextBox(box: Int, ok: Boolean) = if (ok) (box + 1).coerceAtMost(4) else 0
fun weakWords(mastery: Map<String, Int>, n: Int): List<String> = mastery.entries.sortedBy { it.value }.filter { it.value < 3 }.take(n).map { it.key }
fun reviewQuestions(words: List<String>, seed: Long): List<Question> {
    val g = Gen(seed); val kinds = listOf(Kind.PICTURE_WORD, Kind.FIRST_LETTER, Kind.MISSING_LETTER, Kind.LAST_LETTER)
    return words.mapIndexedNotNull { n, w -> bank.firstOrNull { it.w == w }?.let { g.make(kinds[n % kinds.size], 1, it) } }.ifEmpty { g.mix(6, 0) }
}

// Achievements are rules over a stats snapshot.
data class Stats(val xp: Int, val stars: Int, val answered: Int, val correct: Int, val bestCombo: Int, val streak: Int, val levelsDone: Int,
                 val threeStar: Int, val mastered: Int, val daily: Int, val coins: Int, val race: Int, val games: Int)
data class Badge(val id: String, val emoji: String, val name: String, val desc: String, val ok: (Stats) -> Boolean)
val badges: List<Badge> = buildList {
    fun b(id: String, e: String, n: String, d: String, f: (Stats) -> Boolean) = add(Badge(id, e, n, d, f))
    b("first", "🌱", "First Steps", "Answer your first question") { it.answered >= 1 }
    listOf(10, 50, 100, 250, 500, 1000, 2500).forEachIndexed { i, n -> b("ans$n", listOf("📝", "📚", "🎓", "🧠", "💡", "🔭", "🌌")[i], "$n Answers", "Answer $n questions") { s -> s.answered >= n } }
    listOf(5, 20, 50, 100, 250).forEach { n -> b("ok$n", "✅", "$n Correct", "Get $n answers right") { s -> s.correct >= n } }
    listOf(3, 5, 8, 12, 20).forEach { n -> b("combo$n", "🔥", "Combo x$n", "Get $n right in a row") { s -> s.bestCombo >= n } }
    listOf(2, 3, 5, 7, 14, 30).forEach { n -> b("streak$n", "📅", "$n Day Streak", "Play $n days in a row") { s -> s.streak >= n } }
    listOf(1, 5, 10, 20, 30, 40).forEach { n -> b("lv$n", "🗺️", "Map Level $n", "Finish $n adventure levels") { s -> s.levelsDone >= n } }
    listOf(1, 5, 15, 30).forEach { n -> b("three$n", "🌟", "Perfect x$n", "Earn 3 stars on $n levels") { s -> s.threeStar >= n } }
    listOf(3, 10, 25, 50, 100).forEach { n -> b("word$n", "🏅", "$n Words Mastered", "Master $n words") { s -> s.mastered >= n } }
    listOf(1, 3, 7, 14).forEach { n -> b("daily$n", "🎯", "Daily x$n", "Finish $n daily challenges") { s -> s.daily >= n } }
    listOf(10, 25, 50, 100).forEach { n -> b("star$n", "⭐", "$n Stars", "Collect $n stars") { s -> s.stars >= n } }
    listOf(100, 500, 1000).forEach { n -> b("xp$n", "⚡", "$n XP", "Earn $n XP") { s -> s.xp >= n } }
    listOf(5, 10, 15).forEach { n -> b("race$n", "🏁", "Race $n", "Build $n words in a race") { s -> s.race >= n } }
    b("rich", "💰", "Piggy Bank", "Save 100 coins") { it.coins >= 100 }
    b("games10", "🎮", "Game Lover", "Play 10 games") { it.games >= 10 }
}

fun dayKey(ms: Long): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(ms))
fun dailySeed(day: String): Long = day.filter { it.isDigit() }.toLong()
fun starsFor(correct: Int, total: Int): Int = when { correct == total -> 3; correct * 10 >= total * 8 -> 2; correct * 10 >= total * 5 -> 1; else -> 0 }
fun lbl(x: Int) = if (x >= 1000) "%.1fk".format(x / 1000f) else x.toString()

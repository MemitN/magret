package com.margaretm.app

import java.util.Random
import kotlin.math.*

// =====================================================================================
//  World3D.kt  -  pure Kotlin (no Android imports) so it can be unit-tested on the JVM.
//  1) A tiny software 3D toolkit: camera projection, convex meshes, rotation, lighting.
//  2) RunWorld: the full simulation behind "Letter Dash 3D" (lanes, gates, hazards, power-ups).
// =====================================================================================

// ---------- 3D toolkit ----------
/** A convex solid centred on the origin. Face normals are the (normalised) direction of each face centroid. */
class Mesh(val v: Array<FloatArray>, val f: Array<IntArray>) {
    val n: Array<FloatArray> = Array(f.size) { i ->
        var x = 0f; var y = 0f; var z = 0f
        for (k in f[i]) { x += v[k][0]; y += v[k][1]; z += v[k][2] }
        val l = sqrt(x * x + y * y + z * z).let { if (it == 0f) 1f else it }
        floatArrayOf(x / l, y / l, z / l)
    }
}

/** Unit cube (-1..1). Face 0 is the face pointing at the camera when the cube is not rotated. */
val CUBE = Mesh(
    Array(8) { i -> floatArrayOf(if ((i and 1) != 0) 1f else -1f, if ((i and 2) != 0) 1f else -1f, if ((i and 4) != 0) 1f else -1f) },
    arrayOf(intArrayOf(0, 1, 3, 2), intArrayOf(5, 4, 6, 7), intArrayOf(4, 0, 2, 6), intArrayOf(1, 5, 7, 3), intArrayOf(4, 5, 1, 0), intArrayOf(2, 3, 7, 6)))

/** Octahedron (8 triangles) - used for crystals and asteroids. */
val OCTA = Mesh(
    arrayOf(floatArrayOf(1f, 0f, 0f), floatArrayOf(-1f, 0f, 0f), floatArrayOf(0f, 1f, 0f), floatArrayOf(0f, -1f, 0f), floatArrayOf(0f, 0f, 1f), floatArrayOf(0f, 0f, -1f)),
    Array(8) { i -> intArrayOf(i and 1, 2 + ((i shr 1) and 1), 4 + ((i shr 2) and 1)) })

/** Rotate p by roll (about z), then pitch (about x), then yaw (about y). */
fun rot(p: FloatArray, rx: Float, ry: Float, rz: Float, out: FloatArray = FloatArray(3)): FloatArray {
    var x = p[0]; var y = p[1]; var z = p[2]
    if (rz != 0f) { val c = cos(rz); val s = sin(rz); val nx = x * c - y * s; y = x * s + y * c; x = nx }
    if (rx != 0f) { val c = cos(rx); val s = sin(rx); val ny = y * c - z * s; z = y * s + z * c; y = ny }
    if (ry != 0f) { val c = cos(ry); val s = sin(ry); val nx = x * c + z * s; z = -x * s + z * c; x = nx }
    out[0] = x; out[1] = y; out[2] = z; return out
}

/**
 * Pin-hole camera looking down +z. x is right, y is up, z is forward.
 * The horizon sits at [hor] pixels from the top; the eye is [camH] above the ground and [back] behind z = 0.
 */
class Cam(val w: Float, val h: Float, val hor: Float = h * 0.30f, val camH: Float = 1.7f, val back: Float = 3.4f, val fl: Float = w * 0.92f) {
    fun scale(z: Float) = fl / (z + back)
    fun sx(x: Float, z: Float) = w / 2f + x * scale(z)
    fun sy(y: Float, z: Float) = hor + (camH - y) * scale(z)
    fun inFront(z: Float) = z + back > 0.45f
    /** True when a face with outward normal n and centre c is turned towards the eye (back-face culling). */
    fun facing(n: FloatArray, cx: Float, cy: Float, cz: Float) = n[0] * cx + n[1] * (cy - camH) + n[2] * (cz + back) < 0f
}

private val LIGHT = floatArrayOf(-0.35f, 0.8f, -0.5f).let { l -> val m = sqrt(l[0] * l[0] + l[1] * l[1] + l[2] * l[2]); floatArrayOf(l[0] / m, l[1] / m, l[2] / m) }
/** Brightness 0.45..1.0 of a surface with normal n. */
fun shade(n: FloatArray): Float = 0.45f + 0.55f * max(0f, n[0] * LIGHT[0] + n[1] * LIGHT[1] + n[2] * LIGHT[2])

/** A mesh after scale + rotation + translation: world vertices and rotated face normals. */
class Placed(val m: Mesh, val w: Array<FloatArray>, val n: Array<FloatArray>)

/** [tilt] is an extra rotation about the world x axis applied last (trackball-style: yaw first, then tip the whole thing). */
fun place(m: Mesh, sx: Float, sy: Float, sz: Float, rx: Float, ry: Float, rz: Float, ox: Float, oy: Float, oz: Float, tilt: Float = 0f): Placed {
    val tmp = FloatArray(3)
    val w = Array(m.v.size) { i -> tmp[0] = m.v[i][0] * sx; tmp[1] = m.v[i][1] * sy; tmp[2] = m.v[i][2] * sz
        val r = rot(rot(tmp, rx, ry, rz), tilt, 0f, 0f); floatArrayOf(r[0] + ox, r[1] + oy, r[2] + oz) }
    val n = Array(m.n.size) { rot(rot(m.n[it], rx, ry, rz), tilt, 0f, 0f) }
    return Placed(m, w, n)
}

/** Which face of a rotated mesh points most directly at a camera on the -z axis. */
fun frontFace(m: Mesh, rx: Float, ry: Float, rz: Float, tilt: Float = 0f): Int {
    var best = 0; var bv = -2f
    for (i in m.n.indices) { val v = -rot(rot(m.n[i], rx, ry, rz), tilt, 0f, 0f)[2]; if (v > bv) { bv = v; best = i } }
    return best
}

// ---------- Letter Dash 3D: simulation ----------
const val LANE_W = 1f
const val SPAWN_Z = 32f
const val HIT_Z = 0.6f
const val ROAD_HALF = 1.9f

enum class EType { LETTER, ROCK, COIN, SHIELD, SLOW }
class Ent(val type: EType, val lane: Int, var z: Float, val ch: Char = ' ', val ok: Boolean = false, val gate: Int = 0, val seed: Float = 0f) {
    var done = false   // already resolved (picked up, hit, or flown past the player)
    var gone = false   // picked up / smashed: do not draw any more
}

sealed class Ev {
    data class Good(val ch: Char, val combo: Int) : Ev()
    data class Bad(val ch: Char, val need: Char) : Ev()
    data class WordDone(val word: String, val clean: Boolean) : Ev()
    object Crash : Ev()
    object ShieldPop : Ev()
    object Coin : Ev()
    data class Power(val t: EType) : Ev()
    data class Over(val won: Boolean) : Ev()
}

class Spark(var x: Float, var y: Float, var z: Float, val vx: Float, var vy: Float, val vz: Float, var life: Float, val color: Long, val size: Float)
class Pop3(val text: String, val x: Float, var y: Float, var life: Float, val color: Long)

data class RunSummary(val won: Boolean, val words: Int, val total: Int, val letters: Int, val coins: Int, val bestCombo: Int, val hearts: Int,
                      val score: Int, val stars: Int, val xp: Int, val coinReward: Int)

val CONFUSE = mapOf('B' to "DPR", 'D' to "BOP", 'P' to "BQR", 'Q' to "OGP", 'M' to "NWH", 'N' to "MHU", 'W' to "MV", 'V' to "UW", 'U' to "VN",
    'O' to "QCD", 'C' to "OGE", 'E' to "FLB", 'F' to "EPT", 'G' to "CQO", 'H' to "NMA", 'I' to "LJT", 'J' to "IL", 'L' to "IJT", 'T' to "IF",
    'S' to "ZC", 'Z' to "SN", 'K' to "XR", 'X' to "KY", 'Y' to "VX", 'R' to "PBK", 'A' to "HV")

/** Picks the words for one run: words the child is still learning first, then new ones, then mastered ones. */
fun runWords(mastery: Map<String, Int>, diff: Int, n: Int, seed: Long): List<Item> {
    val g = Random(seed); val maxLen = 3 + diff.coerceIn(0, 2)
    val pool = bank.filter { it.w.length in 3..maxLen }.ifEmpty { bank }
    val weak = pool.filter { (mastery[it.w] ?: -1) in 0..2 }
    val fresh = pool.filter { !mastery.containsKey(it.w) }
    val strong = pool.filter { (mastery[it.w] ?: 0) >= 3 }
    return (weak.shuffled(g).take(2) + fresh.shuffled(g) + weak.shuffled(g) + strong.shuffled(g)).distinctBy { it.w }.take(n)
}

class RunWorld(val words: List<Item>, val diff: Int, val speedMul: Float, seed: Long) {
    private val rnd = Random(seed)
    val ents = ArrayList<Ent>(); val sparks = ArrayList<Spark>(); val pops = ArrayList<Pop3>()
    var wi = 0; var idx = 0; var hearts = 3
    var lane = 0; var px = 0f
    var dist = 0f; var t = 0f
    var combo = 0; var bestCombo = 0; var coins = 0; var letters = 0; var wordsDone = 0; var cleanWords = 0; var score = 0
    var shield = false; var slowT = 0f
    var hitFlash = 0f; var shake = 0f
    var banner = ""; var bannerT = 0f
    var over = false; var won = false
    private var mistakes = 0; private var gateId = 0; private var gateOut = false
    private var gateCool = 1.2f; private var fillT = 1.5f

    val item: Item get() = words[wi.coerceAtMost(words.lastIndex)]
    val word: String get() = item.w
    val need: Char get() = word[idx.coerceAtMost(word.lastIndex)]
    val speed: Float get() = (8.5f + diff * 1.0f + min(wordsDone * 0.5f, 2f)) * speedMul * (if (slowT > 0f) 0.6f else 1f)

    fun move(d: Int) { if (!over) lane = (lane + d).coerceIn(-1, 1) }

    /** Advances the world by dt seconds and returns what happened (for sound, voice and HUD). */
    fun step(dt0: Float): List<Ev> {
        val ev = ArrayList<Ev>(); if (over) return ev
        val dt = dt0.coerceIn(0f, 0.05f); val sp = speed
        t += dt; dist += sp * dt
        px += (lane - px) * min(1f, dt * 13f)
        slowT = max(0f, slowT - dt); hitFlash = max(0f, hitFlash - dt * 2.5f); shake = max(0f, shake - dt * 2.5f); bannerT = max(0f, bannerT - dt)
        for (e in ents) e.z -= sp * dt
        resolve(ev)
        ents.removeAll { it.z < -3f }
        spawn(dt)
        val si = sparks.iterator()
        while (si.hasNext()) { val s = si.next(); s.x += s.vx * dt; s.y += s.vy * dt; s.z += s.vz * dt; s.vy -= 6f * dt; s.life -= dt; if (s.life <= 0f || s.y < 0f) si.remove() }
        val pi = pops.iterator()
        while (pi.hasNext()) { val p = pi.next(); p.y += 1.2f * dt; p.life -= dt * 1.1f; if (p.life <= 0f) pi.remove() }
        return ev
    }

    // ----- collisions -----
    private fun resolve(ev: MutableList<Ev>) {
        val pl = px.roundToInt().coerceIn(-1, 1); val x = pl * LANE_W
        for (e in ents) {
            if (e.done || e.z > HIT_Z) continue
            e.done = true
            if (over || e.lane != pl) continue
            e.gone = true
            when (e.type) {
                EType.LETTER -> if (e.ok) good(e.ch, x, ev) else bad(e.ch, x, ev)
                EType.ROCK -> { if (!hurt(ev, Ev.Crash)) burst(x, .5f, 0f, 18, 0xFFFF7043); combo = 0 }
                EType.COIN -> { coins++; score += 5; burst(x, .6f, 0f, 6, 0xFFFFD54F); ev += Ev.Coin }
                EType.SHIELD -> { shield = true; burst(x, .7f, 0f, 14, 0xFF4FC3F7); pop("Shield!", x, 0xFF4FC3F7); ev += Ev.Power(EType.SHIELD) }
                EType.SLOW -> { slowT = 6f; burst(x, .7f, 0f, 14, 0xFFBA68C8); pop("Slow-mo!", x, 0xFFBA68C8); ev += Ev.Power(EType.SLOW) }
            }
        }
        if (gateOut && ents.none { it.type == EType.LETTER && !it.done }) { gateOut = false; gateCool = max(gateCool, 0.6f) }
    }

    private fun good(ch: Char, x: Float, ev: MutableList<Ev>) {
        idx++; letters++; combo++; bestCombo = max(bestCombo, combo)
        val pts = 10 * (1 + combo / 3); score += pts
        burst(x, .7f, 0f, 16, 0xFFFFE066); pop("+$pts", x, 0xFFFFE066)
        ev += Ev.Good(ch, combo)
        if (idx >= word.length) {
            val w = word; val clean = mistakes == 0
            wordsDone++; if (clean) { cleanWords++; if (hearts < 3) hearts++ }
            score += 100 + (if (clean) 50 else 0)
            banner = w; bannerT = 1.6f; ev += Ev.WordDone(w, clean)
            wi++; idx = 0; mistakes = 0; gateCool = 1.8f
            if (wi >= words.size) { over = true; won = true; ev += Ev.Over(true) }
        }
    }

    private fun bad(ch: Char, x: Float, ev: MutableList<Ev>) {
        val n = need; combo = 0; mistakes++
        if (!hurt(ev, Ev.Bad(ch, n))) { burst(x, .6f, 0f, 10, 0xFFEF5350); pop("Oops!", x, 0xFFEF5350) }
    }

    /** Takes one hit. Returns true when the shield absorbed it. */
    private fun hurt(ev: MutableList<Ev>, hit: Ev): Boolean {
        if (shield) { shield = false; ev += Ev.ShieldPop; burst(lane * LANE_W, .8f, 0f, 20, 0xFF4FC3F7); return true }
        hearts--; hitFlash = 1f; shake = 1f; ev += hit
        if (hearts <= 0) { over = true; won = false; ev += Ev.Over(false) }
        return false
    }

    // ----- spawning -----
    // Fairness rules: a gate always gets 12+ units of clear road before it; rock rows need 9+ units from whatever is ahead of them;
    // when a gate is due nothing else spawns until it has, so it can never be starved.
    private fun spawn(dt: Float) {
        if (over) return
        if (!gateOut) gateCool -= dt
        fillT -= dt
        val room = SPAWN_Z - (ents.maxOfOrNull { it.z } ?: -99f)      // free distance between the spawn line and the farthest object
        if (!gateOut && gateCool <= 0f) { if (room >= 12f) spawnGate(); return }
        if (fillT <= 0f && room >= 6f) { fillT = 0.7f + rnd.nextFloat() * 1.1f; spawnFiller(room) }
    }

    private fun spawnGate() {
        val n = need; gateId++
        val okLane = rnd.nextInt(3) - 1
        val pool = decoys(n, word).toMutableList()
        for (l in -1..1) ents += Ent(EType.LETTER, l, SPAWN_Z, if (l == okLane) n else pool.removeAt(0), l == okLane, gateId, rnd.nextFloat() * 6f)
        gateOut = true
    }

    /** Wrong letters for a gate. Harder levels mix in look-alike letters (B/D/P...) and letters from the word itself. */
    fun decoys(n: Char, w: String): List<Char> {
        val out = LinkedHashSet<Char>()
        if (diff >= 1) (CONFUSE[n] ?: "").toList().shuffled(rnd).forEach { out += it }
        if (diff >= 2) w.toList().shuffled(rnd).forEach { out += it }
        ('A'..'Z').shuffled(rnd).forEach { out += it }
        out.remove(n)
        return out.toList()
    }

    private fun spawnFiller(room: Float) {
        val r = rnd.nextFloat(); val rockP = if (room >= 9f) 0.30f + 0.08f * diff else 0f
        when {
            r < rockP -> { val n = if (diff >= 1 && rnd.nextFloat() < 0.4f) 2 else 1
                (-1..1).toList().shuffled(rnd).take(n).forEach { ents += Ent(EType.ROCK, it, SPAWN_Z, seed = rnd.nextFloat() * 6f) } }
            r < rockP + 0.50f -> coinLine()
            r < rockP + 0.58f && !shield -> ents += Ent(EType.SHIELD, rnd.nextInt(3) - 1, SPAWN_Z, seed = rnd.nextFloat() * 6f)
            r < rockP + 0.66f && slowT <= 0f -> ents += Ent(EType.SLOW, rnd.nextInt(3) - 1, SPAWN_Z, seed = rnd.nextFloat() * 6f)
            else -> coinLine()
        }
    }
    private fun coinLine() { val l = rnd.nextInt(3) - 1; repeat(3 + rnd.nextInt(2)) { ents += Ent(EType.COIN, l, SPAWN_Z + it * 1.6f, seed = it * .7f) } }

    private fun burst(x: Float, y: Float, z: Float, n: Int, color: Long) {
        if (sparks.size > 300) return
        repeat(n) { sparks += Spark(x, y, z, (rnd.nextFloat() - .5f) * 3f, rnd.nextFloat() * 3f + .5f, (rnd.nextFloat() - .3f) * 3f, .6f + rnd.nextFloat() * .4f, color, .04f + rnd.nextFloat() * .05f) }
    }
    private fun pop(text: String, x: Float, color: Long) { pops += Pop3(text, x, 1.2f, 1f, color) }

    fun summary(): RunSummary {
        val stars = when { won && hearts >= 3 -> 3; won && hearts == 2 -> 2; won || wordsDone >= 1 -> 1; else -> 0 }
        return RunSummary(won, wordsDone, words.size, letters, coins, bestCombo, hearts.coerceAtLeast(0), score, stars,
            xp = wordsDone * 15 + letters * 2 + stars * 10, coinReward = coins + stars * 3 + wordsDone * 2)
    }
}

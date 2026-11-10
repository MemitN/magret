import com.margaretm.app.*
import java.util.Random
import kotlin.math.*

fun check(c: Boolean, m: String) { if (!c) { println("FAIL: $m"); kotlin.system.exitProcess(1) } else println("ok   $m") }

/** Attentive-kid bot: sees 14 units ahead, may change lane at most once per 0.3s. */
class Bot { var cd = 0
    fun act(g: RunWorld) {
        if (cd > 0) { cd--; return }
        val ahead = g.ents.filter { !it.done && it.z < 14f && (it.type == EType.ROCK || it.type == EType.LETTER) }.sortedBy { it.z }
        val gate = g.ents.firstOrNull { it.type == EType.LETTER && !it.done && it.ok }
        val first = ahead.firstOrNull()
        val target = if (first != null && first.type == EType.ROCK) {
            val z0 = first.z; val bad = ahead.filter { it.type == EType.ROCK && abs(it.z - z0) < 1f }.map { it.lane }.toSet()
            val free = (-1..1).filter { it !in bad }
            if (g.lane !in bad) g.lane else free.minBy { abs(it - (gate?.lane ?: g.lane)) * 3 + abs(it - g.lane) }
        } else gate?.lane ?: g.lane
        val dir = sign((target - g.lane).toFloat()).toInt()
        if (dir != 0) { g.move(dir); cd = 17 }
    }
}

fun runBot(seed: Long, diff: Int, mode: String): RunSummary {
    val g = RunWorld(runWords(emptyMap(), diff, 4, seed), diff, 1f, seed)
    val r = Random(seed); var frames = 0; val bot = Bot()
    while (!g.over && frames < 60 * 600) {
        frames++
        when (mode) { "perfect" -> bot.act(g); "random" -> if (r.nextInt(20) == 0) g.move(r.nextInt(3) - 1) }
        g.step(1f / 60f)
    }
    check(g.over, "run ended ($mode seed=$seed frames=$frames)")
    return g.summary()
}

fun main() {
    // --- 3D math ---
    val cam = Cam(1000f, 2000f)
    check(cam.sx(0f, 10f) == 500f, "centre projects to screen centre")
    check(cam.sy(0f, 0f) > cam.sy(0f, 20f), "near ground is lower on screen than far ground")
    check(cam.scale(0f) > cam.scale(10f), "near objects are bigger")
    val p = place(CUBE, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1.7f, 5f)
    val vis = (0 until 6).filter { cam.facing(p.n[it], 0f, 1.7f, 5f) }
    check(vis == listOf(0), "unrotated cube dead ahead shows only front face (face 0); got $vis")
    val p2 = place(CUBE, 1f, 1f, 1f, 0f, 0.6f, 0f, 1.5f, 1.7f, 5f)
    val vis2 = (0 until 6).count { cam.facing(p2.n[it], 1.5f, 1.7f, 5f) }
    check(vis2 == 2 || vis2 == 3, "yawed cube off to the side shows 2-3 faces ($vis2)")
    check(frontFace(CUBE, 0f, 0f, 0f) == 0, "front face of unrotated cube is 0")
    val sides = (0 until 4).map { frontFace(CUBE, 0f, it * PI.toFloat() / 2f, 0f) }
    check(sides.distinct().size == 4 && sides.none { it == 4 || it == 5 }, "four quarter turns show four distinct side faces: $sides")
    check(setOf(frontFace(CUBE, PI.toFloat() / 2f, 0f, 0f), frontFace(CUBE, -PI.toFloat() / 2f, 0f, 0f)) == setOf(4, 5), "pitching +-90deg shows top/bottom")
    check(frontFace(CUBE, 0.1f, 0.2f, 0f) == 0, "small tilt still shows face 0")
    var sumShade = 0f; for (i in 0 until 6) sumShade += shade(CUBE.n[i]); check(sumShade / 6 in 0.45f..1f, "shade range ok")
    check(OCTA.f.size == 8 && OCTA.n.all { abs(it[0]*it[0]+it[1]*it[1]+it[2]*it[2] - 1f) < 1e-4 }, "octahedron: 8 unit-normal faces")
    // octahedron seen from camera: exactly 4 faces visible when far away dead ahead
    val po = place(OCTA, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1.7f, 30f)
    check((0 until 8).count { cam.facing(po.n[it], 0f, 1.7f, 30f) } == 4, "octahedron shows 4 faces")

    // --- words ---
    for (d in 0..3) { val w = runWords(emptyMap(), d, 4, 7); check(w.size == 4 && w.map { it.w }.distinct().size == 4 && w.all { it.w.length <= 3 + min(d, 2) }, "runWords diff=$d -> ${w.map { it.w }}") }
    val weakFirst = runWords(mapOf("CAT" to 1, "DOG" to 0, "SUN" to 4), 0, 4, 3).map { it.w }
    check("CAT" in weakFirst && "DOG" in weakFirst, "weak words are prioritised: $weakFirst")

    // --- decoys ---
    val g0 = RunWorld(runWords(emptyMap(), 2, 4, 1), 2, 1f, 1)
    repeat(200) { val ds = g0.decoys('B', "BAG"); check(ds.size >= 2 && 'B' !in ds.take(2) && ds[0] != ds[1], "decoys valid") ; if (it > 2) return@repeat }

    // --- simulations ---
    var wins = 0; var tot = 0; var stars3 = 0
    for (d in 0..3) for (s in 1L..25L) { val r = runBot(s * 31 + d, d, "perfect"); tot++; if (r.won) wins++; if (r.stars == 3) stars3++
        if (!(r.won && r.words == 4)) { println("  perfect bot lost? seed=${s*31+d} diff=$d $r") } }
    println("perfect bot: $wins/$tot wins, $stars3 three-star")
    check(wins >= tot * 0.9, "attentive bot wins >=90%")
    var losses = 0; for (s in 1L..20L) { val r = runBot(s, 1, "random"); if (!r.won) losses++ }
    println("random flailing bot lost $losses/20")
    check(losses >= 10, "random bot usually loses")
    var idle = 0; for (s in 1L..20L) { val r = runBot(s, 0, "idle"); if (!r.won) idle++; check(r.hearts >= 0, "hearts never negative") }
    check(idle >= 15, "idle player loses ($idle/20)")
    val sample = runBot(5, 1, "perfect"); println(sample)
    check(sample.xp > 0 && sample.coinReward > 0, "rewards positive")

    // --- timing: how long is a perfect 4-word run? ---
    val g = RunWorld(runWords(emptyMap(), 0, 4, 9), 0, 1f, 9); var f = 0; val bt = Bot()
    while (!g.over && f < 60*300) { bt.act(g); g.step(1f/60f); f++ }
    println("perfect 4x3-letter run takes ${f/60f}s")
    // entity bookkeeping never explodes
    check(g.ents.size < 60 && g.sparks.size <= 400, "entity counts bounded")
    println("ALL PASSED")
}

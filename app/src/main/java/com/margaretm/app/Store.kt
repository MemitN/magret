package com.margaretm.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

data class Profile(
    val id: String, val name: String, val av: String, val stars: Int = 0, val best: Int = 0, val streak: Int = 0, val day: String = "",
    val xp: Int = 0, val coins: Int = 0, val answered: Int = 0, val correct: Int = 0, val bestCombo: Int = 0,
    val levels: Map<Int, Int> = emptyMap(), val mastery: Map<String, Int> = emptyMap(), val badges: Set<String> = emptySet(),
    val daily: Int = 0, val dailyDay: String = "", val race: Int = 0, val games: Int = 0,
    val history: Map<String, Int> = emptyMap(), val ownedAv: Set<String> = emptySet(), val ownedTheme: Set<Int> = setOf(0), val theme: Int = 0,
    val runBest: Int = 0, val runWords: Int = 0, val chestDay: String = "", val art: Int = 0, val cube: Int = 0) {
    val level get() = levelOf(xp)
    fun stats() = Stats(xp, stars, answered, correct, bestCombo, streak, levels.count { it.value >= 1 }, levels.count { it.value >= 3 },
        mastery.count { it.value >= 3 }, daily, coins, race, games, runBest, runWords, art, cube)
    fun today(): Int = history[dayKey(System.currentTimeMillis())] ?: 0
    fun accuracy() = if (answered == 0) 0 else correct * 100 / answered
}
data class Settings(val voiceOn: Boolean = true, val rate: Float = 0.8f, val sound: Boolean = true, val goal: Int = 10, val music: Boolean = true, val voiceIdx: Int = 0)

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("mm2", Context.MODE_PRIVATE)
    private fun h(s: String) = MessageDigest.getInstance("SHA-256").digest("mm$s".toByteArray()).joinToString("") { "%02x".format(it) }
    fun hasAccount() = p.contains("email")
    fun signUp(e: String, pin: String) { p.edit().putString("email", e.lowercase()).putString("pin", h(pin)).apply() }
    fun login(e: String, pin: String) = p.getString("email", "") == e.lowercase() && p.getString("pin", "") == h(pin)
    fun checkPin(pin: String) = p.getString("pin", "") == h(pin)
    fun changePin(pin: String) { p.edit().putString("pin", h(pin)).apply() }
    var activeId: String?
        get() = p.getString("active", null)
        set(v) { p.edit().putString("active", v).apply() }

    var settings: Settings
        get() = Settings(p.getBoolean("s_voice", true), p.getFloat("s_rate", 0.8f), p.getBoolean("s_sound", true), p.getInt("s_goal", 10), p.getBoolean("s_music", true), p.getInt("s_vidx", 0))
        set(s) { p.edit().putBoolean("s_voice", s.voiceOn).putFloat("s_rate", s.rate).putBoolean("s_sound", s.sound).putInt("s_goal", s.goal).putBoolean("s_music", s.music).putInt("s_vidx", s.voiceIdx).apply() }

    private fun intMap(o: JSONObject?): Map<String, Int> = o?.let { j -> j.keys().asSequence().associateWith { j.getInt(it) } } ?: emptyMap()
    private fun strSet(a: JSONArray?): Set<String> = a?.let { (0 until it.length()).map { i -> it.getString(i) }.toSet() } ?: emptySet()

    fun profiles(): List<Profile> {
        val a = JSONArray(p.getString("kids", "[]"))
        return (0 until a.length()).map { val o = a.getJSONObject(it)
            Profile(o.getString("id"), o.getString("name"), o.getString("av"), o.optInt("stars"), o.optInt("best"), o.optInt("streak"), o.optString("day"),
                o.optInt("xp"), o.optInt("coins"), o.optInt("answered"), o.optInt("correct"), o.optInt("bestCombo"),
                intMap(o.optJSONObject("levels")).mapKeys { e -> e.key.toInt() }, intMap(o.optJSONObject("mastery")), strSet(o.optJSONArray("badges")),
                o.optInt("daily"), o.optString("dailyDay"), o.optInt("race"), o.optInt("games"), intMap(o.optJSONObject("history")),
                strSet(o.optJSONArray("ownedAv")), strSet(o.optJSONArray("ownedTheme")).map { s -> s.toInt() }.toSet().ifEmpty { setOf(0) }, o.optInt("theme"),
                o.optInt("runBest"), o.optInt("runWords"), o.optString("chestDay"), o.optInt("art"), o.optInt("cube")) }
    }
    private fun save(l: List<Profile>) {
        val arr = JSONArray()
        l.forEach { k -> arr.put(JSONObject().put("id", k.id).put("name", k.name).put("av", k.av).put("stars", k.stars).put("best", k.best)
            .put("streak", k.streak).put("day", k.day).put("xp", k.xp).put("coins", k.coins).put("answered", k.answered).put("correct", k.correct)
            .put("bestCombo", k.bestCombo).put("levels", JSONObject(k.levels.mapKeys { it.key.toString() })).put("mastery", JSONObject(k.mastery))
            .put("badges", JSONArray(k.badges.toList())).put("daily", k.daily).put("dailyDay", k.dailyDay).put("race", k.race).put("games", k.games)
            .put("history", JSONObject(k.history)).put("ownedAv", JSONArray(k.ownedAv.toList())).put("ownedTheme", JSONArray(k.ownedTheme.map { it.toString() }))
            .put("theme", k.theme).put("runBest", k.runBest).put("runWords", k.runWords).put("chestDay", k.chestDay).put("art", k.art).put("cube", k.cube)) }
        p.edit().putString("kids", arr.toString()).apply()
    }
    fun addKid(n: String, a: String): Profile { val k = Profile(UUID.randomUUID().toString(), n, a, ownedAv = setOf(a)); save(profiles() + k); return k }
    fun update(id: String, f: (Profile) -> Profile) = save(profiles().map { if (it.id == id) f(it) else it })
    fun delete(id: String) { save(profiles().filter { it.id != id }); if (activeId == id) activeId = null }

    fun checkin(id: String) {
        val now = System.currentTimeMillis(); val today = dayKey(now); val y = dayKey(now - 86400000L)
        update(id) { k -> if (k.day == today) k else k.copy(streak = if (k.day == y) k.streak + 1 else 1, day = today) }
    }
    /** Records one answer: totals, today's history, and Leitner box for the word. */
    fun record(id: String, q: Question, ok: Boolean) = update(id) { k ->
        val d = dayKey(System.currentTimeMillis())
        val m = if (q.word in bank.map { it.w }) k.mastery + (q.word to nextBox(k.mastery[q.word] ?: 0, ok)) else k.mastery
        k.copy(answered = k.answered + 1, correct = k.correct + if (ok) 1 else 0, mastery = m, history = k.history + (d to (k.history[d] ?: 0) + 1))
    }
    /** Applies rewards then unlocks any newly earned badges (each pays 10 coins). Returns the new badges. */
    fun award(id: String, xp: Int, coins: Int, stars: Int, combo: Int, edit: (Profile) -> Profile = { it }): List<Badge> {
        var fresh = emptyList<Badge>()
        update(id) { k0 ->
            val k = edit(k0).let { it.copy(xp = it.xp + xp, coins = it.coins + coins, stars = it.stars + stars, bestCombo = maxOf(it.bestCombo, combo)) }
            val s = k.stats(); fresh = badges.filter { it.id !in k.badges && it.ok(s) }
            k.copy(badges = k.badges + fresh.map { it.id }, coins = k.coins + fresh.size * 10, xp = k.xp + fresh.size * 15)
        }
        return fresh
    }
}

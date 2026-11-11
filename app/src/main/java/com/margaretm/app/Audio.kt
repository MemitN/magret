package com.margaretm.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.compositionLocalOf
import android.speech.tts.Voice as TtsVoice
import java.util.Locale

// =====================================================================================
//  Audio.kt  -  all sound in one place.
//   * Sfx    : studio-quality sample effects (res/raw/sfx_*.ogg) played through SoundPool
//   * Music  : a calm ambient loop (res/raw/music_ambient.ogg), pauses with the app
//   * Voice  : the device's best English voice, selectable by a parent. If you drop a recorded
//              voice file in res/raw named  v_<words>.ogg  (e.g. v_great_job.ogg, v_a.ogg) it is
//              played instead of the synthetic voice - so a real person can record the app.
// =====================================================================================

val LocalSfx = compositionLocalOf<Sfx?> { null }

class Sfx(private val ctx: Context) {
    var on = true
    private val pool: SoundPool = SoundPool.Builder().setMaxStreams(6)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val ids = HashMap<String, Int>()
    private val ready = HashSet<Int>()
    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready.add(id) }
        mapOf("tap" to R.raw.sfx_tap, "good" to R.raw.sfx_good, "bad" to R.raw.sfx_bad, "win" to R.raw.sfx_win, "coin" to R.raw.sfx_coin,
            "whoosh" to R.raw.sfx_whoosh, "crash" to R.raw.sfx_crash, "power" to R.raw.sfx_power, "star" to R.raw.sfx_star, "open" to R.raw.sfx_open)
            .forEach { (k, r) -> try { ids[k] = pool.load(ctx, r, 1) } catch (_: Exception) {} }
    }
    private fun play(k: String, vol: Float = 1f, rate: Float = 1f) {
        if (!on) return
        val id = ids[k] ?: return
        if (id in ready) pool.play(id, vol, vol, 1, 0, rate)
    }
    fun tap() = play("tap", .7f)
    fun good() = play("good", .9f)
    fun bad() = play("bad", .8f)
    fun win() = play("win", 1f)
    fun coin() = play("coin", .7f, 0.95f + (Math.random() * .12).toFloat())   // tiny pitch variation so repeats never sound identical
    fun whoosh() = play("whoosh", .5f)
    fun crash() = play("crash", .9f)
    fun power() = play("power", .9f)
    fun star() = play("star", .8f)
    fun open() = play("open", 1f)
    fun close() { try { pool.release() } catch (_: Exception) {} }
}

class Music(private val ctx: Context) {
    private var mp: MediaPlayer? = null
    var on = true
        set(v) { field = v; if (v) resume() else pause() }
    private var paused = false
    private fun ensure(): MediaPlayer? {
        if (mp == null) mp = try { MediaPlayer.create(ctx, R.raw.music_ambient)?.apply { isLooping = true; setVolume(.30f, .30f) } } catch (_: Exception) { null }
        return mp
    }
    fun resume() { paused = false; if (on) try { ensure()?.let { if (!it.isPlaying) it.start() } } catch (_: Exception) {} }
    fun pause() { paused = true; try { mp?.let { if (it.isPlaying) it.pause() } } catch (_: Exception) {} }
    /** Lower the music while the voice speaks. */
    fun duck(low: Boolean) { try { val v = if (low) .12f else .30f; mp?.setVolume(v, v) } catch (_: Exception) {} }
    fun close() { try { mp?.release() } catch (_: Exception) {}; mp = null }
}

class Voice(private val ctx: Context) {
    private var ready = false
    private var tts: TextToSpeech? = null
    private var rec: MediaPlayer? = null
    var on = true
    var music: Music? = null
    var rate = 0.8f
        set(v) { field = v; if (ready) tts?.setSpeechRate(v) }
    /** 0 = automatic (best English voice on the phone); n = nth entry of [options]. */
    var pick = 0
        set(v) { field = v; apply() }
    private var list: List<TtsVoice> = emptyList()
    val options: List<String> get() = list.mapIndexed { i, v -> "Voice ${i + 1}" + (if (v.quality >= TtsVoice.QUALITY_HIGH) " ✦" else "") + " · ${v.locale.displayCountry.ifEmpty { v.locale.language }}" }

    init {
        tts = TextToSpeech(ctx) {
            if (it == TextToSpeech.SUCCESS) {
                val t = tts ?: return@TextToSpeech
                t.language = Locale.US; t.setSpeechRate(rate); t.setPitch(1.04f)
                list = try { t.voices.orEmpty().filter { v -> v.locale.language == "en" && !v.isNetworkConnectionRequired && !v.features.orEmpty().contains("notInstalled") }
                    .sortedWith(compareByDescending<TtsVoice> { v -> v.quality }.thenBy { v -> v.name }) } catch (_: Exception) { emptyList() }
                ready = true; apply()
                t.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(u: String?) { music?.duck(true) }
                    override fun onDone(u: String?) { music?.duck(false) }
                    @Deprecated("Deprecated in Java") override fun onError(u: String?) { music?.duck(false) }
                })
            }
        }
    }
    private fun apply() { val t = tts ?: return; if (!ready) return; try { if (list.isNotEmpty()) t.voice = list[(if (pick <= 0) 0 else pick - 1).coerceIn(0, list.lastIndex)] } catch (_: Exception) {} }
    private fun slug(t: String) = t.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(40)

    fun say(t: String) {
        if (!on) return
        // Recorded human voice wins if the file exists in res/raw
        val id = ctx.resources.getIdentifier("v_" + slug(t), "raw", ctx.packageName)
        if (id != 0) {
            try { rec?.release(); rec = MediaPlayer.create(ctx, id)?.apply { setOnCompletionListener { music?.duck(false) }; music?.duck(true); start() }; return } catch (_: Exception) {}
        }
        if (ready) tts?.speak(t, TextToSpeech.QUEUE_FLUSH, null, t)
    }
    fun close() { try { tts?.shutdown() } catch (_: Exception) {}; try { rec?.release() } catch (_: Exception) {} }
}

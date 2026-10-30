package it.lu3g.tvlauncher.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.security.MessageDigest
import java.util.Locale

/**
 * Voce dell'assistente. Due sorgenti:
 *  - le clip della voce neurale "Paola" (Piper), pregenerate sul Mac da `tools/gen_voice_clips.py` in
 *    `res/raw/voice_<hash>.m4a`: sulla TV si riproduce solo un file audio, nessun calcolo;
 *  - la sintesi vocale di Android (TextToSpeech) in italiano, per chi sceglie una voce di sistema e come
 *    ripiego per le frasi senza clip.
 * Se nessuna delle due è disponibile l'assistente resta solo visivo.
 */
class VoiceAssistant(context: Context) {
    private val app: Context = context.applicationContext
    private var ready = false
    private var pendingSpeak: (() -> Unit)? = null
    private var player: MediaPlayer? = null
    private val clipIds = HashMap<String, Int>()
    private val tts: TextToSpeech = TextToSpeech(app) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            runCatching { tts.language = Locale.ITALIAN }
            pendingSpeak?.invoke()
            pendingSpeak = null
        } else {
            Log.w(TAG, "sintesi vocale di sistema non disponibile")
        }
    }

    /** Voci italiane di sistema installate, offline prima; vuoto se il motore non è pronto. */
    fun italianVoices(): List<Voice> = if (!ready) emptyList() else runCatching {
        tts.voices.orEmpty()
            .filter { it.locale.language == "it" && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features }
            .sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.name }))
    }.getOrDefault(emptyList())

    /**
     * Dice [text]. Con [VoiceNames.NATURAL] riproduce la clip di Paola, se esiste; altrimenti usa la
     * sintesi di sistema con la voce [voiceName] (per [VoiceNames.SYSTEM], la migliore italiana).
     */
    fun speak(text: String, voiceName: String, tone: VoiceTone, volume: Float) {
        stop()
        if (voiceName == VoiceNames.NATURAL) {
            val id = clipId(text)
            if (id != 0 && playClip(id, tone, volume)) return
            Log.i(TAG, "nessuna clip per «$text»: uso la sintesi di sistema")
        }
        speakTts(text, voiceName, tone, volume)
    }

    fun stop() {
        runCatching { tts.stop() }
        player?.let { release(it) }
    }

    fun shutdown() {
        stop()
        runCatching { tts.shutdown() }
    }

    /** Id della risorsa `raw/voice_<hash>` per [text], 0 se manca: stesso SHA-1 di tools/gen_voice_clips.py. */
    private fun clipId(text: String): Int = clipIds.getOrPut(text) {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }.substring(0, 10)
        app.resources.getIdentifier("voice_$hex", "raw", app.packageName)
    }

    private fun playClip(resId: Int, tone: VoiceTone, volume: Float): Boolean = runCatching {
        val afd = app.resources.openRawResourceFd(resId) ?: return@runCatching false
        val mp = MediaPlayer()
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        afd.use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
        val v = volume.coerceIn(0f, 1f)
        mp.setVolume(v, v)
        mp.setOnPreparedListener { p ->
            // Tono: velocità e altezza della clip; se il decoder non lo supporta resta il tono normale.
            if (tone != VoiceTone.NORMALE) runCatching { p.playbackParams = PlaybackParams().setSpeed(tone.rate).setPitch(tone.pitch) }
            runCatching { p.start() }.onFailure { Log.w(TAG, "clip: avvio non riuscito", it) }
            Log.i(TAG, "clip $resId: in riproduzione, ${p.duration} ms")
        }
        mp.setOnCompletionListener { p -> Log.i(TAG, "clip $resId: terminata"); release(p) }
        mp.setOnErrorListener { p, what, extra -> Log.w(TAG, "clip $resId: errore $what/$extra"); release(p); true }
        player = mp
        mp.prepareAsync()
        true
    }.getOrElse { Log.w(TAG, "clip non riproducibile", it); false }

    private fun release(p: MediaPlayer) {
        runCatching { p.release() }
        if (player === p) player = null
    }

    private fun speakTts(text: String, voiceName: String, tone: VoiceTone, volume: Float) {
        val action = {
            runCatching {
                val voice = when (voiceName) {
                    VoiceNames.NATURAL, VoiceNames.SYSTEM -> bestItalianVoice()
                    else -> italianVoices().firstOrNull { it.name == voiceName } ?: bestItalianVoice()
                }
                if (voice != null) tts.voice = voice else tts.language = Locale.ITALIAN
                tts.setPitch(tone.pitch)
                tts.setSpeechRate(tone.rate)
                val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f)) }
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "lu3g-" + System.currentTimeMillis())
            }.onFailure { Log.w(TAG, "speak non riuscito", it) }
            Unit
        }
        if (ready) action() else pendingSpeak = action
    }

    /** La voce italiana di sistema di qualità più alta, preferendo quelle che non richiedono la rete. */
    private fun bestItalianVoice(): Voice? {
        val all = italianVoices()
        val offline = all.filter { !it.isNetworkConnectionRequired }
        return offline.ifEmpty { all }.maxByOrNull { it.quality }
    }

    private companion object { const val TAG = "Lu3gVoice" }
}

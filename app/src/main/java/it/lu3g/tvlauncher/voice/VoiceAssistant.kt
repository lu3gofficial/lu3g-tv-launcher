package it.lu3g.tvlauncher.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Sintesi vocale di Android (TextToSpeech) in italiano. Le frasi sono generate al volo,
 * niente registrazioni. Se il motore non è disponibile l'assistente resta solo visivo.
 */
class VoiceAssistant(context: Context) {
    private var ready = false
    private var pendingSpeak: (() -> Unit)? = null
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            runCatching { tts.language = Locale.ITALIAN }
            pendingSpeak?.invoke()
            pendingSpeak = null
        } else {
            Log.w(TAG, "sintesi vocale non disponibile")
        }
    }

    /** Voci italiane installate, offline prima; vuoto se il motore non è pronto. */
    fun italianVoices(): List<Voice> = if (!ready) emptyList() else runCatching {
        tts.voices.orEmpty()
            .filter { it.locale.language == "it" }
            .sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.name }))
    }.getOrDefault(emptyList())

    fun speak(text: String, voiceName: String?, tone: VoiceTone, volume: Float) {
        val action = {
            runCatching {
                val voice = voiceName?.let { name -> tts.voices.orEmpty().firstOrNull { it.name == name } }
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

    fun stop() { runCatching { tts.stop() } }
    fun shutdown() { runCatching { tts.shutdown() } }

    private companion object { const val TAG = "Lu3gVoice" }
}

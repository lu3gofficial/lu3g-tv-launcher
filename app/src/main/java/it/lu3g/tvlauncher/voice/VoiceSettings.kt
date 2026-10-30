package it.lu3g.tvlauncher.voice

/** Valori speciali di [VoiceSettings.voiceName]; qualsiasi altro valore è il nome di una voce di sistema. */
object VoiceNames {
    /** Voce neurale "Paola": clip pregenerate con Piper (`res/raw/voice_*.m4a`, da `tools/gen_voice_clips.py`). */
    const val NATURAL = "lu3g:paola"
    /** Sintesi vocale di Android con la migliore voce italiana installata. */
    const val SYSTEM = "system"
}

/** Impostazioni dell'assistente vocale, salvate nelle preferenze. */
data class VoiceSettings(
    val enabled: Boolean = true,
    val intervalMin: Int = 90,
    val quiet: QuietHours = QuietHours.NIGHT,
    val voiceName: String = VoiceNames.NATURAL,
    val tone: VoiceTone = VoiceTone.NORMALE,
    val volume: Float = 0.8f,
    val categories: Set<String> = setOf(VoiceCategory.COFFEE.id, VoiceCategory.LUNCH.id),
)

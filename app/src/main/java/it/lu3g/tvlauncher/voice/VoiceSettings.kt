package it.lu3g.tvlauncher.voice

/** Impostazioni dell'assistente vocale, salvate nelle preferenze. */
data class VoiceSettings(
    val enabled: Boolean = true,
    val intervalMin: Int = 90,
    val quiet: QuietHours = QuietHours.NIGHT,
    val voiceName: String? = null,
    val tone: VoiceTone = VoiceTone.NORMALE,
    val volume: Float = 0.8f,
    val categories: Set<String> = setOf(VoiceCategory.COFFEE.id, VoiceCategory.LUNCH.id),
)

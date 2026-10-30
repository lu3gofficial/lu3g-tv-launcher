package it.lu3g.tvlauncher.voice

import java.util.Calendar

/**
 * Categorie di avvisi vocali. Per aggiungerne una basta una nuova voce qui: etichetta,
 * frasi (una viene scelta a caso), frase di conferma e regola di attivazione.
 */
enum class VoiceCategory(val id: String, val label: String, val eyebrow: String, val phrases: List<String>, val confirm: String) {
    COFFEE(
        id = "coffee", label = "Pausa caffè", eyebrow = "PAUSA CAFFÈ",
        phrases = listOf(
            "Alt! Il tuo cervello ha richiesto un aggiornamento: caffè in corso!",
            "Ehi capo, molliamo tutto per cinque minuti? Il caffè ci aspetta!",
            "Errore 404: energia umana non trovata. Avviare procedura caffè!",
            "Le task possono aspettare... il caffè assolutamente no!",
            "Signore e signori, interrompiamo le operazioni! È arrivato il momento più importante della giornata... il caffè!",
            "Livello caffeina basso. Ricarica consigliata: tazzina, zucchero, sorriso.",
            "Pausa caffè: il lavoro può aspettare, la tazzina no.",
        ),
        confirm = "Ottima scelta. Buona pausa!",
    ),
    LUNCH(
        id = "lunch", label = "Pausa pranzo", eyebrow = "PAUSA PRANZO",
        phrases = listOf(
            "È ora di pranzo: anche i migliori si fermano a mangiare.",
            "Pausa pranzo! Le app non scappano, il pranzo sì.",
            "Mezzogiorno e mezza: tavola, non tastiera.",
        ),
        confirm = "Buon appetito!",
    );

    fun randomPhrase(): String = phrases.random()

    companion object {
        /** Frase detta quando la pausa viene rimandata. */
        const val SNOOZE_PHRASE = "Va bene, ti avviso tra dieci minuti."
        fun byId(id: String): VoiceCategory? = entries.firstOrNull { it.id == id }
    }
}

/** Tono della voce: altezza e velocità. */
enum class VoiceTone(val label: String, val pitch: Float, val rate: Float) {
    NORMALE("Normale", 1f, 1f),
    ALLEGRO("Allegro", 1.15f, 1.1f),
    CALMO("Calmo", 0.92f, 0.88f),
}

/** Fascia di silenzio (ora di inizio inclusa, ora di fine esclusa, a cavallo della mezzanotte). */
enum class QuietHours(val label: String, val from: Int, val to: Int) {
    NONE("Nessuno", -1, -1),
    EVENING("Dalle 19 alle 8", 19, 8),
    NIGHT("Dalle 20 alle 8", 20, 8),
    LATE("Dalle 22 alle 7", 22, 7);

    fun contains(now: Calendar): Boolean {
        if (this == NONE) return false
        val h = now.get(Calendar.HOUR_OF_DAY)
        return if (from <= to) h in from until to else h >= from || h < to
    }
}

/** Intervalli proposti per la pausa caffè, in minuti. */
val BREAK_INTERVALS = listOf(30, 45, 60, 90, 120, 180)

/** Volumi proposti. */
val VOICE_VOLUMES = listOf(0.4f, 0.6f, 0.8f, 1f)

/**
 * Pianificatore delle pause: decide quando proporre cosa. Tiene conto di silenzio, rinvii e
 * ignorati (ogni avviso ignorato allunga l'intervallo, fino al triplo), e del fatto che la
 * home sia in primo piano: mai parlare sopra un film o una videochiamata.
 */
class BreakScheduler(now: Long = System.currentTimeMillis()) {
    private var lastBreakAt = now
    private var snoozeUntil = 0L
    private var ignored = 0
    private var lastLunchDay = -1

    fun due(
        now: Long,
        foreground: Boolean,
        intervalMin: Int,
        quiet: QuietHours,
        enabled: Set<String>,
    ): VoiceCategory? {
        if (!foreground) return null
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        if (quiet.contains(cal)) return null
        if (now < snoozeUntil) return null
        val day = cal.get(Calendar.DAY_OF_YEAR)
        val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        if (VoiceCategory.LUNCH.id in enabled && minutes in (12 * 60 + 30) until (13 * 60 + 30) && lastLunchDay != day) {
            lastLunchDay = day
            return VoiceCategory.LUNCH
        }
        if (VoiceCategory.COFFEE.id in enabled) {
            val effective = intervalMin * 60_000L * (1 + minOf(ignored, 2))
            if (now - lastBreakAt >= effective) return VoiceCategory.COFFEE
        }
        return null
    }

    fun onConfirm(now: Long) { lastBreakAt = now; ignored = 0 }
    fun onSnooze(now: Long) { snoozeUntil = now + 10 * 60_000L }
    fun onIgnore(now: Long) { lastBreakAt = now; ignored++ }
    fun onShown(now: Long, category: VoiceCategory) { if (category == VoiceCategory.COFFEE) lastBreakAt = now }
}

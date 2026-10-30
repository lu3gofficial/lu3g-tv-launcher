package it.lu3g.tvlauncher.theme

import androidx.compose.ui.graphics.Color
import it.lu3g.tvlauncher.R
import java.util.Calendar

/** Identificativo stabile di un tema (salvato nelle preferenze). */
enum class SeasonId { AUTO, NONE, HALLOWEEN, NATALE, SAN_VALENTINO, PASQUA, ESTATE, AUTUNNO }

/** Forma delle particelle che fluttuano sullo sfondo. */
enum class ParticleKind { LEAF, SNOW, HEART, PUMPKIN, EGG, SPARK }

/** Decorazione fissa agli angoli o ai bordi. */
enum class EdgeDecor { NONE, WEB, LIGHTS, FLOWERS, SUN }

/**
 * Particelle: poche, lente, dietro ai contenuti.
 * @param speed velocità verticale in frazione di schermo al secondo
 * @param size diametro in dp
 * @param sway ampiezza dell'oscillazione orizzontale in dp
 * @param upward true se salgono invece di cadere
 */
data class ParticleSpec(
    val kind: ParticleKind,
    val count: Int,
    val speed: Float,
    val size: Float,
    val sway: Float,
    val colors: List<Color>,
    val upward: Boolean = false,
    val alpha: Float = 0.9f,
)

/** Un tema stagionale: quando vale, come colora lo sfondo, cosa disegna. */
data class SeasonTheme(
    val id: SeasonId,
    val label: String,
    val greeting: String?,
    /** Tinta dell'alone di sfondo (sostituisce l'ambra del brand). */
    val tint: Color,
    val particles: ParticleSpec?,
    val edge: EdgeDecor,
    /** true se il tema vale in questo giorno (mese 1-12, giorno, anno). */
    val isActive: (year: Int, month: Int, day: Int) -> Boolean,
    /** Jingle della splash per questo tema (res/raw); null = jingle di base. */
    val soundRes: Int? = null,
)

/**
 * Catalogo dei temi. Per aggiungerne uno basta un nuovo [SeasonTheme] nella lista [all]:
 * l'ordine è la priorità (le festività vengono prima delle stagioni).
 */
object Seasons {

    private fun inRange(month: Int, day: Int, from: Int, to: Int): Boolean {
        val md = month * 100 + day
        return if (from <= to) md in from..to else md >= from || md <= to
    }

    /** Pasqua (calendario gregoriano, algoritmo di Gauss/Meeus) come giorno dell'anno. */
    fun easterDayOfYear(year: Int): Int {
        val a = year % 19; val b = year / 100; val c = year % 100
        val d = b / 4; val e = b % 4; val f = (b + 8) / 25; val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4; val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return dayOfYear(year, month, day)
    }

    private fun dayOfYear(year: Int, month: Int, day: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, 12, 0, 0)
        return cal.get(Calendar.DAY_OF_YEAR)
    }

    val halloween = SeasonTheme(
        id = SeasonId.HALLOWEEN, label = "Halloween", greeting = "Buon Halloween",
        tint = Color(0xFFFF7A1A),
        particles = ParticleSpec(ParticleKind.PUMPKIN, count = 10, speed = 0.025f, size = 22f, sway = 26f,
            colors = listOf(Color(0xFFFF8C2B), Color(0xFFE8671B), Color(0xFF9B59B6)), alpha = 0.55f),
        edge = EdgeDecor.WEB,
        isActive = { _, m, d -> inRange(m, d, 1024, 1102) },
        soundRes = R.raw.splash_halloween,
    )

    val natale = SeasonTheme(
        id = SeasonId.NATALE, label = "Natale", greeting = "Buone feste",
        tint = Color(0xFFD64545),
        particles = ParticleSpec(ParticleKind.SNOW, count = 40, speed = 0.035f, size = 6f, sway = 14f,
            colors = listOf(Color.White), alpha = 0.7f),
        edge = EdgeDecor.LIGHTS,
        isActive = { _, m, d -> inRange(m, d, 1201, 106) },
        soundRes = R.raw.splash_natale,
    )

    val sanValentino = SeasonTheme(
        id = SeasonId.SAN_VALENTINO, label = "San Valentino", greeting = "Buon San Valentino",
        tint = Color(0xFFE0457B),
        particles = ParticleSpec(ParticleKind.HEART, count = 14, speed = 0.03f, size = 16f, sway = 18f,
            colors = listOf(Color(0xFFFF5C8A), Color(0xFFE0457B), Color(0xFFFF9EBB)), upward = true, alpha = 0.6f),
        edge = EdgeDecor.NONE,
        isActive = { _, m, d -> inRange(m, d, 207, 215) },
        soundRes = R.raw.splash_san_valentino,
    )

    val pasqua = SeasonTheme(
        id = SeasonId.PASQUA, label = "Pasqua", greeting = "Buona Pasqua",
        tint = Color(0xFF7CC6A6),
        particles = ParticleSpec(ParticleKind.EGG, count = 10, speed = 0.02f, size = 18f, sway = 16f,
            colors = listOf(Color(0xFFF9C9D6), Color(0xFFA8DDF0), Color(0xFFFFE9A6), Color(0xFFBFE8C2)), alpha = 0.6f),
        edge = EdgeDecor.FLOWERS,
        isActive = { y, m, d -> val e = easterDayOfYear(y); dayOfYear(y, m, d) in (e - 7)..(e + 1) },
        soundRes = R.raw.splash_pasqua,
    )

    val estate = SeasonTheme(
        id = SeasonId.ESTATE, label = "Estate", greeting = "Buona estate",
        tint = Color(0xFFFFC53D),
        particles = ParticleSpec(ParticleKind.SPARK, count = 18, speed = 0.015f, size = 7f, sway = 10f,
            colors = listOf(Color(0xFFFFE08A), Color(0xFF8FD3FF)), upward = true, alpha = 0.5f),
        edge = EdgeDecor.SUN,
        isActive = { _, m, d -> inRange(m, d, 615, 915) },
        soundRes = R.raw.splash_estate,
    )

    val autunno = SeasonTheme(
        id = SeasonId.AUTUNNO, label = "Autunno", greeting = null,
        tint = Color(0xFFC9671F),
        particles = ParticleSpec(ParticleKind.LEAF, count = 16, speed = 0.03f, size = 16f, sway = 30f,
            colors = listOf(Color(0xFFD9822B), Color(0xFFB5451B), Color(0xFFE3B341), Color(0xFF8C4A1E)), alpha = 0.6f),
        edge = EdgeDecor.NONE,
        isActive = { _, m, d -> inRange(m, d, 916, 1130) },
        soundRes = R.raw.splash_autunno,
    )

    /** Tutti i temi, in ordine di priorità. */
    val all: List<SeasonTheme> = listOf(halloween, natale, sanValentino, pasqua, estate, autunno)

    fun byId(id: SeasonId): SeasonTheme? = all.firstOrNull { it.id == id }

    /** Tema attivo oggi, o null se nessuno. */
    fun forToday(now: Calendar = Calendar.getInstance()): SeasonTheme? {
        val y = now.get(Calendar.YEAR); val m = now.get(Calendar.MONTH) + 1; val d = now.get(Calendar.DAY_OF_MONTH)
        return all.firstOrNull { it.isActive(y, m, d) }
    }

    /** Risolve la scelta dell'utente: automatico, nessuno o un tema forzato. */
    fun resolve(enabled: Boolean, choice: SeasonId): SeasonTheme? = when {
        !enabled -> null
        choice == SeasonId.AUTO -> forToday()
        choice == SeasonId.NONE -> null
        else -> byId(choice)
    }
}

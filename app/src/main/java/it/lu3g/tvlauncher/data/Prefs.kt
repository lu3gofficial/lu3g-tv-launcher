package it.lu3g.tvlauncher.data

import android.content.Context
import it.lu3g.tvlauncher.theme.SeasonId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Preferiti e app recenti, salvati nelle SharedPreferences. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("lu3g_launcher", Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(readList(KEY_FAVORITES))
    /** Pacchetti preferiti, nell'ordine scelto dall'utente. */
    val favorites: StateFlow<List<String>> = _favorites

    private val _recents = MutableStateFlow(readRecents())
    /** Pacchetti ordinati dall'ultimo avvio al meno recente. */
    val recents: StateFlow<List<String>> = _recents

    private val _lastLaunch = MutableStateFlow<Map<String, Long>>(readRecentMap())
    /** Ultimo avvio (millisecondi) per pacchetto, solo per le app avviate da qui. */
    val lastLaunch: StateFlow<Map<String, Long>> = _lastLaunch

    private val _seasonalEnabled = MutableStateFlow(sp.getBoolean(KEY_SEASONAL, true))
    /** Effetti stagionali attivi (predefinito: sì). */
    val seasonalEnabled: StateFlow<Boolean> = _seasonalEnabled

    private val _seasonChoice = MutableStateFlow(
        runCatching { SeasonId.valueOf(sp.getString(KEY_SEASON, null) ?: "AUTO") }.getOrDefault(SeasonId.AUTO),
    )
    /** Tema scelto: automatico (calendario), nessuno, o uno forzato. */
    val seasonChoice: StateFlow<SeasonId> = _seasonChoice

    fun setSeasonalEnabled(enabled: Boolean) {
        sp.edit().putBoolean(KEY_SEASONAL, enabled).apply()
        _seasonalEnabled.value = enabled
    }

    fun setSeasonChoice(id: SeasonId) {
        sp.edit().putString(KEY_SEASON, id.name).apply()
        _seasonChoice.value = id
    }

    fun toggleFavorite(pkg: String) {
        val list = _favorites.value.toMutableList()
        if (!list.remove(pkg)) list.add(pkg)
        sp.edit().putString(KEY_FAVORITES, list.joinToString(SEP)).apply()
        _favorites.value = list
    }

    /** Sposta un preferito di [delta] posizioni (negativo = verso sinistra). */
    fun moveFavorite(pkg: String, delta: Int) {
        val list = _favorites.value.toMutableList()
        val i = list.indexOf(pkg)
        val j = i + delta
        if (i < 0 || j < 0 || j >= list.size) return
        list.removeAt(i)
        list.add(j, pkg)
        sp.edit().putString(KEY_FAVORITES, list.joinToString(SEP)).apply()
        _favorites.value = list
    }

    fun recordLaunch(pkg: String) {
        val map = readRecentMap()
        map[pkg] = System.currentTimeMillis()
        val trimmed = map.entries.sortedByDescending { it.value }.take(MAX_RECENTS)
        sp.edit().putString(KEY_RECENTS, trimmed.joinToString(SEP) { "${it.key}=${it.value}" }).apply()
        _recents.value = trimmed.map { it.key }
        _lastLaunch.value = trimmed.associate { it.key to it.value }
    }

    private fun readList(key: String): List<String> =
        sp.getString(key, "")?.split(SEP)?.filter { it.isNotBlank() } ?: emptyList()

    private fun readRecentMap(): MutableMap<String, Long> =
        readList(KEY_RECENTS).mapNotNull { entry ->
            val i = entry.lastIndexOf('=')
            if (i <= 0) null else entry.substring(0, i) to (entry.substring(i + 1).toLongOrNull() ?: 0L)
        }.toMap().toMutableMap()

    private fun readRecents(): List<String> =
        readRecentMap().entries.sortedByDescending { it.value }.map { it.key }

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_RECENTS = "recents"
        const val KEY_SEASONAL = "seasonal_enabled"
        const val KEY_SEASON = "season_choice"
        const val SEP = "|"
        const val MAX_RECENTS = 12
    }
}

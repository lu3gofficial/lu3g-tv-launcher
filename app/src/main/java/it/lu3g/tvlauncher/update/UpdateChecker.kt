package it.lu3g.tvlauncher.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Una versione nuova pubblicata su GitHub Releases. */
data class UpdateInfo(val version: String, val apkUrl: String, val notes: String, val size: Long)

/**
 * Controlla l'ultima release pubblica su GitHub (nessuna chiave: API pubblica, limite 60 richieste
 * l'ora per indirizzo, noi ne facciamo due al giorno). La release deve avere un file .apk allegato.
 */
object UpdateChecker {
    const val REPO = "lu3gofficial/lu3g-tv-launcher"
    const val RELEASES_URL = "https://github.com/$REPO/releases"

    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val conn = (URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "LU3G-TV/$currentVersion")
        }
        try {
            if (conn.responseCode == 404) return@withContext null // nessuna release ancora
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val tag = json.optString("tag_name").removePrefix("v").trim()
            if (tag.isEmpty() || !isNewer(tag, currentVersion)) return@withContext null
            val assets = json.optJSONArray("assets") ?: return@withContext null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                    return@withContext UpdateInfo(
                        version = tag,
                        apkUrl = a.optString("browser_download_url"),
                        notes = json.optString("body").trim(),
                        size = a.optLong("size"),
                    )
                }
            }
            null
        } finally {
            conn.disconnect()
        }
    }

    /** true se [candidate] è una versione più recente di [current] (confronto numerico per parti). */
    fun isNewer(candidate: String, current: String): Boolean {
        fun parts(v: String) = v.substringBefore('-').split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

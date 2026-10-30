package it.lu3g.tvlauncher.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Scarica l'APK della release dentro una sessione del PackageInstaller e la conferma.
 * Android mostra la sua finestra "Installare l'aggiornamento?": il resto lo fa il sistema,
 * e a installazione finita il launcher riparte da solo (con la splash).
 */
object UpdateInstaller {
    const val ACTION_STATUS = "it.lu3g.tvlauncher.UPDATE_STATUS"

    suspend fun downloadAndInstall(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit) =
        withContext(Dispatchers.IO) {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
            }
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "LU3G-TV")
                }
                try {
                    if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
                    val total = if (info.size > 0) info.size else conn.contentLength.toLong()
                    conn.inputStream.use { input ->
                        session.openWrite("lu3g-tv.apk", 0, total).use { out ->
                            val buf = ByteArray(64 * 1024)
                            var done = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                done += n
                                if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                            }
                            session.fsync(out)
                        }
                    }
                } finally {
                    conn.disconnect()
                }
                val intent = Intent(context, UpdateInstallReceiver::class.java).setAction(ACTION_STATUS)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
                session.commit(pending.intentSender)
            }
        }
}

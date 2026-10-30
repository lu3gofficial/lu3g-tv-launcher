package it.lu3g.tvlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Un'app avviabile dal launcher. */
data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
    val banner: ImageBitmap?,
    val isGame: Boolean,
    val isSystem: Boolean,
    val launchIntent: Intent,
    /** Tinta ricavata dall'icona, usata per colorare lo sfondo quando l'app è a fuoco. */
    val accent: Color,
)

/** Un ingresso fisico della TV (HDMI, AV, ...). */
data class TvInput(val id: String, val label: String, val intent: Intent)

/**
 * Legge le app installate dal PackageManager.
 * Le icone vengono convertite in bitmap una sola volta e tenute in cache
 * per pacchetto + data di aggiornamento: i refresh successivi sono quasi gratis.
 */
class AppRepository(private val context: Context) {
    private val pm: PackageManager = context.packageManager
    private val cache = HashMap<String, Pair<Long, AppEntry>>()

    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val self = context.packageName
        val leanback = query(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val mobile = query(Intent.CATEGORY_LAUNCHER)
        val byPackage = LinkedHashMap<String, ResolveInfo>()
        (leanback + mobile).forEach { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@forEach
            if (pkg != self && !byPackage.containsKey(pkg)) byPackage[pkg] = ri
        }
        val result = byPackage.values.mapNotNull { ri -> runCatching { build(ri) }.getOrNull() }
        synchronized(cache) { cache.keys.retainAll(byPackage.keys) }
        result.sortedBy { it.label.lowercase() }
    }

    private fun query(category: String): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        return runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
    }

    private fun build(ri: ResolveInfo): AppEntry? {
        val pkg = ri.activityInfo.packageName
        val pkgInfo = runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull() ?: return null
        synchronized(cache) {
            cache[pkg]?.let { if (it.first == pkgInfo.lastUpdateTime) return it.second }
        }
        val appInfo = ri.activityInfo.applicationInfo
        val label = ri.loadLabel(pm)?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: pkg

        val launch = pm.getLeanbackLaunchIntentForPackage(pkg)
            ?: pm.getLaunchIntentForPackage(pkg)
            ?: Intent(Intent.ACTION_MAIN).setClassName(pkg, ri.activityInfo.name)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

        val bannerDrawable = runCatching { ri.activityInfo.loadBanner(pm) }.getOrNull()
            ?: runCatching { appInfo.loadBanner(pm) }.getOrNull()
        // Dimensioni limitate a quelle effettivamente mostrate (TV 1080p): memoria bassa anche con molte app.
        // I banner sono opachi: 16 bit (RGB_565) dimezzano la memoria senza differenze visibili.
        val banner = bannerDrawable?.let { toBitmap(it, 448, 252, Bitmap.Config.RGB_565) }
        val iconBitmap = toAndroidBitmap(ri.loadIcon(pm), 160, 160, Bitmap.Config.ARGB_8888)
        val icon = iconBitmap.asImageBitmap()
        val accent = dominantColor(iconBitmap) ?: NEUTRAL_ACCENT

        @Suppress("DEPRECATION")
        val isGame = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo.category == ApplicationInfo.CATEGORY_GAME) ||
            (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        val entry = AppEntry(pkg, label, icon, banner, isGame, isSystem, launch, accent)
        synchronized(cache) { cache[pkg] = pkgInfo.lastUpdateTime to entry }
        return entry
    }

    private fun toBitmap(drawable: Drawable, maxW: Int, maxH: Int, config: Bitmap.Config): ImageBitmap =
        toAndroidBitmap(drawable, maxW, maxH, config).asImageBitmap()

    private fun toAndroidBitmap(drawable: Drawable, maxW: Int, maxH: Int, config: Bitmap.Config): Bitmap {
        val iw = drawable.intrinsicWidth
        val ih = drawable.intrinsicHeight
        val (w, h) = if (iw > 0 && ih > 0) {
            val scale = minOf(1f, maxW.toFloat() / iw, maxH.toFloat() / ih)
            (iw * scale).toInt().coerceAtLeast(1) to (ih * scale).toInt().coerceAtLeast(1)
        } else {
            maxW to maxH
        }
        return drawable.toBitmap(w, h, config)
    }

    /**
     * Colore dominante "utile" dell'icona: l'icona viene ridotta a 16×16 pixel, i pixel
     * trasparenti, quasi bianchi o quasi neri vengono scartati, i restanti raggruppati per
     * tonalità; vince il gruppo più pesante. La tinta finale è scurita e un po' desaturata,
     * così resta un alone di sfondo e non un colore pieno.
     */
    private fun dominantColor(src: Bitmap): Color? {
        val small = Bitmap.createScaledBitmap(src, 16, 16, true)
        val buckets = 12
        val weight = FloatArray(buckets)
        val hueSum = FloatArray(buckets)
        val satSum = FloatArray(buckets)
        val hsv = FloatArray(3)
        for (y in 0 until small.height) for (x in 0 until small.width) {
            val p = small.getPixel(x, y)
            if (android.graphics.Color.alpha(p) < 200) continue
            android.graphics.Color.colorToHSV(p, hsv)
            val s = hsv[1]
            val v = hsv[2]
            if (s < 0.3f || v < 0.2f) continue
            val b = ((hsv[0] / 360f) * buckets).toInt().coerceIn(0, buckets - 1)
            val w = s * s
            weight[b] += w
            hueSum[b] += hsv[0] * w
            satSum[b] += s * w
        }
        if (small !== src) small.recycle()
        val best = weight.indices.maxByOrNull { weight[it] } ?: return null
        if (weight[best] < 2f) return null
        val hue = hueSum[best] / weight[best]
        val sat = (satSum[best] / weight[best]).coerceAtMost(0.7f)
        return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 0.42f)))
    }

    private companion object {
        /** Tinta neutra per icone monocromatiche. */
        val NEUTRAL_ACCENT = Color(0xFF3F3F46)
    }

    /** Ingressi fisici (HDMI ecc.): aprono l'app TV di sistema sulla sorgente scelta. */
    fun loadInputs(): List<TvInput> {
        val tim = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
        return runCatching {
            tim.tvInputList
                .filter { it.isPassthroughInput }
                .map { info ->
                    val label = runCatching { info.loadLabel(context)?.toString() }.getOrNull()
                        .takeUnless { it.isNullOrBlank() } ?: info.id
                    val intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(info.id))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    TvInput(info.id, label, intent)
                }
                .sortedBy { it.label }
        }.getOrDefault(emptyList())
    }
}

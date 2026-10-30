package it.lu3g.tvlauncher.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import it.lu3g.tvlauncher.R
import it.lu3g.tvlauncher.data.AppEntry
import it.lu3g.tvlauncher.ui.theme.Amber
import it.lu3g.tvlauncher.ui.theme.Fog
import it.lu3g.tvlauncher.ui.theme.GUTTER
import it.lu3g.tvlauncher.ui.theme.Ink
import it.lu3g.tvlauncher.ui.theme.Muted
import it.lu3g.tvlauncher.ui.theme.Silver
import it.lu3g.tvlauncher.ui.theme.Surface1
import it.lu3g.tvlauncher.ui.theme.Surface2
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Dati dell'app a fuoco mostrati nell'intestazione. */
data class HeroInfo(val app: AppEntry?, val isFavorite: Boolean, val lastLaunch: Long?)

/**
 * Intestazione "ricca": l'app a fuoco è protagonista. A sinistra etichetta, nome in
 * grande, ultimo utilizzo e i tasti disponibili; a destra il suo banner in grande.
 * Cambia con dissolvenza e leggero scorrimento quando il fuoco si sposta.
 */
@Composable
fun Hero(info: HeroInfo, favoritesEmpty: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = GUTTER)
            .padding(top = 8.dp, bottom = 6.dp)
            .height(198.dp),
    ) {
        AnimatedContent(
            targetState = info,
            transitionSpec = {
                (fadeIn(tween(240, delayMillis = 40)) + slideInVertically(tween(240, delayMillis = 40)) { it / 8 })
                    .togetherWith(fadeOut(tween(120)))
            },
            contentKey = { it.app?.packageName },
            label = "hero",
        ) { h ->
            val a = h.app
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val kind = when {
                        a == null -> stringResource(R.string.app_name)
                        a.isGame -> stringResource(R.string.eyebrow_game)
                        a.isSystem -> stringResource(R.string.eyebrow_system)
                        else -> stringResource(R.string.eyebrow_app)
                    }
                    val eyebrow = if (h.isFavorite) "$kind · ${stringResource(R.string.hero_favorite)}" else kind
                    Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = Amber)
                    Spacer(Modifier.height(6.dp))
                    // Nomi lunghi: corpo più piccolo su due righe invece di troncare
                    val label = a?.label ?: "Ciao"
                    val long = label.length > 16
                    Text(
                        text = label,
                        style = if (long) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayMedium,
                        color = Fog,
                        maxLines = if (long) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (a == null) stringResource(R.string.hint_welcome) else metaText(h.lastLaunch, favoritesEmpty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Silver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row {
                        KeyChip(stringResource(R.string.chip_ok), stringResource(R.string.chip_open), primary = true)
                        Spacer(Modifier.width(10.dp))
                        KeyChip(stringResource(R.string.chip_ok_long), stringResource(R.string.chip_options), primary = false)
                    }
                }
                Spacer(Modifier.width(32.dp))
                if (a != null) HeroBanner(a, Modifier.width(326.dp))
            }
        }
    }
}

@Composable
private fun metaText(lastLaunch: Long?, favoritesEmpty: Boolean): String {
    if (favoritesEmpty) return stringResource(R.string.hint_no_favorites)
    if (lastLaunch == null) return stringResource(R.string.hero_meta_never)
    val locale = Locale.ITALIAN
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = lastLaunch }
    val time = SimpleDateFormat("HH:mm", locale).format(Date(lastLaunch))
    val sameDay = now[Calendar.YEAR] == then[Calendar.YEAR] && now[Calendar.DAY_OF_YEAR] == then[Calendar.DAY_OF_YEAR]
    now.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = now[Calendar.YEAR] == then[Calendar.YEAR] && now[Calendar.DAY_OF_YEAR] == then[Calendar.DAY_OF_YEAR]
    return when {
        sameDay -> stringResource(R.string.hero_meta_today, time)
        yesterday -> stringResource(R.string.hero_meta_yesterday, time)
        else -> stringResource(R.string.hero_meta_date, SimpleDateFormat("d MMMM", locale).format(Date(lastLaunch)))
    }
}

/** "Pillola" che ricorda un tasto del telecomando e cosa fa. Non è attivabile: è un promemoria. */
@Composable
private fun KeyChip(key: String, label: String, primary: Boolean) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (primary) Amber else Surface2)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(key, style = MaterialTheme.typography.labelSmall, color = if (primary) Ink.copy(alpha = 0.7f) else Muted)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (primary) Ink else Fog)
    }
}

/** Banner grande dell'app a fuoco: bordo ambra, alone e un riflesso leggero. Non è attivabile. */
@Composable
private fun HeroBanner(app: AppEntry, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .shadow(22.dp, shape, ambientColor = Amber, spotColor = Amber)
            .clip(shape)
            .background(Surface1)
            .border(3.dp, Amber, shape),
    ) {
        if (app.banner != null) {
            Image(
                bitmap = app.banner,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Surface2, Surface1))),
            ) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(88.dp)
                        .align(Alignment.Center),
                )
                Text(
                    app.label,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Fog,
                )
            }
        }
        // Riflesso
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.14f),
                        0.45f to Color.Transparent,
                        1f to Color.Transparent,
                    ),
                ),
        )
    }
}

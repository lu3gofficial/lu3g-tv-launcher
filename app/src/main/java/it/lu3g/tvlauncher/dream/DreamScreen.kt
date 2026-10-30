package it.lu3g.tvlauncher.dream

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import it.lu3g.tvlauncher.R
import it.lu3g.tvlauncher.theme.SeasonTheme
import it.lu3g.tvlauncher.ui.components.AnimatedBackground
import it.lu3g.tvlauncher.ui.components.SeasonalLayer
import it.lu3g.tvlauncher.ui.theme.Amber
import it.lu3g.tvlauncher.ui.theme.Fog
import it.lu3g.tvlauncher.ui.theme.Ink
import it.lu3g.tvlauncher.ui.theme.Muted
import it.lu3g.tvlauncher.ui.theme.Surface3
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Schermata del salvaschermo: sfondo LU3G fermo (niente aloni in movimento), decorazioni del
 * tema stagionale, e un gruppo logo + orologio + data che ogni 40 secondi scivola piano in un
 * punto diverso dello schermo (contro il burn-in dei pannelli).
 */
@Composable
fun DreamScreen(season: SeasonTheme?) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        AnimatedBackground(Modifier.fillMaxSize(), tint = Surface3, seasonTint = season?.tint, animate = false)
        season?.let { SeasonalLayer(theme = it, modifier = Modifier.fillMaxSize()) }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val groupW = 440.dp
            val groupH = 300.dp
            val rangeX = maxWidth - groupW
            val rangeY = maxHeight - groupH
            var target by remember { mutableStateOf(0.5f to 0.5f) }
            LaunchedEffect(Unit) {
                val rnd = Random(System.currentTimeMillis())
                while (true) {
                    delay(40_000)
                    target = (0.08f + 0.84f * rnd.nextFloat()) to (0.08f + 0.84f * rnd.nextFloat())
                }
            }
            val fx by animateFloatAsState(target.first, tween(7_000, easing = FastOutSlowInEasing), label = "x")
            val fy by animateFloatAsState(target.second, tween(7_000, easing = FastOutSlowInEasing), label = "y")

            Column(
                Modifier
                    .offset(x = rangeX * fx, y = rangeY * fy)
                    .width(groupW),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_lu3g_logo),
                    contentDescription = null,
                    modifier = Modifier.size(110.dp),
                )
                Spacer(Modifier.height(18.dp))
                DreamClock(accent = season?.tint ?: Amber, greeting = season?.greeting)
            }
        }
    }
}

@Composable
private fun DreamClock(accent: androidx.compose.ui.graphics.Color, greeting: String?) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    val locale = Locale.ITALIAN
    val time = remember(now) { SimpleDateFormat("HH:mm", locale).format(Date(now)) }
    val date = remember(now) {
        SimpleDateFormat("EEEE d MMMM", locale).format(Date(now)).replaceFirstChar { it.titlecase(locale) }
    }
    Text(time, style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp, lineHeight = 100.sp), color = Fog)
    Spacer(Modifier.height(4.dp))
    Text(date, style = MaterialTheme.typography.titleMedium, color = Muted)
    if (greeting != null) {
        Spacer(Modifier.height(8.dp))
        Text(greeting, style = MaterialTheme.typography.titleMedium, color = accent)
    }
}

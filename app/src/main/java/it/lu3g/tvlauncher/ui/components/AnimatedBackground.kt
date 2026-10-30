package it.lu3g.tvlauncher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import it.lu3g.tvlauncher.R
import it.lu3g.tvlauncher.ui.theme.Amber
import it.lu3g.tvlauncher.ui.theme.Ink
import it.lu3g.tvlauncher.ui.theme.Surface3
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

/**
 * Sfondo vivo ma leggero: gradiente zinc, due aloni che scivolano lentamente e il
 * logo in filigrana.
 *
 * Pensato per SoC da TV economici:
 *  - gli aloni sono rasterizzati una volta sola in un livello (Offscreen) e poi
 *    vengono solo traslati/scalati dalla GPU: nessun ridisegno di gradienti;
 *  - la posizione si aggiorna 12 volte al secondo invece di 60: con un periodo
 *    di 16-23 secondi lo spostamento per passo è di circa un pixel, invisibile;
 *  - l'animazione gira solo quando il launcher è in primo piano (RESUMED).
 */
@Composable
fun AnimatedBackground(modifier: Modifier = Modifier, tint: Color, seasonTint: Color? = null, animate: Boolean = true) {
    // Alone del brand: ambra, o la tinta del tema stagionale quando c'è
    val brandAnim by animateColorAsState(targetValue = seasonTint ?: Amber, animationSpec = tween(900), label = "brand")
    val brandAlpha = if (seasonTint != null) 0.2f else 0.10f
    // Tinta dell'app a fuoco: cambia con una dissolvenza di mezzo secondo
    val tintAnim by animateColorAsState(targetValue = tint, animationSpec = tween(500), label = "tint")
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val animate = animate && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

    val phaseA = remember { mutableFloatStateOf(0.3f) }
    val phaseB = remember { mutableFloatStateOf(0.6f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val start = System.currentTimeMillis() - 4_000
        while (true) {
            val t = (System.currentTimeMillis() - start) / 1000f
            phaseA.floatValue = (1f - cos(t / 16f * 2f * PI.toFloat())) / 2f
            phaseB.floatValue = (1f - cos(t / 23f * 2f * PI.toFloat())) / 2f
            delay(80)
        }
    }

    BoxWithConstraints(
        modifier.background(Brush.verticalGradient(listOf(Ink, Color(0xFF111114)))),
    ) {
        val m = min(maxWidth.value, maxHeight.value).let { Dp(it) }
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        // Alone con la tinta dell'app a fuoco, in alto a destra
        val gT = m * 0.6f
        Glow(
            color = tintAnim.copy(alpha = 0.6f),
            size = gT,
            modifier = Modifier
                .offset(x = maxWidth * 0.86f - gT / 2, y = maxHeight * 0.06f - gT / 2)
                .graphicsLayer {
                    translationX = wPx * 0.04f * phaseB.floatValue
                    scaleX = 3.3f
                    scaleY = 2.7f
                    compositingStrategy = CompositingStrategy.Offscreen
                },
        )

        // Alone ambra, in alto a destra
        val gA = m * 0.42f
        Glow(
            color = brandAnim.copy(alpha = brandAlpha),
            size = gA,
            modifier = Modifier
                .offset(x = maxWidth * 0.78f - gA / 2, y = maxHeight * 0.05f - gA / 2)
                .graphicsLayer {
                    translationX = wPx * 0.08f * phaseA.floatValue
                    translationY = hPx * 0.2f * phaseB.floatValue
                    scaleX = 3.4f
                    scaleY = 3.4f
                    compositingStrategy = CompositingStrategy.Offscreen
                },
        )

        // Alone grigio, in basso a sinistra
        val gB = m * 0.5f
        Glow(
            color = Surface3.copy(alpha = 0.35f),
            size = gB,
            modifier = Modifier
                .offset(x = maxWidth * 0.1f - gB / 2, y = maxHeight * 0.95f - gB / 2)
                .graphicsLayer {
                    translationX = wPx * 0.12f * phaseB.floatValue
                    translationY = -hPx * 0.15f * phaseA.floatValue
                    scaleX = 3.4f
                    scaleY = 3.4f
                    compositingStrategy = CompositingStrategy.Offscreen
                },
        )

        // Logo in filigrana, in basso a destra
        val logo = maxHeight * 0.95f
        Image(
            painter = painterResource(R.drawable.ic_lu3g_logo),
            contentDescription = null,
            modifier = Modifier
                .offset(x = maxWidth - logo * 0.6f, y = maxHeight - logo * 0.52f)
                .size(logo),
            alpha = 0.035f,
        )
    }
}

@Composable
private fun Glow(color: Color, size: Dp, modifier: Modifier) {
    val brush = remember(color) { Brush.radialGradient(listOf(color, Color.Transparent)) }
    Box(
        modifier
            .size(size)
            .background(brush, CircleShape),
    )
}

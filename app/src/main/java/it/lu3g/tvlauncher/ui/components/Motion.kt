package it.lu3g.tvlauncher.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import it.lu3g.tvlauncher.ui.theme.Amber
import it.lu3g.tvlauncher.ui.theme.Fog
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.exp

/*
 * Aiuti comuni per le animazioni. Regola di tutto il launcher: i valori animati si leggono
 * solo dentro graphicsLayer o nel disegno, così un'animazione non ricompone mai la schermata.
 */

/** Decelerazione morbida: parte veloce e si posa. */
internal val Decel: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/** Avanzamento 0..1 della fase [from, to] dentro un orologio 0..1. */
internal fun phase(clock: Float, from: Float, to: Float, easing: Easing = Decel): Float =
    easing.transform(((clock - from) / (to - from)).coerceIn(0f, 1f))

/**
 * Ingresso a cascata della home: l'elemento [index] compare e sale (la barra scende)
 * seguendo l'orologio [enter], che va da 0 a 1 in un secondo. A orologio fermo non fa nulla.
 */
internal fun Modifier.staggerIn(
    enter: Animatable<Float, AnimationVector1D>,
    index: Int,
    fromTop: Boolean = false,
): Modifier = graphicsLayer {
    val v = enter.value
    if (v < 1f) {
        val start = 0.07f + index * 0.065f
        val p = phase(v, start, start + 0.46f)
        alpha = p
        translationY = (1f - p) * (if (fromTop) -16.dp.toPx() else 28.dp.toPx())
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}

/** Comparsa dei pannelli: crescono appena e si schiariscono. Da chiamare dentro il Dialog. */
@Composable
internal fun rememberPopIn(): Modifier {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(260, easing = Decel)) }
    return Modifier.graphicsLayer {
        val p = pop.value
        val s = 0.92f + 0.08f * p
        scaleX = s
        scaleY = s
        alpha = p
    }
}

/**
 * Anello di fuoco condiviso: un solo bordo ambra che scivola da una card all'altra.
 *
 * La card a fuoco consegna le proprie coordinate; l'anello le insegue fotogramma per
 * fotogramma solo per mezzo secondo dopo ogni spostamento (cambio di fuoco, scorrimento,
 * card che cambia posto), poi si ferma: a riposo non costa nulla.
 */
@Stable
class FocusRingState {
    private var owner: Any? = null
    private var coords: LayoutCoordinates? = null

    /** Cambia a ogni spostamento della card a fuoco: sveglia l'inseguimento. */
    internal var kick by mutableIntStateOf(0)
        private set

    /** OK premuto sulla card a fuoco: l'anello si stringe con lei. */
    var pressed by mutableStateOf(false)

    /** Avanzamento della pressione lunga (0..1): riempie l'anello fino all'apertura del menu. */
    val hold = Animatable(0f)

    internal var rect by mutableStateOf(Rect.Zero)
    internal var alpha by mutableFloatStateOf(0f)

    fun focus(who: Any, c: LayoutCoordinates?) {
        owner = who
        coords = c
        kick++
    }

    fun moved(who: Any, c: LayoutCoordinates) {
        if (owner === who) {
            coords = c
            kick++
        }
    }

    fun blur(who: Any) {
        if (owner === who) {
            owner = null
            coords = null
            pressed = false
            kick++
        }
    }

    fun owns(who: Any): Boolean = owner === who

    /** Rettangolo sullo schermo della card a fuoco, già ingrandita dell'8%. Serve ad apertura e menu. */
    fun screenRect(): Rect? {
        val c = coords?.takeIf { it.isAttached } ?: return null
        val p = c.positionInRoot()
        val w = c.size.width.toFloat()
        val h = c.size.height.toFloat()
        if (w <= 0f || h <= 0f) return null
        return Rect(p.x - w * 0.04f, p.y - h * 0.04f, p.x + w * 1.04f, p.y + h * 1.04f)
    }

    /** Dove deve stare l'anello, nelle coordinate del contenitore che lo disegna. */
    internal fun target(container: LayoutCoordinates?): Rect? {
        val c = coords?.takeIf { it.isAttached } ?: return null
        val box = container?.takeIf { it.isAttached } ?: return null
        val b = box.localBoundingBoxOf(c, clipBounds = false)
        if (b.width <= 0f || b.height <= 0f) return null
        // La card a fuoco è ingrandita dell'8%, premuta del 2%
        val k = if (pressed) 0.01f else 0.04f
        return Rect(b.left - b.width * k, b.top - b.height * k, b.right + b.width * k, b.bottom + b.height * k)
    }
}

/**
 * Disegna l'anello. Va messo dentro il contenuto che scorre, sopra le righe, grande quanto
 * il contenuto: così segue lo scorrimento senza inseguirlo. Ha un livello suo, quindi quando
 * si muove non fa ridisegnare nient'altro.
 */
@Composable
fun FocusRingOverlay(
    ring: FocusRingState,
    enter: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier,
) {
    val holder = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val full = remember { Path() }
    val part = remember { Path() }
    val measure = remember { PathMeasure() }

    LaunchedEffect(ring) {
        snapshotFlow { ring.kick to ring.pressed }.collectLatest {
            val trackUntil = SystemClock.uptimeMillis() + 500
            var last = 0L
            while (true) {
                val now = withFrameNanos { it }
                val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceIn(0f, 0.05f)
                last = now
                val target = ring.target(holder[0])
                if (target != null) {
                    // Da nascosto compare già al suo posto; da visibile scivola
                    ring.rect = if (ring.alpha <= 0f || near(ring.rect, target)) target else follow(ring.rect, target, dt)
                }
                val wanted = if (target != null) 1f else 0f
                val step = dt / 0.15f
                ring.alpha = if (ring.alpha < wanted) (ring.alpha + step).coerceAtMost(wanted) else (ring.alpha - step).coerceAtLeast(wanted)
                val settled = ring.alpha == wanted && (target == null || near(ring.rect, target))
                if (settled && SystemClock.uptimeMillis() > trackUntil) break
            }
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier
            .onGloballyPositioned { holder[0] = it }
            .graphicsLayer { }
            .drawBehind {
                // Durante l'ingresso della home l'anello aspetta che le righe siano arrivate
                val a = ring.alpha * (if (enter.value < 1f) phase(enter.value, 0.55f, 0.95f) else 1f)
                if (a <= 0f) return@drawBehind
                val r = ring.rect
                val stroke = 3.dp.toPx()
                val radius = CornerRadius(15.dp.toPx())
                drawRoundRect(
                    color = Amber,
                    topLeft = Offset(r.left, r.top),
                    size = Size(r.width, r.height),
                    cornerRadius = radius,
                    style = Stroke(stroke),
                    alpha = a,
                )
                val h = ring.hold.value
                if (h > 0f) {
                    full.reset()
                    full.addRoundRect(RoundRect(r, radius))
                    measure.setPath(full, false)
                    part.reset()
                    measure.getSegment(0f, measure.length * h, part, true)
                    drawPath(part, Fog, alpha = a, style = Stroke(stroke))
                }
            },
    )
}

/** Avvicinamento esponenziale: fluido, senza scatti anche se la meta si sposta. */
private fun follow(from: Rect, to: Rect, dt: Float): Rect {
    val k = 1f - exp(-dt * 16f)
    return Rect(
        from.left + (to.left - from.left) * k,
        from.top + (to.top - from.top) * k,
        from.right + (to.right - from.right) * k,
        from.bottom + (to.bottom - from.bottom) * k,
    )
}

private fun near(a: Rect, b: Rect): Boolean =
    abs(a.left - b.left) < 0.5f && abs(a.top - b.top) < 0.5f && abs(a.right - b.right) < 0.5f && abs(a.bottom - b.bottom) < 0.5f

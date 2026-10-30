package it.lu3g.tvlauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import it.lu3g.tvlauncher.theme.EdgeDecor
import it.lu3g.tvlauncher.theme.ParticleKind
import it.lu3g.tvlauncher.theme.ParticleSpec
import it.lu3g.tvlauncher.theme.SeasonTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Una particella in coordinate normalizzate (0..1 dello schermo). */
private class Particle(val index: Int, spec: ParticleSpec, seed: Random) {
    var x = seed.nextFloat()
    var y = seed.nextFloat() * 1.2f - 0.1f
    var rot = seed.nextFloat() * 360f
    val rotSpeed = (seed.nextFloat() - 0.5f) * 60f
    val phase = seed.nextFloat() * 2f * PI.toFloat()
    val freq = 0.5f + seed.nextFloat() * 0.6f
    val speedMul = 0.7f + seed.nextFloat() * 0.6f
    val sizeMul = 0.7f + seed.nextFloat() * 0.6f
    val color = spec.colors[index % spec.colors.size]

    fun step(dt: Float, spec: ParticleSpec, seed: Random) {
        val dir = if (spec.upward) -1f else 1f
        y += dir * spec.speed * speedMul * dt
        rot += rotSpeed * dt
        if (!spec.upward && y > 1.1f) { y = -0.1f; x = seed.nextFloat() }
        if (spec.upward && y < -0.1f) { y = 1.1f; x = seed.nextFloat() }
    }
}

/**
 * Decorazioni stagionali: particelle lente e una decorazione ai bordi, disegnate dietro ai
 * contenuti. Leggere per costruzione: poche forme semplici, 20 aggiornamenti al secondo,
 * solo quando il launcher è in primo piano; la lettura di [frame] nel disegno invalida
 * il solo livello Canvas, senza ricomposizioni.
 */
@Composable
fun SeasonalLayer(theme: SeasonTheme, modifier: Modifier = Modifier) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val animate = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val spec = theme.particles
    val seed = remember(theme.id) { Random(theme.id.ordinal * 7919L) }
    val particles = remember(theme.id) { spec?.let { s -> List(s.count) { Particle(it, s, seed) } } ?: emptyList() }
    val frame = remember { mutableIntStateOf(0) }
    val time = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(animate, theme.id) {
        if (!animate) return@LaunchedEffect
        var last = System.currentTimeMillis()
        while (true) {
            delay(50)
            val now = System.currentTimeMillis()
            val dt = ((now - last) / 1000f).coerceAtMost(0.2f)
            last = now
            time.floatValue += dt
            if (spec != null) for (p in particles) p.step(dt, spec, seed)
            frame.intValue++
        }
    }

    Canvas(modifier) {
        frame.intValue // ridisegna a ogni tick
        val t = time.floatValue
        drawEdge(theme.edge, theme.tint, t)
        if (spec != null) for (p in particles) drawParticle(p, spec, t)
    }
}

private fun DrawScope.drawParticle(p: Particle, spec: ParticleSpec, t: Float) {
    val s = spec.size.dp.toPx() * p.sizeMul
    val cx = p.x * size.width + sin(t * p.freq + p.phase) * spec.sway.dp.toPx()
    val cy = p.y * size.height
    val c = Offset(cx, cy)
    val a = spec.alpha
    when (spec.kind) {
        ParticleKind.SNOW -> drawCircle(p.color, radius = s / 2f, center = c, alpha = a)
        ParticleKind.SPARK -> {
            val pulse = 0.5f + 0.5f * sin(t * 2f + p.phase)
            drawCircle(p.color, radius = s / 2f * (0.6f + 0.4f * pulse), center = c, alpha = a * pulse)
        }
        ParticleKind.LEAF -> rotate(p.rot, pivot = c) {
            drawOval(p.color, topLeft = Offset(cx - s / 2f, cy - s * 0.3f), size = Size(s, s * 0.6f), alpha = a)
            drawLine(p.color.copy(alpha = a), Offset(cx - s / 2f, cy), Offset(cx + s / 2f, cy), strokeWidth = 1.dp.toPx())
        }
        ParticleKind.HEART -> rotate(p.rot * 0.2f, pivot = c) {
            val path = Path().apply {
                moveTo(cx, cy + s * 0.5f)
                cubicTo(cx - s * 0.95f, cy + s * 0.05f, cx - s * 0.55f, cy - s * 0.65f, cx, cy - s * 0.2f)
                cubicTo(cx + s * 0.55f, cy - s * 0.65f, cx + s * 0.95f, cy + s * 0.05f, cx, cy + s * 0.5f)
                close()
            }
            drawPath(path, p.color, alpha = a)
        }
        ParticleKind.PUMPKIN -> if (p.index % 3 == 2) {
            // Pipistrello
            val path = Path().apply {
                moveTo(cx - s, cy)
                quadraticBezierTo(cx - s * 0.5f, cy - s * 0.6f, cx, cy - s * 0.1f)
                quadraticBezierTo(cx + s * 0.5f, cy - s * 0.6f, cx + s, cy)
                quadraticBezierTo(cx + s * 0.5f, cy + s * 0.25f, cx, cy + s * 0.12f)
                quadraticBezierTo(cx - s * 0.5f, cy + s * 0.25f, cx - s, cy)
                close()
            }
            drawPath(path, Color(0xFF3B2A4F), alpha = a)
        } else rotate(sin(t + p.phase) * 8f, pivot = c) {
            // Zucca
            drawOval(p.color, topLeft = Offset(cx - s / 2f, cy - s * 0.4f), size = Size(s, s * 0.8f), alpha = a)
            val dark = Color(0xFF7A3A10).copy(alpha = a * 0.8f)
            drawLine(dark, Offset(cx - s * 0.18f, cy - s * 0.34f), Offset(cx - s * 0.18f, cy + s * 0.34f), strokeWidth = 1.2f.dp.toPx())
            drawLine(dark, Offset(cx + s * 0.18f, cy - s * 0.34f), Offset(cx + s * 0.18f, cy + s * 0.34f), strokeWidth = 1.2f.dp.toPx())
            drawRect(Color(0xFF4F7A2E).copy(alpha = a), topLeft = Offset(cx - s * 0.06f, cy - s * 0.55f), size = Size(s * 0.12f, s * 0.18f))
        }
        ParticleKind.EGG -> rotate(p.rot * 0.3f, pivot = c) {
            drawOval(p.color, topLeft = Offset(cx - s * 0.375f, cy - s / 2f), size = Size(s * 0.75f, s), alpha = a)
            drawLine(Color.White.copy(alpha = a * 0.7f), Offset(cx - s * 0.3f, cy), Offset(cx + s * 0.3f, cy), strokeWidth = 2.dp.toPx())
        }
    }
}

private fun DrawScope.drawEdge(edge: EdgeDecor, tint: Color, t: Float) {
    when (edge) {
        EdgeDecor.NONE -> Unit
        EdgeDecor.WEB -> drawWeb(t)
        EdgeDecor.LIGHTS -> drawLights(t)
        EdgeDecor.FLOWERS -> drawFlowers()
        EdgeDecor.SUN -> drawSun(tint, t)
    }
}

/** Ragnatela nell'angolo in alto a destra, con un ragnetto che dondola. */
private fun DrawScope.drawWeb(t: Float) {
    val c = Offset(size.width, 0f)
    val r = 190.dp.toPx()
    val color = Color(0xFFD4D4D8).copy(alpha = 0.22f)
    val stroke = Stroke(width = 1.dp.toPx())
    val angles = (0..6).map { (90f + 15f * it) * PI.toFloat() / 180f }
    for (an in angles) drawLine(color, c, Offset(c.x + cos(an) * r, c.y + sin(an) * r), strokeWidth = 1.dp.toPx())
    for (ring in 1..5) {
        val rr = r * ring / 5.5f
        val path = Path()
        angles.forEachIndexed { i, an ->
            val pt = Offset(c.x + cos(an) * rr, c.y + sin(an) * rr)
            if (i == 0) path.moveTo(pt.x, pt.y) else {
                val prev = angles[i - 1]
                val mid = (prev + an) / 2f
                val ctrl = Offset(c.x + cos(mid) * rr * 0.92f, c.y + sin(mid) * rr * 0.92f)
                path.quadraticBezierTo(ctrl.x, ctrl.y, pt.x, pt.y)
            }
        }
        drawPath(path, color, style = stroke)
    }
    // Ragnetto appeso a un filo
    val sx = size.width - 70.dp.toPx()
    val sy = 130.dp.toPx() + sin(t * 1.3f) * 12.dp.toPx()
    drawLine(color, Offset(sx, 0f), Offset(sx, sy), strokeWidth = 1.dp.toPx())
    val body = Color(0xFF27272A).copy(alpha = 0.9f)
    drawCircle(body, 5.dp.toPx(), Offset(sx, sy))
    drawCircle(body, 3.dp.toPx(), Offset(sx, sy - 6.dp.toPx()))
    for (i in 0 until 4) {
        val a = (20f + 25f * i) * PI.toFloat() / 180f
        val l = 9.dp.toPx()
        drawLine(body, Offset(sx, sy), Offset(sx + cos(a) * l, sy + sin(a) * l), strokeWidth = 1.dp.toPx())
        drawLine(body, Offset(sx, sy), Offset(sx - cos(a) * l, sy + sin(a) * l), strokeWidth = 1.dp.toPx())
    }
}

/** Filo di luci lungo il bordo superiore, con un leggero lampeggio. */
private fun DrawScope.drawLights(t: Float) {
    val n = 18
    val palette = listOf(Color(0xFFE53935), Color(0xFF43A047), Color(0xFFFFB300), Color(0xFF1E88E5))
    val wire = Path()
    val pts = (0 until n).map { i ->
        val x = size.width * (i + 0.5f) / n
        val y = 6.dp.toPx() + 16.dp.toPx() * sin(PI.toFloat() * ((i + 0.5f) / n))
        Offset(x, y)
    }
    pts.forEachIndexed { i, p -> if (i == 0) wire.moveTo(0f, 6.dp.toPx()); wire.lineTo(p.x, p.y) }
    wire.lineTo(size.width, 6.dp.toPx())
    drawPath(wire, Color(0xFF3F3F46), style = Stroke(width = 1.5f.dp.toPx()))
    pts.forEachIndexed { i, p ->
        val col = palette[i % palette.size]
        val tw = 0.55f + 0.45f * sin(t * 3f + i * 1.7f)
        drawCircle(col, 11.dp.toPx(), p, alpha = 0.22f * tw)
        drawCircle(col, 5.dp.toPx(), p, alpha = 0.9f * tw)
    }
}

/** Qualche fiore primaverile nell'angolo in basso a sinistra. */
private fun DrawScope.drawFlowers() {
    val flowers = listOf(
        Triple(Offset(46.dp.toPx(), size.height - 48.dp.toPx()), 12.dp.toPx(), Color(0xFFF9C9D6)),
        Triple(Offset(96.dp.toPx(), size.height - 30.dp.toPx()), 9.dp.toPx(), Color(0xFFA8DDF0)),
        Triple(Offset(22.dp.toPx(), size.height - 104.dp.toPx()), 8.dp.toPx(), Color(0xFFFFE9A6)),
    )
    for ((c, r, col) in flowers) {
        for (k in 0 until 5) rotate(72f * k, pivot = c) {
            drawOval(col, topLeft = Offset(c.x - r * 0.45f, c.y - r * 2.1f), size = Size(r * 0.9f, r * 1.6f), alpha = 0.55f)
        }
        drawCircle(Color(0xFFFFC53D), r * 0.5f, c, alpha = 0.8f)
    }
}

/** Sole morbido in alto a destra con raggi che ruotano piano. */
private fun DrawScope.drawSun(tint: Color, t: Float) {
    val c = Offset(size.width - 150.dp.toPx(), 40.dp.toPx())
    val r = 110.dp.toPx()
    drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.28f), Color.Transparent), center = c, radius = r), r, c)
    rotate(t * 4f, pivot = c) {
        for (k in 0 until 12) {
            val a = k * 30f * PI.toFloat() / 180f
            drawLine(tint.copy(alpha = 0.14f), Offset(c.x + cos(a) * r * 0.45f, c.y + sin(a) * r * 0.45f), Offset(c.x + cos(a) * r, c.y + sin(a) * r), strokeWidth = 2.dp.toPx())
        }
    }
}

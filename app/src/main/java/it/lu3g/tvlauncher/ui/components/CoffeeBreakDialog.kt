package it.lu3g.tvlauncher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import it.lu3g.tvlauncher.R
import it.lu3g.tvlauncher.ui.theme.Amber
import it.lu3g.tvlauncher.ui.theme.Fog
import it.lu3g.tvlauncher.ui.theme.Ink
import it.lu3g.tvlauncher.ui.theme.Surface1
import it.lu3g.tvlauncher.ui.theme.Surface3
import it.lu3g.tvlauncher.voice.VoiceCategory
import kotlinx.coroutines.delay
import kotlin.math.sin

/** Finestra della pausa: tazzina animata con vapore, frase dell'assistente, "Vado!" o "Tra 10 minuti". */
@Composable
fun CoffeeBreakDialog(
    category: VoiceCategory,
    phrase: String,
    onConfirm: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    val pop = remember { Animatable(0.7f) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) {
            delay(80)
            runCatching { first.requestFocus() }
        }
        LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
        val shape = RoundedCornerShape(24.dp)
        Surface(
            modifier = Modifier
                .width(600.dp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                },
            shape = shape,
            colors = SurfaceDefaults.colors(containerColor = Surface1, contentColor = Fog),
            border = Border(BorderStroke(1.dp, Surface3), shape = shape),
        ) {
            Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                CoffeeCup(Modifier.size(150.dp))
                Spacer(Modifier.width(22.dp))
                Column(Modifier.weight(1f)) {
                    Text(category.eyebrow, style = MaterialTheme.typography.labelSmall, color = Amber)
                    Spacer(Modifier.height(8.dp))
                    Text(phrase, style = MaterialTheme.typography.titleLarge, color = Fog)
                    Spacer(Modifier.height(16.dp))
                    val colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = Fog,
                        focusedContainerColor = Amber,
                        focusedContentColor = Ink,
                    )
                    ListItem(
                        selected = false,
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(first),
                        leadingContent = { Icon(Icons.Default.Check, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.break_go), style = MaterialTheme.typography.labelLarge) },
                        colors = colors,
                    )
                    ListItem(
                        selected = false,
                        onClick = onSnooze,
                        modifier = Modifier.fillMaxWidth(),
                        leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.break_snooze), style = MaterialTheme.typography.labelLarge) },
                        colors = colors,
                    )
                }
            }
        }
    }
}

/** Tazzina con piattino e tre fili di vapore che salgono ondeggiando (20 fotogrammi al secondo). */
@Composable
private fun CoffeeCup(modifier: Modifier = Modifier) {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            time.floatValue += 0.05f
        }
    }
    Canvas(modifier) {
        val t = time.floatValue
        val w = size.width
        val h = size.height
        val cup = Color(0xFFFAFAFA)
        val outline = Color(0xFF3F3F46)
        val coffee = Color(0xFF5C3A1E)

        // Piattino
        drawOval(Color(0xFFD4D4D8), topLeft = Offset(w * 0.08f, h * 0.76f), size = Size(w * 0.84f, h * 0.16f))
        drawOval(outline, topLeft = Offset(w * 0.08f, h * 0.76f), size = Size(w * 0.84f, h * 0.16f), style = Stroke(1.5f.dp.toPx()))

        // Manico
        val handleRect = androidx.compose.ui.geometry.Rect(Offset(w * 0.66f, h * 0.44f), Size(w * 0.26f, h * 0.26f))
        drawArc(outline, startAngle = -90f, sweepAngle = 180f, useCenter = false, topLeft = handleRect.topLeft, size = handleRect.size, style = Stroke(7.dp.toPx()))
        drawArc(cup, startAngle = -90f, sweepAngle = 180f, useCenter = false, topLeft = handleRect.topLeft, size = handleRect.size, style = Stroke(4.dp.toPx()))

        // Corpo della tazzina (leggermente rastremato)
        val body = Path().apply {
            moveTo(w * 0.2f, h * 0.4f)
            lineTo(w * 0.74f, h * 0.4f)
            lineTo(w * 0.68f, h * 0.78f)
            quadraticBezierTo(w * 0.47f, h * 0.86f, w * 0.26f, h * 0.78f)
            close()
        }
        drawPath(body, cup)
        drawPath(body, outline, style = Stroke(2.dp.toPx()))
        // Bordo e caffè
        drawOval(cup, topLeft = Offset(w * 0.2f, h * 0.35f), size = Size(w * 0.54f, h * 0.1f))
        drawOval(outline, topLeft = Offset(w * 0.2f, h * 0.35f), size = Size(w * 0.54f, h * 0.1f), style = Stroke(2.dp.toPx()))
        drawOval(coffee, topLeft = Offset(w * 0.25f, h * 0.37f), size = Size(w * 0.44f, h * 0.06f))

        // Vapore: tre fili che salgono e si dissolvono
        for (i in 0 until 3) {
            val x0 = w * (0.36f + 0.11f * i)
            val phase = t * 2.2f + i * 1.3f
            val path = Path()
            val steps = 12
            for (s in 0..steps) {
                val f = s / steps.toFloat()
                val y = h * 0.33f - f * h * 0.3f
                val x = x0 + sin(phase + f * 5f) * w * 0.035f
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Fog.copy(alpha = 0.55f), style = Stroke(width = 3.dp.toPx()))
        }
    }
}

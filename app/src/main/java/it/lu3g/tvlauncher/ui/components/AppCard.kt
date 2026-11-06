package it.lu3g.tvlauncher.ui.components

import android.view.KeyEvent
import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import it.lu3g.tvlauncher.data.AppEntry
import it.lu3g.tvlauncher.ui.theme.Amber
import it.lu3g.tvlauncher.ui.theme.Fog
import it.lu3g.tvlauncher.ui.theme.Ink
import it.lu3g.tvlauncher.ui.theme.Surface1
import it.lu3g.tvlauncher.ui.theme.Surface2
import it.lu3g.tvlauncher.ui.theme.Surface3

/**
 * Card 16:9 di un'app. Se l'app ha un banner TV lo usa, altrimenti icona + nome su fondo zinc.
 *
 * A fuoco: si ingrandisce con l'alone (tv-material), un riflesso di luce la attraversa una
 * volta e dietro pulsa piano un alone. Il bordo ambra non è suo: è l'anello condiviso
 * ([FocusRingState]) che scivola da una card all'altra; la card gli consegna la propria
 * posizione. Tenendo premuto OK l'anello si riempie fino all'apertura del menu.
 * Tutto in graphicsLayer/draw: nessuna ricomposizione per fotogramma, e l'alone pulsa solo
 * sulla card a fuoco.
 */
@Composable
fun AppCard(
    app: AppEntry,
    width: Dp,
    ring: FocusRingState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    var focused by remember { mutableStateOf(false) }
    // Identità della card per l'anello, e ultime coordinate note (non sono stato: niente ricomposizioni)
    val me = remember { Any() }
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    DisposableEffect(ring) { onDispose { ring.blur(me) } }

    // Riflesso: parte da capo a ogni messa a fuoco
    val sheen = remember { Animatable(2f) }
    LaunchedEffect(focused) {
        if (focused) {
            sheen.snapTo(-0.4f)
            sheen.animateTo(1.4f, tween(520, delayMillis = 60, easing = FastOutSlowInEasing))
        } else {
            sheen.snapTo(2f)
        }
    }

    // Pressione lunga: il menu si apre alla prima ripetizione del tasto, cioè dopo questo tempo.
    // L'anello parte con un piccolo ritardo, così un OK breve non lo fa lampeggiare.
    var holding by remember { mutableStateOf(false) }
    LaunchedEffect(holding) {
        if (holding) {
            val total = ViewConfiguration.getKeyRepeatTimeout().coerceAtLeast(200)
            ring.hold.snapTo(0f)
            ring.hold.animateTo(1f, tween(total - 90, delayMillis = 90, easing = LinearEasing))
        } else if (ring.owns(me)) {
            ring.hold.snapTo(0f)
        }
    }
    val release = {
        holding = false
        if (ring.owns(me)) ring.pressed = false
    }

    Box(
        // Nessuna traslazione della card a fuoco: sposterebbe il suo rettangolo e la ricerca
        // del fuoco di Android (Freccia giù) sceglierebbe la card accanto invece della riga sotto.
        modifier
            .width(width)
            .aspectRatio(16f / 9f),
    ) {
        // Alone pulsante dietro la card a fuoco
        if (focused) {
            val pulse = rememberInfiniteTransition(label = "halo")
            val k by pulse.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
                label = "k",
            )
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val s = 1.18f + 0.06f * k
                        scaleX = s
                        scaleY = s
                        alpha = 0.35f + 0.3f * k
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .background(Brush.radialGradient(listOf(Amber.copy(alpha = 0.55f), Color.Transparent)), shape),
            )
        }

        Surface(
            onClick = onClick,
            onLongClick = {
                release()
                onLongClick()
            },
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) {
                        ring.focus(me, coords[0])
                        onFocused()
                    } else {
                        holding = false
                        ring.blur(me)
                    }
                }
                .onGloballyPositioned {
                    coords[0] = it
                    ring.moved(me, it)
                }
                // Solo per osservare OK premuto/rilasciato: l'evento prosegue fino alla Surface
                .onPreviewKeyEvent { e ->
                    val native = e.nativeKeyEvent
                    val isOk = native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                        native.keyCode == KeyEvent.KEYCODE_ENTER ||
                        native.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                    if (isOk) {
                        if (native.action == KeyEvent.ACTION_DOWN) {
                            if (native.repeatCount == 0 && ring.owns(me)) {
                                holding = true
                                ring.pressed = true
                            }
                        } else if (native.action == KeyEvent.ACTION_UP) {
                            release()
                        }
                    }
                    false
                },
            shape = ClickableSurfaceDefaults.shape(shape = shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Surface1,
                contentColor = Fog,
                focusedContainerColor = Surface2,
                focusedContentColor = Fog,
                pressedContainerColor = Surface3,
                pressedContentColor = Fog,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f, pressedScale = 1.02f),
            glow = ClickableSurfaceDefaults.glow(
                focusedGlow = Glow(elevationColor = Amber.copy(alpha = 0.55f), elevation = 18.dp),
            ),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        // Riflesso obliquo che attraversa la card una volta
                        val s = sheen.value
                        if (s > -0.4f && s < 1.4f) {
                            val x = size.width * s
                            val band = size.width * 0.28f
                            drawRect(
                                brush = Brush.linearGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                                    start = Offset(x - band, 0f),
                                    end = Offset(x + band, size.height),
                                ),
                            )
                        }
                    },
            ) {
                if (app.banner != null) {
                    Image(
                        bitmap = app.banner,
                        contentDescription = app.label,
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
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(width * 0.3f)
                                .align(Alignment.Center),
                        )
                        // Scrim in basso per leggere il nome sopra qualsiasi icona
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(34.dp)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = 0.85f)))),
                        )
                        Text(
                            text = app.label,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Fog,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

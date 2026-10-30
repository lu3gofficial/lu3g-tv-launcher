package it.lu3g.tvlauncher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
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
 * A fuoco: si ingrandisce con bordo ambra e alone (tv-material), un riflesso di luce la
 * attraversa una volta e dietro pulsa piano un alone.
 * Tutto in graphicsLayer/draw: nessuna ricomposizione per fotogramma, e l'alone pulsa solo
 * sulla card a fuoco.
 *
 * [onBounds] riceve la posizione della card nello schermo: serve all'animazione di apertura.
 */
@Composable
fun AppCard(
    app: AppEntry,
    width: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    onBounds: (Rect) -> Unit = {},
) {
    val shape = RoundedCornerShape(14.dp)
    var focused by remember { mutableStateOf(false) }

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
            onLongClick = onLongClick,
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                }
                .onGloballyPositioned { onBounds(it.boundsInRoot()) },
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
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(3.dp, Amber), inset = 0.dp, shape = shape),
            ),
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

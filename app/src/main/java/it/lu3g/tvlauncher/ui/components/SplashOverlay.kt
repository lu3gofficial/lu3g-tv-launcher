package it.lu3g.tvlauncher.ui.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import it.lu3g.tvlauncher.R
import it.lu3g.tvlauncher.theme.SeasonTheme
import it.lu3g.tvlauncher.ui.theme.Amber
import it.lu3g.tvlauncher.ui.theme.Ink
import it.lu3g.tvlauncher.ui.theme.Muted
import kotlin.math.PI
import kotlin.math.sin

/** Durata della splash (il jingle in res/raw dura altrettanto). */
private const val DURATION_MS = 4000

/**
 * Splash animata sul logo, 4 secondi, con jingle sincronizzato:
 *
 *  0,06-1,1 s  i quattro triangoli entrano in ventaglio dall'alto a destra, con rotazione,
 *              zoom e un leggero rimbalzo (una nota che sale per ciascuno);
 *  1,15-1,9 s  le lettere L, U, 3, G si "scrivono" una alla volta (tocchi acuti + accordo);
 *  1,9-2,7 s   un riflesso di luce attraversa il logo (scintillio);
 *  2,6-3,3 s   il logo fa un respiro e l'alone pulsa (tocco basso);
 *  3,3-4,0 s   tenuta, poi chi ospita la splash la apre sulla home con dissolvenza e zoom.
 *
 * Un solo "orologio" 0..1 guida tutte le fasi; i valori sono letti dentro graphicsLayer/draw,
 * quindi ogni fotogramma aggiorna i livelli senza ricomposizioni. Il suono segue il volume
 * (e il muto) della TV.
 */
@Composable
fun SplashOverlay(running: Boolean, season: SeasonTheme? = null, onDone: () -> Unit) {
    // Tema stagionale: tinta dell'alone, decorazioni e saluto
    val glowColor = season?.tint ?: Amber
    val clock = remember { Animatable(0f) }
    val context = LocalContext.current
    val player = remember { arrayOfNulls<MediaPlayer>(1) }

    fun releasePlayer() {
        val p = player[0] ?: return
        player[0] = null
        runCatching { if (p.isPlaying) p.stop() }
        runCatching { p.release() }
    }
    DisposableEffect(Unit) { onDispose { releasePlayer() } }

    // Finché running è false la splash mostra solo nero (clock a 0): serve al risveglio
    // dallo standby, quando il pannello impiega più di un secondo ad accendersi.
    // Animazione e jingle partono insieme.
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        clock.snapTo(0f)
        releasePlayer()
        player[0] = runCatching {
            MediaPlayer.create(
                context,
                season?.soundRes ?: R.raw.splash_sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                0,
            )
        }.getOrNull()
        runCatching {
            player[0]?.setVolume(1f, 1f)
            player[0]?.start()
        }
        clock.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        onDone()
    }

    /** Avanzamento 0..1 della fase tra due istanti in millisecondi. */
    fun phase(fromMs: Int, toMs: Int, easing: Easing = FastOutSlowInEasing): Float {
        val t = (clock.value * DURATION_MS - fromMs) / (toMs - fromMs).toFloat()
        return easing.transform(t.coerceIn(0f, 1f))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        // Decorazioni del tema: sfumano dentro mentre il logo si compone
        if (season != null) {
            SeasonalLayer(
                theme = season,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = phase(500, 1900, LinearEasing) },
            )
        }

        // Alone (ambra, o tinta del tema): cresce mentre il logo si compone, pulsa sul respiro, poi si attenua
        Box(
            Modifier
                .size(460.dp)
                .graphicsLayer {
                    val rise = phase(100, 1300)
                    val pulse = sin(phase(2600, 3300, LinearEasing) * PI).toFloat()
                    val fall = phase(3300, 4000, LinearEasing)
                    alpha = (rise * (1f - 0.5f * fall) + 0.3f * pulse).coerceIn(0f, 1f)
                    val s = 0.7f + 0.5f * phase(100, 4000) + 0.06f * pulse
                    scaleX = s
                    scaleY = s
                }
                .background(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.45f), Color.Transparent)), CircleShape),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        // Respiro del logo intero
                        val b = sin(phase(2600, 3300, LinearEasing) * PI).toFloat()
                        val s = 1f + 0.035f * b
                        scaleX = s
                        scaleY = s
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        // Riflesso: banda di luce obliqua solo sulle parti disegnate del logo
                        val r = phase(1900, 2700, LinearEasing)
                        if (r > 0f && r < 1f) {
                            val x = size.width * (-0.45f + 1.9f * r)
                            val band = size.width * 0.26f
                            drawRect(
                                brush = Brush.linearGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.75f), Color.Transparent),
                                    start = Offset(x - band, 0f),
                                    end = Offset(x + band, size.height),
                                ),
                                blendMode = BlendMode.SrcAtop,
                            )
                        }
                    },
            ) {
                // Triangoli: entrano in ventaglio con rotazione, zoom e rimbalzo
                val layers = listOf(R.drawable.lu3g_tri1, R.drawable.lu3g_tri2, R.drawable.lu3g_tri3, R.drawable.lu3g_tri4)
                layers.forEachIndexed { i, res ->
                    val start = 60 + 170 * i
                    Image(
                        painter = painterResource(res),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val a = phase(start, start + 420)
                                val m = phase(start, start + 560, EaseOutBack)
                                alpha = a
                                translationX = (1f - m) * 90.dp.toPx()
                                translationY = -(1f - m) * 60.dp.toPx()
                                rotationZ = (1f - m) * 9f
                                val s = 1.12f - 0.12f * m
                                scaleX = s
                                scaleY = s
                                transformOrigin = TransformOrigin(0.5f, 0.6f)
                            },
                    )
                }
                // Lettere: L, U, 3, G si scrivono una alla volta, salendo con un piccolo rimbalzo
                val letters = listOf(R.drawable.lu3g_letter_l, R.drawable.lu3g_letter_u, R.drawable.lu3g_letter_3, R.drawable.lu3g_letter_g)
                letters.forEachIndexed { j, res ->
                    val start = 1150 + 120 * j
                    Image(
                        painter = painterResource(res),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val a = phase(start, start + 300)
                                val m = phase(start, start + 420, EaseOutBack)
                                alpha = a
                                translationY = (1f - m) * 16.dp.toPx()
                                val s = 0.85f + 0.15f * m
                                scaleX = s
                                scaleY = s
                                transformOrigin = TransformOrigin(0.5f, 0.85f)
                            },
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "LU3G TV",
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                modifier = Modifier.graphicsLayer {
                    val l = phase(2200, 2700)
                    alpha = l
                    translationY = (1f - l) * 10.dp.toPx()
                },
            )
            if (season?.greeting != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    season.greeting,
                    style = MaterialTheme.typography.titleMedium,
                    color = glowColor,
                    modifier = Modifier.graphicsLayer {
                        val l = phase(2500, 3100)
                        alpha = l
                        translationY = (1f - l) * 12.dp.toPx()
                    },
                )
            }
        }
    }
}

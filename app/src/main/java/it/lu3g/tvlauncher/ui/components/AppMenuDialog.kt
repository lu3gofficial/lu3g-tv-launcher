package it.lu3g.tvlauncher.ui.components

import android.view.KeyEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
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
import it.lu3g.tvlauncher.data.AppEntry
import it.lu3g.tvlauncher.ui.theme.Amber
import it.lu3g.tvlauncher.ui.theme.Fog
import it.lu3g.tvlauncher.ui.theme.Ink
import it.lu3g.tvlauncher.ui.theme.Muted
import it.lu3g.tvlauncher.ui.theme.Surface1
import it.lu3g.tvlauncher.ui.theme.Surface3
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Il pannello arriva appena oltre la misura e si posa. */
private val PanelEasing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.05f)

/**
 * Menu contestuale di un'app (OK premuto a lungo, o tasto Menu).
 *
 * Nasce dalla card: [origin] è il rettangolo della card sullo schermo, da cui il pannello
 * cresce fino al centro mentre le voci entrano in cascata; alla chiusura torna alla card.
 * Senza [origin] compare crescendo appena. Un solo orologio ([Animatable] 0..1) letto dentro
 * graphicsLayer: nessuna ricomposizione durante l'animazione.
 */
@Composable
fun AppMenuDialog(
    app: AppEntry,
    origin: Rect?,
    isFavorite: Boolean,
    favoriteIndex: Int,
    favoritesCount: Int,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    val first = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val open = remember { Animatable(0f) }
    var leaving by remember { mutableStateOf(false) }
    // Il menu si apre mentre OK è ancora premuto: le ripetizioni e il rilascio di quella pressione
    // arrivano a questa finestra, e la voce a fuoco ("Apri") prenderebbe il rilascio per un clic.
    // OK viene ignorato finché non arriva una pressione nuova, iniziata con il menu già aperto.
    val okArmed = remember { booleanArrayOf(false) }
    LaunchedEffect(Unit) { open.animateTo(1f, tween(380, easing = LinearEasing)) }
    // Chiusura animata: prima il pannello rientra, poi si esegue l'azione scelta
    val leave: (() -> Unit) -> Unit = { then ->
        if (!leaving) {
            leaving = true
            scope.launch {
                open.animateTo(0f, tween(200, easing = LinearEasing))
                then()
            }
        }
    }
    Dialog(onDismissRequest = { leave(onDismiss) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) {
            delay(50)
            runCatching { first.requestFocus() }
        }
        // La finestra occupa tutto lo schermo, così il pannello può partire dalla card
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { e ->
                    val native = e.nativeKeyEvent
                    val isOk = native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                        native.keyCode == KeyEvent.KEYCODE_ENTER ||
                        native.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                    if (!isOk) return@onPreviewKeyEvent false
                    if (native.action == KeyEvent.ACTION_DOWN && native.repeatCount == 0) okArmed[0] = true
                    !okArmed[0]
                },
            contentAlignment = Alignment.Center,
        ) {
            val screenW = constraints.maxWidth.toFloat()
            val screenH = constraints.maxHeight.toFloat()
            var panel by remember { mutableStateOf(IntSize.Zero) }
            val shape = RoundedCornerShape(22.dp)
            Surface(
                modifier = Modifier
                    .width(440.dp)
                    .onSizeChanged { panel = it }
                    .graphicsLayer {
                        val pw = panel.width.toFloat()
                        val ph = panel.height.toFloat()
                        if (pw <= 0f || ph <= 0f) {
                            alpha = 0f
                            return@graphicsLayer
                        }
                        val p = phase(open.value, 0f, 0.85f, PanelEasing)
                        if (origin != null && origin.width > 0f && origin.height > 0f) {
                            val sx = origin.width / pw
                            val sy = origin.height / ph
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = sx + (1f - sx) * p
                            scaleY = sy + (1f - sy) * p
                            translationX = (origin.left - (screenW - pw) / 2f) * (1f - p)
                            translationY = (origin.top - (screenH - ph) / 2f) * (1f - p)
                        } else {
                            val s = 0.92f + 0.08f * p
                            scaleX = s
                            scaleY = s
                        }
                        alpha = if (leaving) p.coerceIn(0f, 1f) else (0.25f + 0.75f * p).coerceIn(0f, 1f)
                    },
                shape = shape,
                colors = SurfaceDefaults.colors(containerColor = Surface1, contentColor = Fog),
                border = Border(BorderStroke(1.dp, Surface3), shape = shape),
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(Modifier.cascade(open, -1), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            bitmap = app.icon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(app.label, style = MaterialTheme.typography.titleLarge, color = Fog, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    var i = 0
                    MenuItem(stringResource(R.string.menu_open), Icons.Default.PlayArrow, { leave(onOpen) }, Modifier.focusRequester(first).cascade(open, i++))
                    MenuItem(
                        if (isFavorite) stringResource(R.string.menu_fav_remove) else stringResource(R.string.menu_fav_add),
                        Icons.Default.Star,
                        { leave(onToggleFavorite) },
                        Modifier.cascade(open, i++),
                    )
                    if (isFavorite && favoriteIndex > 0) {
                        MenuItem(stringResource(R.string.menu_move_left), Icons.Default.KeyboardArrowLeft, { if (!leaving) onMoveLeft() }, Modifier.cascade(open, i++))
                    }
                    if (isFavorite && favoriteIndex in 0 until favoritesCount - 1) {
                        MenuItem(stringResource(R.string.menu_move_right), Icons.Default.KeyboardArrowRight, { if (!leaving) onMoveRight() }, Modifier.cascade(open, i++))
                    }
                    MenuItem(stringResource(R.string.menu_info), Icons.Default.Info, { leave(onInfo) }, Modifier.cascade(open, i++))
                    if (!app.isSystem) MenuItem(stringResource(R.string.menu_uninstall), Icons.Default.Delete, { leave(onUninstall) }, Modifier.cascade(open, i++))
                    MenuItem(stringResource(R.string.menu_close), Icons.Default.Close, { leave(onDismiss) }, Modifier.cascade(open, i++))
                }
            }
        }
    }
}

/** Voce [index] del pannello: compare e sale poco dopo la precedente (-1 è l'intestazione). */
private fun Modifier.cascade(open: Animatable<Float, AnimationVector1D>, index: Int): Modifier = graphicsLayer {
    val v = open.value
    if (v < 1f) {
        val start = 0.22f + index * 0.07f
        val p = phase(v, start, start + 0.36f)
        alpha = p
        translationY = (1f - p) * 12.dp.toPx()
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(text, style = MaterialTheme.typography.labelLarge) },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Fog,
            focusedContainerColor = Amber,
            focusedContentColor = Ink,
        ),
    )
}

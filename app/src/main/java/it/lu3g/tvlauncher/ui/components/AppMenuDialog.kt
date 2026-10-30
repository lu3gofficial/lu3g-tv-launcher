package it.lu3g.tvlauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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

/** Menu contestuale di un'app (OK premuto a lungo, o tasto Menu). */
@Composable
fun AppMenuDialog(
    app: AppEntry,
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
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) {
            delay(50)
            runCatching { first.requestFocus() }
        }
        val shape = RoundedCornerShape(22.dp)
        Surface(
            modifier = Modifier.width(440.dp),
            shape = shape,
            colors = SurfaceDefaults.colors(containerColor = Surface1, contentColor = Fog),
            border = Border(BorderStroke(1.dp, Surface3), shape = shape),
        ) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                MenuItem(stringResource(R.string.menu_open), Icons.Default.PlayArrow, onOpen, Modifier.focusRequester(first))
                MenuItem(
                    if (isFavorite) stringResource(R.string.menu_fav_remove) else stringResource(R.string.menu_fav_add),
                    Icons.Default.Star,
                    onToggleFavorite,
                )
                if (isFavorite && favoriteIndex > 0) {
                    MenuItem(stringResource(R.string.menu_move_left), Icons.Default.KeyboardArrowLeft, onMoveLeft)
                }
                if (isFavorite && favoriteIndex in 0 until favoritesCount - 1) {
                    MenuItem(stringResource(R.string.menu_move_right), Icons.Default.KeyboardArrowRight, onMoveRight)
                }
                MenuItem(stringResource(R.string.menu_info), Icons.Default.Info, onInfo)
                if (!app.isSystem) MenuItem(stringResource(R.string.menu_uninstall), Icons.Default.Delete, onUninstall)
                MenuItem(stringResource(R.string.menu_close), Icons.Default.Close, onDismiss)
            }
        }
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

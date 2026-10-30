package it.lu3g.tvlauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import it.lu3g.tvlauncher.update.UpdateInfo
import it.lu3g.tvlauncher.update.UpdateState
import it.lu3g.tvlauncher.ui.theme.Amber
import it.lu3g.tvlauncher.ui.theme.Fog
import it.lu3g.tvlauncher.ui.theme.Ink
import it.lu3g.tvlauncher.ui.theme.Muted
import it.lu3g.tvlauncher.ui.theme.Silver
import it.lu3g.tvlauncher.ui.theme.Surface1
import it.lu3g.tvlauncher.ui.theme.Surface3
import kotlinx.coroutines.delay

/** Finestra "Aggiornamento disponibile": note di versione, stato del download, Aggiorna ora / Più tardi. */
@Composable
fun UpdateDialog(info: UpdateInfo, state: UpdateState, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) {
            delay(50)
            runCatching { first.requestFocus() }
        }
        val shape = RoundedCornerShape(22.dp)
        Surface(
            modifier = Modifier.width(480.dp),
            shape = shape,
            colors = SurfaceDefaults.colors(containerColor = Surface1, contentColor = Fog),
            border = Border(BorderStroke(1.dp, Surface3), shape = shape),
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(stringResource(R.string.update_title), style = MaterialTheme.typography.labelSmall, color = Amber)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.update_version, info.version), style = MaterialTheme.typography.titleLarge, color = Fog)
                if (info.notes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(info.notes, style = MaterialTheme.typography.bodyMedium, color = Silver, maxLines = 6, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(12.dp))
                val status = when (state) {
                    is UpdateState.Downloading -> stringResource(R.string.update_downloading, (state.progress * 100).toInt())
                    UpdateState.Installing -> stringResource(R.string.update_installing)
                    UpdateState.NeedsPermission -> stringResource(R.string.update_permission)
                    is UpdateState.Error -> stringResource(R.string.update_error, state.message)
                    else -> null
                }
                if (status != null) {
                    Text(status, style = MaterialTheme.typography.bodyMedium, color = Muted)
                    Spacer(Modifier.height(10.dp))
                }
                val busy = state is UpdateState.Downloading || state == UpdateState.Installing
                val colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = Fog,
                    focusedContainerColor = Amber,
                    focusedContentColor = Ink,
                )
                ListItem(
                    selected = false,
                    enabled = !busy,
                    onClick = onUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(first),
                    leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.update_now), style = MaterialTheme.typography.labelLarge) },
                    colors = colors,
                )
                ListItem(
                    selected = false,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    leadingContent = { Icon(Icons.Default.Close, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.update_later), style = MaterialTheme.typography.labelLarge) },
                    colors = colors,
                )
            }
        }
    }
}

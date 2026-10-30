package it.lu3g.tvlauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import it.lu3g.tvlauncher.R
import it.lu3g.tvlauncher.theme.SeasonId
import it.lu3g.tvlauncher.theme.SeasonTheme
import it.lu3g.tvlauncher.theme.Seasons
import it.lu3g.tvlauncher.ui.theme.Amber
import it.lu3g.tvlauncher.ui.theme.Fog
import it.lu3g.tvlauncher.ui.theme.Ink
import it.lu3g.tvlauncher.ui.theme.Surface1
import it.lu3g.tvlauncher.ui.theme.Surface2
import it.lu3g.tvlauncher.ui.theme.Surface3
import kotlinx.coroutines.delay

/** Pannello "Tema": effetti stagionali on/off e scelta del tema (automatico, nessuno, o forzato). */
@Composable
fun ThemeSettingsDialog(
    enabled: Boolean,
    choice: SeasonId,
    todayTheme: SeasonTheme?,
    onDismiss: () -> Unit,
    onToggleEnabled: () -> Unit,
    onChoose: (SeasonId) -> Unit,
    versionLabel: String,
    updateHint: String?,
    onCheckUpdates: () -> Unit,
) {
    val first = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) {
            delay(50)
            runCatching { first.requestFocus() }
        }
        val shape = RoundedCornerShape(22.dp)
        Surface(
            modifier = Modifier
                .width(470.dp)
                .heightIn(max = 470.dp),
            shape = shape,
            colors = SurfaceDefaults.colors(containerColor = Surface1, contentColor = Fog),
            border = Border(BorderStroke(1.dp, Surface3), shape = shape),
        ) {
            Column(
                Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.theme_title), style = MaterialTheme.typography.titleLarge, color = Fog)
                Spacer(Modifier.height(14.dp))
                ThemeItem(
                    title = stringResource(R.string.theme_effects),
                    supporting = stringResource(if (enabled) R.string.theme_on else R.string.theme_off),
                    checked = enabled,
                    onClick = onToggleEnabled,
                    modifier = Modifier.focusRequester(first),
                )
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.theme_choose).uppercase(), style = MaterialTheme.typography.labelSmall, color = Amber)
                Spacer(Modifier.height(4.dp))
                ThemeItem(
                    title = stringResource(R.string.theme_auto),
                    supporting = todayTheme?.let { stringResource(R.string.theme_today, it.label) } ?: stringResource(R.string.theme_today_none),
                    checked = choice == SeasonId.AUTO,
                    onClick = { onChoose(SeasonId.AUTO) },
                )
                ThemeItem(
                    title = stringResource(R.string.theme_none),
                    supporting = null,
                    checked = choice == SeasonId.NONE,
                    onClick = { onChoose(SeasonId.NONE) },
                )
                Seasons.all.forEach { theme ->
                    ThemeItem(
                        title = theme.label,
                        supporting = theme.greeting,
                        checked = choice == theme.id,
                        onClick = { onChoose(theme.id) },
                    )
                }
                Spacer(Modifier.height(6.dp))
                ThemeItem(
                    title = versionLabel,
                    supporting = updateHint ?: stringResource(R.string.update_check),
                    checked = false,
                    onClick = onCheckUpdates,
                )
                ListItem(
                    selected = false,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    leadingContent = { Icon(Icons.Default.Close, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.menu_close), style = MaterialTheme.typography.labelLarge) },
                    colors = itemColors(),
                )
            }
        }
    }
}

@Composable
private fun itemColors() = ListItemDefaults.colors(
    containerColor = Color.Transparent,
    contentColor = Fog,
    focusedContainerColor = Amber,
    focusedContentColor = Ink,
    selectedContainerColor = Surface2,
    selectedContentColor = Fog,
    focusedSelectedContainerColor = Amber,
    focusedSelectedContentColor = Ink,
)

@Composable
private fun ThemeItem(title: String, supporting: String?, checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        selected = checked,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            if (checked) Icon(Icons.Default.Check, contentDescription = null) else Spacer(Modifier.width(24.dp))
        },
        headlineContent = { Text(title, style = MaterialTheme.typography.labelLarge) },
        // Il colore segue quello della voce (chiaro a riposo, scuro sull'ambra del fuoco)
        supportingContent = supporting?.let { { Text(it, style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = 0.75f)) } },
        colors = itemColors(),
    )
}

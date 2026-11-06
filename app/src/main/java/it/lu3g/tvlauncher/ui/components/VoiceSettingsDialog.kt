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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import it.lu3g.tvlauncher.ui.theme.Amber
import it.lu3g.tvlauncher.ui.theme.Fog
import it.lu3g.tvlauncher.ui.theme.Ink
import it.lu3g.tvlauncher.ui.theme.Surface1
import it.lu3g.tvlauncher.ui.theme.Surface2
import it.lu3g.tvlauncher.ui.theme.Surface3
import it.lu3g.tvlauncher.voice.VoiceCategory
import it.lu3g.tvlauncher.voice.VoiceSettings
import kotlinx.coroutines.delay

/**
 * Opzioni dell'assistente vocale. Ogni voce mostra il valore attuale; OK passa al successivo.
 * [voices] è la lista (valore salvato, etichetta) delle voci selezionabili: Paola, sistema, voci installate.
 */
@Composable
fun VoiceSettingsDialog(
    settings: VoiceSettings,
    voices: List<Pair<String, String>>,
    ttsAvailable: Boolean,
    onDismiss: () -> Unit,
    onToggleEnabled: () -> Unit,
    onCycleInterval: () -> Unit,
    onCycleQuiet: () -> Unit,
    onCycleVoice: () -> Unit,
    onCycleTone: () -> Unit,
    onCycleVolume: () -> Unit,
    onPreview: () -> Unit,
    onToggleCategory: (VoiceCategory) -> Unit,
    onShowNow: () -> Unit,
) {
    val first = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) {
            delay(50)
            runCatching { first.requestFocus() }
        }
        val shape = RoundedCornerShape(22.dp)
        val pop = rememberPopIn()
        Surface(
            modifier = Modifier
                .width(500.dp)
                .heightIn(max = 470.dp)
                .then(pop),
            shape = shape,
            colors = SurfaceDefaults.colors(containerColor = Surface1, contentColor = Fog),
            border = Border(BorderStroke(1.dp, Surface3), shape = shape),
        ) {
            Column(
                Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.voice_settings_title), style = MaterialTheme.typography.titleLarge, color = Fog)
                if (!ttsAvailable) {
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.voice_no_tts), style = MaterialTheme.typography.bodySmall, color = Amber)
                }
                Spacer(Modifier.height(12.dp))
                OptionItem(
                    title = stringResource(R.string.voice_enabled),
                    value = stringResource(if (settings.enabled) R.string.voice_on else R.string.voice_off),
                    checked = settings.enabled,
                    onClick = onToggleEnabled,
                    modifier = Modifier.focusRequester(first),
                )
                OptionItem(stringResource(R.string.voice_interval), stringResource(R.string.voice_interval_value, settings.intervalMin), null, onCycleInterval)
                OptionItem(stringResource(R.string.voice_quiet), settings.quiet.label, null, onCycleQuiet)
                val voiceLabel = voices.firstOrNull { it.first == settings.voiceName }?.second ?: stringResource(R.string.voice_default)
                OptionItem(stringResource(R.string.voice_voice), voiceLabel, null, onCycleVoice)
                OptionItem(stringResource(R.string.voice_tone), settings.tone.label, null, onCycleTone)
                OptionItem(stringResource(R.string.voice_volume), stringResource(R.string.voice_volume_value, (settings.volume * 100).toInt()), null, onCycleVolume)
                ActionItem(stringResource(R.string.voice_preview), Icons.Default.PlayArrow, onPreview)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.voice_categories).uppercase(), style = MaterialTheme.typography.labelSmall, color = Amber)
                Spacer(Modifier.height(4.dp))
                VoiceCategory.entries.forEach { c ->
                    val on = c.id in settings.categories
                    OptionItem(c.label, stringResource(if (on) R.string.voice_on else R.string.voice_off), on, onClick = { onToggleCategory(c) })
                }
                Spacer(Modifier.height(8.dp))
                ActionItem(stringResource(R.string.voice_show_now), Icons.Default.PlayArrow, onShowNow)
                ActionItem(stringResource(R.string.menu_close), Icons.Default.Close, onDismiss)
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
private fun OptionItem(title: String, value: String, checked: Boolean?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        selected = checked == true,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        leadingContent = { if (checked == true) Icon(Icons.Default.Check, contentDescription = null) else Spacer(Modifier.width(24.dp)) },
        headlineContent = { Text(title, style = MaterialTheme.typography.labelLarge) },
        supportingContent = { Text(value, style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = 0.75f)) },
        colors = itemColors(),
    )
}

@Composable
private fun ActionItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title, style = MaterialTheme.typography.labelLarge) },
        colors = itemColors(),
    )
}

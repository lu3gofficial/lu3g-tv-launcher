package it.lu3g.tvlauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import it.lu3g.tvlauncher.R
import it.lu3g.tvlauncher.ui.theme.Amber
import it.lu3g.tvlauncher.ui.theme.Fog
import it.lu3g.tvlauncher.ui.theme.GUTTER
import it.lu3g.tvlauncher.ui.theme.Ink
import it.lu3g.tvlauncher.ui.theme.Muted
import it.lu3g.tvlauncher.ui.theme.Silver
import it.lu3g.tvlauncher.ui.theme.Surface1
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TopBar(
    onSettings: () -> Unit,
    onTheme: () -> Unit,
    onSearch: () -> Unit,
    greeting: String?,
    updateLabel: String?,
    onUpdate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = GUTTER, end = GUTTER, top = 26.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_lu3g_logo),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text("LU3G", style = MaterialTheme.typography.titleLarge, color = Fog)
            Text("TV", style = MaterialTheme.typography.labelSmall, color = Amber)
        }
        Spacer(Modifier.weight(1f))
        Clock(greeting)
        Spacer(Modifier.width(22.dp))
        if (updateLabel != null) {
            Button(
                onClick = onUpdate,
                colors = ButtonDefaults.colors(
                    containerColor = Amber,
                    contentColor = Ink,
                    focusedContainerColor = Fog,
                    focusedContentColor = Ink,
                ),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(updateLabel, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(10.dp))
        }
        Button(
            onClick = onSearch,
            colors = ButtonDefaults.colors(
                containerColor = Surface1,
                contentColor = Silver,
                focusedContainerColor = Amber,
                focusedContentColor = Ink,
            ),
        ) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.search_button), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onTheme,
            colors = ButtonDefaults.colors(
                containerColor = Surface1,
                contentColor = Silver,
                focusedContainerColor = Amber,
                focusedContentColor = Ink,
            ),
        ) {
            Text(stringResource(R.string.theme_button), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(10.dp))
        IconButton(
            onClick = onSettings,
            colors = IconButtonDefaults.colors(
                containerColor = Surface1,
                contentColor = Silver,
                focusedContainerColor = Amber,
                focusedContentColor = Ink,
            ),
        ) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
        }
    }
}

/** Ora e data in italiano; si aggiorna allo scoccare di ogni minuto. */
@Composable
private fun Clock(greeting: String?) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    val locale = Locale.ITALIAN
    val time = remember(now) { SimpleDateFormat("HH:mm", locale).format(Date(now)) }
    val date = remember(now) {
        SimpleDateFormat("EEEE d MMMM", locale).format(Date(now))
            .replaceFirstChar { it.titlecase(locale) }
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(time, style = MaterialTheme.typography.headlineMedium, color = Fog)
        Text(date, style = MaterialTheme.typography.bodyMedium, color = Muted)
        if (greeting != null) Text(greeting, style = MaterialTheme.typography.labelSmall, color = Amber)
    }
}

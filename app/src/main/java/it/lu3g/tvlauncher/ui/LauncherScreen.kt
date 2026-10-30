package it.lu3g.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import it.lu3g.tvlauncher.BuildConfig
import it.lu3g.tvlauncher.LauncherViewModel
import it.lu3g.tvlauncher.R
import it.lu3g.tvlauncher.data.AppEntry
import it.lu3g.tvlauncher.data.TvInput
import it.lu3g.tvlauncher.ui.components.AnimatedBackground
import it.lu3g.tvlauncher.ui.components.AppCard
import it.lu3g.tvlauncher.ui.components.AppMenuDialog
import it.lu3g.tvlauncher.ui.components.Hero
import it.lu3g.tvlauncher.ui.components.HeroInfo
import it.lu3g.tvlauncher.ui.components.SeasonalLayer
import it.lu3g.tvlauncher.ui.components.SplashOverlay
import it.lu3g.tvlauncher.ui.components.ThemeSettingsDialog
import it.lu3g.tvlauncher.ui.components.UpdateDialog
import it.lu3g.tvlauncher.theme.Seasons
import it.lu3g.tvlauncher.ui.components.TopBar
import it.lu3g.tvlauncher.ui.theme.Amber
import it.lu3g.tvlauncher.ui.theme.Dim
import it.lu3g.tvlauncher.ui.theme.Fog
import it.lu3g.tvlauncher.ui.theme.GUTTER
import it.lu3g.tvlauncher.ui.theme.Ink
import it.lu3g.tvlauncher.ui.theme.Silver
import it.lu3g.tvlauncher.ui.theme.Surface2
import it.lu3g.tvlauncher.ui.theme.Surface3
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Scorrimento verticale "da TV", calcolato conoscendo la posizione attuale:
 *  - se l'elemento a fuoco sta nella prima "schermata" (intestazione + prima riga)
 *    si torna in cima, così logo, orologio e intestazione restano interi;
 *  - se è già comodamente visibile non si scorre;
 *  - altrimenti la riga a fuoco viene portata a circa un quarto dall'alto, così
 *    quella successiva è sempre già visibile.
 */
@OptIn(ExperimentalFoundationApi::class)
private class PivotBringIntoViewSpec(private val scroll: ScrollState) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val absoluteTop = offset + scroll.value
        if (absoluteTop < containerSize * 0.75f) return -scroll.value.toFloat()
        if (offset >= 0f && offset + size <= containerSize * 0.72f) return 0f
        return offset - containerSize * 0.22f
    }
}

/** Scorrimento orizzontale: l'elemento a fuoco resta dentro il margine laterale. */
@OptIn(ExperimentalFoundationApi::class)
private val EdgeBringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val margin = containerSize * 0.06f
        return when {
            offset < margin -> offset - margin
            offset + size > containerSize - margin -> offset + size - (containerSize - margin)
            else -> 0f
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun LauncherScreen(vm: LauncherViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()
    val pivotSpec = remember(scroll) { PivotBringIntoViewSpec(scroll) }
    val scope = rememberCoroutineScope()
    val firstFocus = remember { FocusRequester() }
    val showSplash by vm.showSplash.collectAsStateWithLifecycle()
    val splashRunning by vm.splashRunning.collectAsStateWithLifecycle()
    val season by vm.seasonTheme.collectAsStateWithLifecycle()
    val themeSettingsOpen by vm.themeSettingsOpen.collectAsStateWithLifecycle()
    val seasonalEnabled by vm.seasonalEnabled.collectAsStateWithLifecycle()
    val seasonChoice by vm.seasonChoice.collectAsStateWithLifecycle()
    val updateInfo by vm.updateInfo.collectAsStateWithLifecycle()
    val updateState by vm.updateState.collectAsStateWithLifecycle()
    val updateDialogOpen by vm.updateDialogOpen.collectAsStateWithLifecycle()
    val updateHint by vm.updateHint.collectAsStateWithLifecycle()

    val goTop: () -> Unit = {
        scope.launch { scroll.animateScrollTo(0) }
        runCatching { firstFocus.requestFocus() }
    }
    BackHandler(onBack = goTop)
    LaunchedEffect(Unit) { vm.homeEvents.collect { goTop() } }
    LaunchedEffect(state.loading, showSplash) {
        if (!state.loading && !showSplash) {
            delay(60)
            runCatching { firstFocus.requestFocus() }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        AnimatedBackground(Modifier.fillMaxSize(), tint = state.focused?.accent ?: Surface3, seasonTint = season?.tint)
        season?.let { SeasonalLayer(theme = it, modifier = Modifier.fillMaxSize()) }

        CompositionLocalProvider(LocalBringIntoViewSpec provides pivotSpec) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll),
            ) {
                TopBar(
                    onSettings = vm::openSettings,
                    onTheme = vm::openThemeSettings,
                    onSearch = vm::openSearch,
                    greeting = season?.greeting,
                    updateLabel = updateInfo?.let { stringResource(R.string.update_chip, it.version) },
                    onUpdate = vm::openUpdateDialog,
                )
                val focused = state.focused
                Hero(
                    info = HeroInfo(
                        app = focused,
                        isFavorite = focused != null && state.favorites.any { it.packageName == focused.packageName },
                        lastLaunch = focused?.let { state.lastLaunch[it.packageName] },
                    ),
                    favoritesEmpty = state.favorites.isEmpty(),
                )

                var first = true
                if (state.favorites.isNotEmpty()) {
                    AppRow(stringResource(R.string.row_favorites), state.favorites, firstFocus.takeIf { first }, vm, cardWidth = 180.dp)
                    first = false
                }
                if (state.recents.isNotEmpty()) {
                    AppRow(stringResource(R.string.row_recents), state.recents, firstFocus.takeIf { first }, vm, cardWidth = 150.dp)
                    first = false
                }
                if (state.inputs.isNotEmpty()) {
                    InputsRow(state.inputs, onOpen = vm::openInput)
                }
                if (state.games.isNotEmpty()) {
                    AppRow(stringResource(R.string.row_games), state.games, null, vm, cardWidth = 150.dp)
                }
                AppGrid(stringResource(R.string.row_all), state.apps, firstFocus.takeIf { first }, vm)

                Text(
                    text = stringResource(R.string.footer, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelSmall,
                    color = Dim,
                    modifier = Modifier.padding(start = GUTTER, top = 28.dp, bottom = 40.dp),
                )
            }
        }

        if (themeSettingsOpen) {
            ThemeSettingsDialog(
                enabled = seasonalEnabled,
                choice = seasonChoice,
                todayTheme = Seasons.forToday(),
                onDismiss = vm::closeThemeSettings,
                onToggleEnabled = { vm.setSeasonalEnabled(!seasonalEnabled) },
                onChoose = vm::chooseSeason,
                versionLabel = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                updateHint = updateHint,
                onCheckUpdates = { vm.checkForUpdates(manual = true) },
            )
        }

        val pendingUpdate = updateInfo
        if (updateDialogOpen && pendingUpdate != null) {
            UpdateDialog(
                info = pendingUpdate,
                state = updateState,
                onUpdate = vm::startUpdate,
                onDismiss = vm::closeUpdateDialog,
            )
        }

        state.menuFor?.let { app ->
            AppMenuDialog(
                app = app,
                isFavorite = state.menuIsFavorite,
                favoriteIndex = state.menuFavoriteIndex,
                favoritesCount = state.favorites.size,
                onMoveLeft = { vm.moveFavorite(app, -1) },
                onMoveRight = { vm.moveFavorite(app, +1) },
                onDismiss = vm::closeMenu,
                onOpen = { vm.closeMenu(); vm.launch(app) },
                onToggleFavorite = { vm.toggleFavorite(app); vm.closeMenu() },
                onInfo = { vm.closeMenu(); vm.openAppInfo(app) },
                onUninstall = { vm.closeMenu(); vm.uninstall(app) },
            )
        }

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(tween(420)) + scaleOut(targetScale = 1.07f, animationSpec = tween(420)),
        ) {
            SplashOverlay(running = splashRunning, season = season, onDone = vm::splashDone)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = Silver,
        modifier = Modifier.padding(start = GUTTER, top = 14.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun AppRow(
    title: String,
    apps: List<AppEntry>,
    firstFocus: FocusRequester?,
    vm: LauncherViewModel,
    cardWidth: Dp,
) {
    SectionTitle(title)
    CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusRestorer(),
            contentPadding = PaddingValues(horizontal = GUTTER, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(apps, key = { _, a -> a.packageName }) { index, app ->
                AppCard(
                    app = app,
                    width = cardWidth,
                    modifier = if (index == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier,
                    onClick = { vm.launch(app) },
                    onLongClick = { vm.openMenu(app) },
                    onFocused = { vm.onFocus(app) },
                )
            }
        }
    }
}

/** Griglia a righe fisse: il numero di colonne dipende dalla larghezza disponibile. */
@Composable
private fun AppGrid(
    title: String,
    apps: List<AppEntry>,
    firstFocus: FocusRequester?,
    vm: LauncherViewModel,
) {
    SectionTitle(title)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = GUTTER, vertical = 14.dp),
    ) {
        val gap = 16.dp
        val minWidth = 140.dp
        val cols = ((maxWidth + gap) / (minWidth + gap)).toInt().coerceAtLeast(1)
        val cardWidth = (maxWidth - gap * (cols - 1)) / cols
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            apps.chunked(cols).forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEachIndexed { c, app ->
                        AppCard(
                            app = app,
                            width = cardWidth,
                            modifier = if (r == 0 && c == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier,
                            onClick = { vm.launch(app) },
                            onLongClick = { vm.openMenu(app) },
                            onFocused = { vm.onFocus(app) },
                        )
                    }
                }
            }
        }
    }
}

/** Ingressi HDMI e simili, come bottoni. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun InputsRow(inputs: List<TvInput>, onOpen: (TvInput) -> Unit) {
    SectionTitle(stringResource(R.string.row_inputs))
    CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusRestorer(),
            contentPadding = PaddingValues(horizontal = GUTTER, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(inputs, key = { _, i -> i.id }) { _, input ->
                Button(
                    onClick = { onOpen(input) },
                    colors = ButtonDefaults.colors(
                        containerColor = Surface2,
                        contentColor = Fog,
                        focusedContainerColor = Amber,
                        focusedContentColor = Ink,
                    ),
                ) {
                    Text(input.label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
    Spacer(Modifier.height(2.dp))
}

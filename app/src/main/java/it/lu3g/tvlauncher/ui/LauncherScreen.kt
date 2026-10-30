package it.lu3g.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
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
import it.lu3g.tvlauncher.ui.components.CoffeeBreakDialog
import it.lu3g.tvlauncher.ui.components.VoiceSettingsDialog
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
import it.lu3g.tvlauncher.ui.theme.Surface1
import it.lu3g.tvlauncher.ui.theme.Surface2
import it.lu3g.tvlauncher.ui.theme.Surface3
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val breakPrompt by vm.breakPrompt.collectAsStateWithLifecycle()
    val voiceSettingsOpen by vm.voiceSettingsOpen.collectAsStateWithLifecycle()
    val voiceSettings by vm.voiceSettings.collectAsStateWithLifecycle()

    // Animazione di apertura: la card "vola" verso il centro mentre la home si allontana e si scurisce
    val cardBounds = remember { HashMap<String, Rect>() }
    var launching by remember { mutableStateOf<Pair<AppEntry, Rect>?>(null) }
    val launchProgress = remember { Animatable(0f) }
    val onLaunch: (AppEntry) -> Unit = { app ->
        if (launching == null) {
            val bounds = cardBounds[app.packageName]
            if (bounds == null) {
                vm.launch(app)
            } else {
                // La card a fuoco è disegnata ingrandita dell'8%: si parte da quella dimensione
                val dx = bounds.width * 0.04f
                val dy = bounds.height * 0.04f
                launching = app to Rect(bounds.left - dx, bounds.top - dy, bounds.right + dx, bounds.bottom + dy)
            }
        }
    }
    LaunchedEffect(launching) {
        val current = launching ?: return@LaunchedEffect
        launchProgress.snapTo(0f)
        launchProgress.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        vm.launch(current.first)
        // L'app è sopra: l'effetto svanisce in silenzio, così al ritorno la home è già pulita
        delay(1_500)
        launchProgress.animateTo(0f, tween(200))
        launching = null
    }

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
                    .graphicsLayer {
                        val p = launchProgress.value
                        val s = 1f + 0.05f * p
                        scaleX = s
                        scaleY = s
                        alpha = 1f - 0.5f * p
                    }
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
                    AppRow(stringResource(R.string.row_favorites), state.favorites, firstFocus.takeIf { first }, vm, onLaunch, cardBounds, cardWidth = 180.dp)
                    first = false
                }
                if (state.recents.isNotEmpty()) {
                    AppRow(stringResource(R.string.row_recents), state.recents, firstFocus.takeIf { first }, vm, onLaunch, cardBounds, cardWidth = 150.dp)
                    first = false
                }
                if (state.inputs.isNotEmpty()) {
                    InputsRow(state.inputs, onOpen = vm::openInput)
                }
                if (state.games.isNotEmpty()) {
                    AppRow(stringResource(R.string.row_games), state.games, null, vm, onLaunch, cardBounds, cardWidth = 150.dp)
                }
                AppGrid(stringResource(R.string.row_all), state.apps, firstFocus.takeIf { first }, vm, onLaunch, cardBounds)

                Text(
                    text = stringResource(R.string.footer, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelSmall,
                    color = Dim,
                    modifier = Modifier.padding(start = GUTTER, top = 28.dp, bottom = 40.dp),
                )
            }
        }

        launching?.let { (app, origin) -> LaunchOverlay(app, origin, launchProgress) }

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
                onVoiceSettings = { vm.closeThemeSettings(); vm.openVoiceSettings() },
            )
        }

        if (voiceSettingsOpen) {
            val voices = remember { vm.italianVoices() }
            val ttsAvailable = remember { vm.ttsAvailable() }
            VoiceSettingsDialog(
                settings = voiceSettings,
                voices = voices,
                ttsAvailable = ttsAvailable,
                onDismiss = vm::closeVoiceSettings,
                onToggleEnabled = { vm.setVoiceEnabled(!voiceSettings.enabled) },
                onCycleInterval = vm::cycleInterval,
                onCycleQuiet = vm::cycleQuiet,
                onCycleVoice = vm::cycleVoice,
                onCycleTone = vm::cycleTone,
                onCycleVolume = vm::cycleVolume,
                onPreview = vm::previewVoice,
                onToggleCategory = vm::toggleCategory,
                onShowNow = { vm.closeVoiceSettings(); vm.showBreakNow() },
            )
        }

        breakPrompt?.let { (category, phrase) ->
            CoffeeBreakDialog(
                category = category,
                phrase = phrase,
                onConfirm = vm::confirmBreak,
                onSnooze = vm::snoozeBreak,
                onDismiss = vm::ignoreBreak,
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
    onLaunch: (AppEntry) -> Unit,
    cardBounds: MutableMap<String, Rect>,
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
                    onClick = { onLaunch(app) },
                    onLongClick = { vm.openMenu(app) },
                    onFocused = { vm.onFocus(app) },
                    onBounds = { cardBounds[app.packageName] = it },
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
    onLaunch: (AppEntry) -> Unit,
    cardBounds: MutableMap<String, Rect>,
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
                            onClick = { onLaunch(app) },
                            onLongClick = { vm.openMenu(app) },
                            onFocused = { vm.onFocus(app) },
                            onBounds = { cardBounds[app.packageName] = it },
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

/**
 * Livello dell'apertura: scurisce la home e fa volare il banner dell'app dalla sua card
 * fino al centro dello schermo, ingrandendolo. Solo trasformazioni di livello: nessuna
 * ricomposizione durante l'animazione.
 */
@Composable
private fun LaunchOverlay(app: AppEntry, origin: Rect, progress: Animatable<Float, AnimationVector1D>) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val targetW = w * 0.5f
        val targetH = targetW * 9f / 16f
        val targetLeft = (w - targetW) / 2f
        val targetTop = (h - targetH) / 2f

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.9f * progress.value }
                .background(Ink),
        )
        Box(
            Modifier
                .offset { IntOffset(origin.left.roundToInt(), origin.top.roundToInt()) }
                .size(with(density) { origin.width.toDp() }, with(density) { origin.height.toDp() })
                .graphicsLayer {
                    val p = progress.value
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = 1f + (targetW / origin.width - 1f) * p
                    scaleY = 1f + (targetH / origin.height - 1f) * p
                    translationX = (targetLeft - origin.left) * p
                    translationY = (targetTop - origin.top) * p
                    shape = RoundedCornerShape(14.dp)
                    clip = true
                }
                .background(Surface1),
        ) {
            if (app.banner != null) {
                Image(bitmap = app.banner, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Surface2, Surface1)))) {
                    Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(48.dp).align(Alignment.Center))
                }
            }
        }
    }
}
